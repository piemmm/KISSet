package org.prowl.ax25;

import com.fazecast.jSerialComm.SerialPortIOException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.util.AX25Tools;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BasicTransmittingConnector extends Connector implements TransmittingConnector, Transmitting {
    public static final int PROTOCOL_AX25 = 4;
    /**
     * These four bits contain the KISS device ID to be used in KISS frames sent through this port.
     * This supports the TCP port type when talking to the DireWolf
     * software TNC which can support up to 6 audio devices (and therefore up to 6 device IDs in
     * KISS frames). Conveniently, since these bits weren't used before, the backwards-compatible
     * default KISS device ID is zero.
     */
    public static final int FLAGS_MASK_KISSPORT = 0xF00;
    /**
     * This constant gets the number of bits to shift the above {@link #FLAGS_MASK_KISSPORT} bits right
     * to put them in the least significant bits of an integer value.
     */
    public static final int FLAGS_SHIFT_KISSPORT = 8;
    private static final Log LOG = LogFactory.getLog("BasicTransmittingConnector");
    private static final ProtocolFamily[] PROTOCOL_FAMILIES = ProtocolFamily.values();
    private final byte[] rcvBuf = new byte[4096];
    private final AX25Stack stack;
    private InputStream in;
    private final ArrayList<AX25FrameSource> queue = new ArrayList<>();
    /**
     * This is the default callsign this connector will use for transmitting things like UI frames.
     * <p>
     * It is seperate to the {@link ConnectionRequestListener}.acceptInbound() method which will allow you to
     * listen and respond to other callsigns (or ssids)
     */
    public AX25Callsign defaultCallsign;
    KissEscapeOutputStream kos;
    private String debugTag = "";
    private transient KissEscapeOutputStream.RcvState curState = KissEscapeOutputStream.RcvState.IDLE;
    private int wEnd = 0;
    private transient long frameStartTime = -1L;
    private final int retransmitCount;
    private transient TimedQueueEntry delayQueueHead = null;
    private String uuid;

    public BasicTransmittingConnector(String uuid, int pacLen, int maxFrames, int baudRateInBits, int retransmitCount, AX25Callsign defaultCallsign, InputStream in, OutputStream out, ConnectionRequestListener connectionRequestListener) {
        this.defaultCallsign = defaultCallsign;
        this.retransmitCount = retransmitCount;
        this.in = in;
        this.uuid = uuid;
        kos = new KissEscapeOutputStream(out);
        stack = new AX25Stack(pacLen, maxFrames, baudRateInBits);
        startRxThread();
        startTxThread();
        stack.setTransmitting(this);
        stack.setConnectionRequestListener(connectionRequestListener);
    }

    public String getUUID() {
        return uuid;
    }

    public void addFrameListener(AX25FrameListener l) {
        stack.addAX25FrameListener(l);
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public PortStats getStats() {
        return new PortStats();
    }

    @Override
    public void sendFrame(AX25Frame frame) throws IOException {
        queue(frame);
    }

    /**
     * Return base callsign or if an SSID is set, the callsign with the SSID
     *
     * @return the default callsign for this connector
     */
    @Override
    public String getCallsign() {
        return defaultCallsign.getSSID() == 0 ? defaultCallsign.getBaseCallsign() : defaultCallsign.getBaseCallsign() + "-" + defaultCallsign.getSSID();
    }

    @Override
    public int getCapabilities() {
        return CAP_XMT_PACKET_DATA | CAP_RCV_PACKET_DATA;
    }

    @Override
    public boolean hasCapability(int capMask) {
        return (getCapabilities() & capMask) == capMask;
    }

    @Override
    public int getAcceptableProtocolsMask() {
        return PROTOCOL_AX25;
    }

    /**
     * Queue the specified frame source for transmission over the specified (or all, if not
     * specified) transmit-enabled PortConnectors.
     *
     * @param entry AX25FrameSource of the frame to be transmitted
     */
    public synchronized void queue(AX25FrameSource entry) {
        queue.add(entry);
        notifyAll();
    }

    /**
     * Queue the specified frame source for transmission over the specified (or all, if not
     * specified) transmit-enabled PortConnectors.
     *
     * @param entry      AX25FrameSource of the frame to be transmitted
     * @param timeToSend long time in milliseconds since Unix epoch when packet is to be dequeued and transmitted
     */
    public synchronized void delayedQueue(AX25FrameSource entry, long timeToSend) {
        if (timeToSend <= System.currentTimeMillis()) {
            queue.add(entry); // overdue, send now
        } else {
            TimedQueueEntry tqe = new TimedQueueEntry(entry, timeToSend);
            if (delayQueueHead == null) {
                delayQueueHead = tqe;
            } else if (tqe.compareTo(delayQueueHead) < 0) {
                tqe.next = delayQueueHead;
                delayQueueHead = tqe;
            } else {
                TimedQueueEntry prev = delayQueueHead;
                while (prev.next != null && prev.next.compareTo(tqe) < 0) {
                    prev = prev.next;
                }
                tqe.next = prev.next;
                prev.next = tqe;
            }
        }
        notifyAll();
    }

    /**
     * Test if this callsign is addressed to the local station.
     *
     * @param destCallsign String of AX.25 callsign-SSID to test as a destination
     * @return boolean true if this callsign is for the local station
     */
    public boolean isLocalDest(String destCallsign) {
        // Default callsign test.
        return (getCallsign() != null && destCallsign.equalsIgnoreCase(getCallsign())) || stack.getConnectionRequestListener().isLocal(destCallsign);
    }

    /**
     * Get the locally-originated message retransmit count.
     *
     * @return retransmit count
     */
    public int getRetransmitCount() {
        return retransmitCount;
    }

    /**
     * Transmit the specified frame out one port.
     *
     * @param entry              the AX25FrameSource implementor that will provide the frames to transmit
     * @param now                current time in Unix milliseconds since 1970 UTC
     * @param p                  the port to transmit through
     * @param incrementXmitCount whether or not this is a new round of transmissions, such that proportional pathing should advance to the next path combination
     * @return boolean true if transmit didn't happen for some reason, such that any next call on the same frame should pass the increment flag again
     */
    private boolean sendForOnePort(AX25FrameSource entry, long now, TransmittingConnector p, boolean incrementXmitCount) {
        AX25Frame[] frames;
        // try all supported protocols if message didn't have a default
        int acceptableProtocolsMask = p.getAcceptableProtocolsMask();
        List<AX25Frame[]> alreadySentFrames = new ArrayList<>();
        proto_loop:
        for (int i = 0; i < PROTOCOL_FAMILIES.length; i++) {
            if ((acceptableProtocolsMask & (1 << i)) != 0) {
                frames = entry.getFrames(incrementXmitCount, PROTOCOL_FAMILIES[i], p.getCallsign());
                if (frames != null && frames.length > 0) {
                    for (AX25Frame[] oldSet : alreadySentFrames) {
                        if (Arrays.equals(oldSet, frames)) {
                            System.out.println("AlreadySentFrame");
                            continue proto_loop;
                        }
                    }
                    alreadySentFrames.add(frames);
                    for (AX25Frame frame : frames) {
                        if (frame != null) {
                            if (frame.rcptTime < 0L) {
                                frame.rcptTime = now;
                            }
                            boolean sentSuccessfully = sendFrame(entry, now, frame, p);
                            incrementXmitCount &= !sentSuccessfully;
                        } else {
                            System.out.println(new Date() + ": received null frame from " + entry);
                        }
                    }
                }
            }
        }
        return incrementXmitCount;
    }

    private int getKISSDeviceIDInCorrectBitsFromConfig() {
        return 0;
        //FIXME
        // return (currentCfg.flags & FLAGS_MASK_KISSPORT) >>> (FLAGS_SHIFT_KISSPORT - 4);
    }

    // Actually write the frame to the outputStream
    public final boolean sendFrame(AX25FrameSource entry, long now, AX25Frame frame, TransmittingConnector p) {
        // TX frames must also be passed to the stack for processing
        stack.fireConsumeAX25Frame(frame, this);

        //   fireTransmitting(true);
        int byteCount;
        try {
            if (frame.sender == null || (frame.sourcePort != null && frame.sourcePort.hasCapability(CAP_IGATE))) {
                frame.sender = new AX25Callsign(p.getCallsign());
                frame.sender.h_c = !frame.dest.h_c;
            }
            synchronized (frame) {
                kos.resetByteCount();
                kos.writeRaw(KissEscapeOutputStream.FEND);
                kos.write(getKISSDeviceIDInCorrectBitsFromConfig()); // data frame to selected TNC port (KISS device ID)
                frame.write(kos);
                kos.writeRaw(KissEscapeOutputStream.FEND);
                kos.flush();
                byteCount = kos.getByteCount();
                stats.numXmtBytes += byteCount;
                stats.numXmtFrames++;
            }
            LOG.debug(debugTag + "Sent frame: KISSByteCount:" + byteCount + "   frameByteCount:" + frame.getByteFrame().length + "   frame:" + frame + "  asciiFrame:" + frame.getAsciiFrame());
        } catch (Exception e) {
            //  fireTransmitting(false);
            //  fireFailed();
            String detail = e.getMessage();
            if (detail.indexOf("roken pipe") >= 0 || detail.indexOf(" closed") >= 0 || detail.indexOf(" reset") >= 0) {
                //       tryToRestartConnection(detail);
            }
            LOG.error(e.getMessage(), e);
            return false; // no need for TNC write delay when packet send failed
        }
        //      long now = System.currentTimeMillis();
        // int estElapsedTimeUntilPacketisTransmitted = (int)((getRFSendTimePerByte() * (byteCount + 10)));
        //  nextAllowableTransmitTime = now + estElapsedTimeUntilPacketisTransmitted;
        //   fireTransmitting(false);
        return true;
    }

    public void stop() {
        LOG.debug("Stopping:" + this);
        try {
            in.close();
        } catch (Throwable e) {
        }
        in = null;
    }

    public final void getbuf() {
        byte[] shortBuf = new byte[1];
        while (in != null) {
            int newData;
            try {
                if (in == null) {
                    break;
                }
                newData = in.read();//shortBuf[0] & 0xFF;
                if (newData == -1) {
                    break;
                }
                switch (curState) {
                    case IDLE:
                        if (KissEscapeOutputStream.FEND == newData) {
                            curState = KissEscapeOutputStream.RcvState.IN_FRAME;
                            wEnd = 0;
                        }
                        break;
                    case IN_FRAME:
                        switch (newData) {
                            case KissEscapeOutputStream.FEND:
                                // send the just-finished frame up to the next layer
                                if (wEnd > 1) {
                                    // not just a stream of frame borders....
                                    sendDecodedKissFrameToParser();
                                }
                                // reset the receive buffer for the next frame
                                wEnd = 0;
                                frameStartTime = -1L;
                                //   fireReceiving(false);
                                break;
                            case KissEscapeOutputStream.FESC:
                                if (-1L == frameStartTime) {
                                    frameStartTime = System.currentTimeMillis();
                                    // fireReceiving(true);
                                }
                                curState = KissEscapeOutputStream.RcvState.IN_ESC;
                                break;
                            default:
                                long now = System.currentTimeMillis();
                                if (-1L == frameStartTime) {
                                    frameStartTime = now;
                                    // fireReceiving(true);
                                }
                                if (wEnd < rcvBuf.length) {
                                    rcvBuf[wEnd++] = (byte) newData;
                                } else {
                                    // some kind of protocol error, so reset and start over
                                    LOG.debug(debugTag + "receive buffer overflow, must be mode garbling, reset protocol");
                                    wEnd = 0;
                                    //  fireReceiving(false);
                                    curState = KissEscapeOutputStream.RcvState.IDLE;
                                }
                                break;
                        }
                        break;
                    case IN_ESC:
                        //stats.numRcvBytes++;
                        switch (newData) {
                            case KissEscapeOutputStream.TFEND:
                                rcvBuf[wEnd++] = (byte) KissEscapeOutputStream.FEND;
                                break;
                            case KissEscapeOutputStream.TFESC:
                                rcvBuf[wEnd++] = (byte) KissEscapeOutputStream.FESC;
                                break;
                            default:
                                rcvBuf[wEnd++] = (byte) newData;
                                break;
                        }
                        curState = KissEscapeOutputStream.RcvState.IN_FRAME;
                        break;
                }
            } catch (SocketException e) {
                //  fireFailed();
                String detail = e.getMessage();
                if (detail.indexOf(" closed") >= 0 || detail.indexOf(" reset") >= 0) {
                    //   tryToRestartConnection(detail);
                }
                curState = KissEscapeOutputStream.RcvState.IDLE;
                in = null;
            } catch (SerialPortIOException e) {
                LOG.info("Serial port closed (probably reopening due to configuration change)");
                curState = KissEscapeOutputStream.RcvState.IDLE;
                in = null;
            } catch (EOFException e) {
                LOG.info("Serial port closed (probably reopening due to configuration change)");
                curState = KissEscapeOutputStream.RcvState.IDLE;
                in = null;
            } catch (Throwable e) {

                //    stats.numBadRcvFrames++;
                LOG.error("unhandled exception in KissOverTcpConnector:" + e.getMessage(), e);
                // discard this frame
                curState = KissEscapeOutputStream.RcvState.IDLE;
                in = null;

            }
        }

    }


    public void sendDecodedKissFrameToParser() {
        try {
            // Get the type byte from the frame.
            int dataType = rcvBuf[0] & 0xFF;
            switch (dataType) {
                case 0: // Normal KISS data frame.
                    AX25Frame frame = AX25Frame.decodeFrame(rcvBuf, 1, wEnd - 1, stack);
                    // Frame will be null if it was invalid, so we will ignore it.
                    if (frame != null) {
                        stack.consumeFrameNow(this, frame);
                    }
                    break;
                case 6: // KISS SetHardware reply
                default:
                    LOG.warn("Received non-AX.25 frame type: " + dataType + " (ignoring)");

            }
        } catch (Throwable e) {
            byte[] buffer = new byte[wEnd - 1];
            System.arraycopy(rcvBuf, 1, buffer, 0, wEnd - 1);
            //SingleThreadBus.INSTANCE.post(new InvalidFrameEvent(buffer, this));
            LOG.error("Exception whilst processing KISS frame: " + AX25Tools.byteArrayToReadableASCIIString(rcvBuf), e);
            LOG.error("Stacktrace:  " + e.getMessage(), e);
        }
    }

    public void startRxThread() {
        Thread rx = new Thread(() -> {
            try {
                getbuf();
            } catch (Throwable e) {
                e.printStackTrace();
                System.exit(1);
            }


        });
        rx.start();
    }

    public void startTxThread() {
        Thread tx = new Thread(() -> {
            AX25FrameSource entry = null;
            try {
                while (true) {
                    try {
                        entry = null;
                        synchronized (this) {
                            if (delayQueueHead == null) {
                                while (queue.size() == 0) {
                                    try {
                                        wait();
                                    } catch (InterruptedException e) {
                                        // do nothing, we expect to be interrupted
                                    }
                                }
                            } else {
                                long now = System.currentTimeMillis();
                                do {
                                    if (now < delayQueueHead.dueTime) {
                                        try {
                                            wait(delayQueueHead.dueTime - now);
                                        } catch (InterruptedException e) {
                                            // do nothing, we expect to be interrupted
                                        }
                                        now = System.currentTimeMillis();
                                    }
                                } while (queue.size() == 0 && delayQueueHead != null && now < delayQueueHead.dueTime);
                            }
                            if (queue.size() > 0) {
                                entry = queue.remove(0);
                            } else if (delayQueueHead != null) {
                                if (delayQueueHead.dueTime <= System.currentTimeMillis()) {
                                    entry = delayQueueHead.frameSource;
                                    delayQueueHead = delayQueueHead.next;
                                }
                            }
                        }
                        if (entry != null) {

                            long now = System.currentTimeMillis();
//                            PortConnector p = (PortConnector) entry.getConnector();
//                            boolean incrementXmitCount = true;
//                            BeaconData beaconData = null;
//                            if (entry instanceof BeaconData) {
//                                beaconData = (BeaconData) entry;
//                            }
                            if (hasCapability(CAP_XMT_PACKET_DATA)) {
                                sendForOnePort(entry, now, this, true);
                            }
                        }
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable e) {
                        if (entry != null) {
                            System.out.println("*** unhandled exception in TransmitterThread with entry type " + entry.getClass().getSimpleName() + ": " + entry);
                        } else {
                            System.out.println("*** unhandled exception in TransmitterThread with null entry");
                        }
                        e.printStackTrace(System.out);
                    }
                }
            } finally {
                LOG.info(debugTag + "terminating TransmitterThread");
            }
        });
        tx.start();
    }

    /**
     * Set some useful debug information to be included in log messages to identify this connector/stack from others
     *
     * @param tag
     */
    public void setDebugTag(String tag) {
        this.debugTag = tag;
        stack.setDebugTag(tag);
    }

    /**
     * Makes an outgoing connection to another station.
     *
     * @param from     Your originating callsign
     * @param to       the station you are connecting to.
     * @param listener the listener to be notified of connection establishment or failure
     * @return true if the connection was successfully initiated, false if there is already a connection in progress
     */
    public synchronized boolean makeConnection(String from, String to, ConnectionEstablishmentListener listener) {


        try {
            AX25Frame sabmFrame = new AX25Frame();
            sabmFrame.sender = new AX25Callsign(from);
            sabmFrame.dest = new AX25Callsign(to);
            sabmFrame.setCmd(true);
            ConnState state = stack.getConnState(sabmFrame.sender, sabmFrame.dest, true);
            //LOG.debug("State:" + state.transition);

            //LOG.debug("Transmitter.openConnection(" + sabmFrame.dest + ',' + sabmFrame.sender + ',' + Arrays.toString(state.via) + "): sending SABM U-frame");
            state.connType = ConnState.ConnType.MOD8;
            state.transition = ConnState.ConnTransition.LINK_UP;
            //sabmFrame.digipeaters = state.via;
            sabmFrame.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_SABM);
            sabmFrame.body = new byte[0];
            state.listener = listener;

            state.clearResendableFrame();
            sendFrame(sabmFrame);
            state.setConnector(BasicTransmittingConnector.this);
            state.setResendableFrame(sabmFrame, getRetransmitCount());
            state.updateSessionTime();
            stack.fireConnStateUpdated(state);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return true;
    }

    /**
     * Send a UI frame to the specified destination
     *
     * @param destinationCallsign
     * @param data
     */
    public void sendUI(String destinationCallsign, byte[] data) {
        try {
            LOG.debug("Sending UI frame to " + destinationCallsign + " contents:" + AX25Tools.byteArrayToReadableASCIIString(data));
            AX25Frame uiFrame = new AX25Frame();
            uiFrame.sender = defaultCallsign;
            uiFrame.dest = new AX25Callsign(destinationCallsign);
            uiFrame.setCmd(true);
            uiFrame.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_UI);
            uiFrame.body = data;
            sendFrame(uiFrame);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

    }

    /**
     * Cancel a current connection setup attempt
     *
     * @param from
     * @param to
     */
    public void cancelConnection(String from, String to) {
        ConnState state = stack.getConnState(new AX25Callsign(from), new AX25Callsign(to), false);
        LOG.debug("Cancel connection: " + from + " to " + to + ", stack=" + stack);

        if (state != null) {
            state.transition = ConnState.ConnTransition.LINK_DOWN;
            if (state.listener != null) {
                state.listener.connectionNotEstablished(this, "Connection cancelled");
            }
        }
    }

    public void disconnect(String from, String to) {
        ConnState state = stack.getConnState(new AX25Callsign(from), new AX25Callsign(to), false);
        if (state != null) {
            stack.transmitDISC(this, state.getSrc(), state.getDst(), new AX25Callsign[]{}, true);
        }
    }

    /**
     * Wrapper class for frames delay-queued for transmission.
     */
    private static class TimedQueueEntry implements Comparable<TimedQueueEntry> {
        TimedQueueEntry next = null;
        AX25FrameSource frameSource;
        long dueTime;

        TimedQueueEntry(AX25FrameSource frameSource, long dueTime) {
            this.frameSource = frameSource;
            this.dueTime = dueTime;
        }

        /**
         * Compares this object with the specified object for order.  Returns a
         * negative integer, zero, or a positive integer as this object is less
         * than, equal to, or greater than the specified object.
         *
         * @param o the object to be compared.
         * @return a negative integer, zero, or a positive integer as this object
         * is less than, equal to, or greater than the specified object.
         * @throws NullPointerException if the specified object is null
         * @throws ClassCastException   if the specified object's type prevents it
         *                              from being compared to this object.
         */
        public int compareTo(TimedQueueEntry o) {
            return Long.signum(dueTime - o.dueTime);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TimedQueueEntry tqe) {
                if (dueTime == tqe.dueTime) {
                    return frameSource.equals(tqe.frameSource);
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return (int) dueTime;
        }

        @Override
        public String toString() {
            return "TimedQueueEntry[@" + dueTime + ',' + frameSource + ']';
        }
    }
}


