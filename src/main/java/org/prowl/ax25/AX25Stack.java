package org.prowl.ax25;
/*
 * Copyright (C) 2011-2023 Andrew Pavlin, KA2DDO
 * This file is part of YAAC (Yet Another APRS Client).
 *
 *  YAAC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAAC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and GNU Lesser General Public License along with YAAC.  If not,
 *  see <http://www.gnu.org/licenses/>.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.ax25.util.AX25Tools;
import org.prowl.ax25.util.FastBlockingQueue;
import org.prowl.ax25.util.ReschedulableTimer;

import java.io.*;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class implements the internal AX.25 (v2.2) protocol stack for a TNC (layer 2), as defined in the
 * <a href="https://ax25.net/AX25.2.2-Jul%2098-2.pdf" target="ax.25">AX.25 Link Access
 * Protocol for Amateur Packet Radio</a> specification.
 * It does not implement the level 7 applications or any layer 3 protocols.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public class AX25Stack implements FrameListener, Runnable {
    /**
     * Time interval (in milliseconds) to wait for an acknowledgement from the
     * other end of a AX.25 connection.
     * <p>
     * If we are feeding KISS frames, then this may allow (in the worst case) MAXLEN frames of 255 PACLEN to be sent over
     * a 300 baud connection, but the send routine returns immediately so the caller doesn't know that they have not actually
     * been sent (but starts the T1 ack timer anyway).  It may take > 30 seconds to send all those frames, especially on a
     * busy channel!
     * <p>
     * The correct solution would be to correctly configure maxframes and paclen for the speed of the channel and then the timer can
     * possibly be sized appropriately.
     */
    public static final long WAIT_FOR_ACK_T1_TIMER_MINIMUM = 1000L * 15L; // 15 seconds minimum
    public static final long WAIT_FOR_ACK_T1_TIMER_MAXIMUM = 1000L * 300L; // 300 seconds maximum
    private static final Log LOG = LogFactory.getLog("AX25Stack");
    private static final int MAX_FRAMES_BEFORE_FREEZE_CHECK = 50;
    private final HashMap<AX25Callsign, Map<AX25Callsign, ConnState>> connMap = new LinkedHashMap<>();
    private final ArrayList<AX25FrameListener> ax25FrameListeners = new ArrayList<>();
    private final ArrayList<ParsedAX25MessageListener> parsedAX25MessageListenerList = new ArrayList<>();
    private final FastBlockingQueue<AX25Frame> frameParserQueue = new FastBlockingQueue<>(4096);
    private final Thread parserThread;
    private final HashMap<Byte, AX25Parser> protocolParserMap = new HashMap<>();
    private final ReschedulableTimer retransTimer = new ReschedulableTimer("AX.25 Retransmit Timer");
    private final ArrayList<ConnStateChangeListener> connStateListeners = new ArrayList<>();
    public long WAIT_FOR_ACK_T1_TIMER = WAIT_FOR_ACK_T1_TIMER_MINIMUM;
    public int maxFrames = 3; // Sensible default for maxframes
    public int baudRateInBitsPerSecond = 1200; // Normally used baud rate
    int pacLen = 112; // Sensible default for paclen.
    private String[] digipeaters;
    private String toCall;
    private transient ConnectionRequestListener connectionRequestListener = null;
    private boolean allowInboundConnectedMode = true;
    private transient int maxBacklog = 0;
    private transient int numConsumedMsgs = 0;
    private transient boolean frozen = false;
    private AX25ParserWithDistributor aprsParser = null;
    private Transmitting transmitting = null;
    // Create a settable debug tag that can be useful when using multiple stacks.
    private String debugTag = "";

    /**
     * Instantiate the AX25Stack parser thread. Note this should not be done until
     * any dynamically loaded JAR files are added to the classpath and context
     * class loader of the invoking thread, so anything this thread invokes has
     * full access to all defined classes.
     */
    public AX25Stack(int pacLen, int maxFrames, int baudRateInBitsPerSecond) {
        // First configure the stack based on supplied information
        this.pacLen = pacLen;
        this.maxFrames = maxFrames;
        this.baudRateInBitsPerSecond = baudRateInBitsPerSecond;
        configure();

        // Now start the parser thread
        parserThread = new Thread(this, "AX25Stack parser");
        parserThread.setDaemon(true);
        parserThread.start();
    }


    /**
     * Configure the stack for use with the supplied settings.
     */
    public void configure() throws AssertionError {

        // Check settings.
        if (pacLen < 3 || pacLen > 256) {
            throw new AssertionError("pacLen is out of range");
        }

        if (maxFrames < 1 || maxFrames > 7) {
            throw new AssertionError("maxFrames is out of range");
        }

        if (baudRateInBitsPerSecond < 300 || baudRateInBitsPerSecond > 115200) {
            throw new AssertionError("baudRateInBitsPerSecond is out of range");
        }

        // Attempt to configure timers appropriately based on the transmitting configuration.
        long candidateSpeed = maxFrames * pacLen * 40000L / baudRateInBitsPerSecond;

        // Make sure we don't go below a sensible minimum or maximum(to prevent stalled connections)
        WAIT_FOR_ACK_T1_TIMER = Math.max(candidateSpeed, WAIT_FOR_ACK_T1_TIMER_MINIMUM);
        WAIT_FOR_ACK_T1_TIMER = Math.min(WAIT_FOR_ACK_T1_TIMER, WAIT_FOR_ACK_T1_TIMER_MAXIMUM);

        LOG.debug(debugTag + "Configured stack ACK1 timer: " + WAIT_FOR_ACK_T1_TIMER + "ms");
    }

    public long getWaitForAckT1Timer() {
        return WAIT_FOR_ACK_T1_TIMER;
    }

    /**
     * Get the default list of digipeaters for this stack.
     *
     * @return array of String digipeater aliases supported by this stack
     */
    public String[] getDigipeaters() {
        return digipeaters;
    }

    /**
     * Set the default list of digipeaters for this stack.
     *
     * @param digipeaters array of String digipeater aliases
     */
    public void setDigipeaters(String[] digipeaters) {
        this.digipeaters = Arrays.copyOf(digipeaters, digipeaters.length);
    }

    /**
     * Get the destination callsign (tocall) that should be used for messages originated
     * by this station.
     *
     * @return destination callsign (tocall)
     */
    public String getToCall() {
        return toCall;
    }

    /**
     * Set the destination callsign (tocall) that should be used for messages originated
     * by this station.
     *
     * @param toCall destination callsign (tocall)
     */
    public void setToCall(String toCall) {
        this.toCall = toCall;
    }

    /**
     * Get the Transmitting object that this AX25Stack will use for implicit but unrouted
     * transmissions.
     *
     * @return Transmitting object
     */
    public Transmitting getTransmitting() {
        return transmitting;
    }

    /**
     * Set the Transmitting object that this AX25Stack will use for implicit but unrouted
     * transmissions.
     *
     * @param transmitting Transmitting object
     */
    public void setTransmitting(Transmitting transmitting) {
        this.transmitting = transmitting;
    }

    /**
     * Get the retransmit timer for outbound AX.25 frames.
     *
     * @return ReschedulableTimer
     */
    public ReschedulableTimer getRetransTimer() {
        return retransTimer;
    }

    /**
     * Indicate whether this system accepts inbound connected-mode connection requests.
     *
     * @return boolean true if inbound connection requests should be accepted
     */
    public boolean isAllowInboundConnectedMode() {
        return allowInboundConnectedMode;
    }

    /**
     * Specify whether this system accepts inbound connected-mode connection requests.
     *
     * @param allowInboundConnectedMode boolean true if inbound connection requests should be accepted
     */
    public void setAllowInboundConnectedMode(boolean allowInboundConnectedMode) {
        this.allowInboundConnectedMode = allowInboundConnectedMode;
    }

    /**
     * Add a listener for incoming AX.25 frames.
     *
     * @param l AX25FrameListener to register
     */
    public void addAX25FrameListener(AX25FrameListener l) {
        for (AX25FrameListener f : ax25FrameListeners) {
            if (f == l) {
                return;
            }
        }
        ax25FrameListeners.add(l);
    }

    /**
     * Remove a listener for incoming AX.25 frames.
     *
     * @param l AX25FrameListener to unregister
     */
    public void removeAX25FrameListener(AX25FrameListener l) {
        for (Iterator<AX25FrameListener> it = ax25FrameListeners.iterator(); it.hasNext(); ) {
            AX25FrameListener f = it.next();
            if (f == l) {
                it.remove();
                return;
            }
        }
    }

    /**
     * Set the handler for inbound connection requests.
     *
     * @param l ConnectionRequestListener to register
     */
    public void setConnectionRequestListener(ConnectionRequestListener l) {
        if (connectionRequestListener != null && l != null && connectionRequestListener != l) {
            LOG.warn(debugTag + "connectionRequestListener being changed behind original one's back, was " + connectionRequestListener + ", now " + l);
        }
        connectionRequestListener = l;
    }

    ConnectionRequestListener getConnectionRequestListener() {
        return connectionRequestListener;
    }

    /**
     * Register another protocol parsing handler for a particular AX.25 UI/I message PID.
     *
     * @param pid    pid byte code for this protocol
     * @param parser AX25Parser to handle receiving this protocol
     */
    public void registerProtocolParser(byte pid, AX25Parser parser) {
        int maskedPid = pid & 0x30;
        if (maskedPid == 0x10 || maskedPid == 0x20) {
            throw new IllegalArgumentException("can't specify any PID colliding with Level 3 AX.25");
        }
        protocolParserMap.put(pid, parser);
        if (pid == AX25Frame.PID_NOLVL3 && parser instanceof AX25ParserWithDistributor) {
            aprsParser = (AX25ParserWithDistributor) parser;
        }
    }

    /**
     * Get the protocol parsing handler for the specified protocol.
     *
     * @param pid level 3 protocol ID byte (as defined by the AX.25 protocol specification)
     * @return AX25Parser object, or null if no parser registered for the specified protocol
     */
    public AX25Parser getParser(byte pid) {
        return protocolParserMap.get(pid);
    }

    /**
     * Send the specified incoming frame to all registered AX25FrameListeners.
     *
     * @param frame     AX25Frame to dispatch
     * @param connector Connector that received the frame
     */
    public void fireConsumeAX25Frame(AX25Frame frame, Connector connector) {
        ArrayList<AX25FrameListener> ax25FrameListeners = this.ax25FrameListeners;
        for (int i = 0; i < ax25FrameListeners.size(); i++) {
            ax25FrameListeners.get(i).consumeAX25Frame(frame, connector);
        }
    }

    /**
     * Queue one AX.25 frame (containing some sort of message) for parsing and processing.
     *
     * @param frame the AX25Frame to be processed
     */
    public void consumeFrame(AX25Frame frame) {
        try {
            FastBlockingQueue<AX25Frame> frameParserQueue;
            if (!(frameParserQueue = this.frameParserQueue).offer(frame)) {
                LOG.warn(debugTag + "AX25Stack parser queue filled up, parsing thread can't keep up from " + Thread.currentThread());
                frameParserQueue.put(frame);
            }
            int sz;
            if ((sz = frameParserQueue.fastSize()) > maxBacklog) {
                maxBacklog = sz;
            }
        } catch (InterruptedException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Return the current number of backlogged frames to be processed by the AX25Stack thread.
     *
     * @return current queue size
     */
    public int getCurrentBacklog() {
        return frameParserQueue.size();
    }

    /**
     * Return the maximum backlog ever incurred by the AX25Stack parser thread.
     *
     * @return maximum queue backlog
     */
    public int getMaxBacklog() {
        return maxBacklog;
    }

    /**
     * Return the maximum allowed size of the queue.
     *
     * @return total capacity of the backlog queue
     */
    public int getMaxQueueCapacity() {
        return frameParserQueue.remainingCapacity() + frameParserQueue.size();
    }

    /**
     * Return the number of AX.25 messages consumed by the AX25Stack.
     *
     * @return count of messages consumed
     */
    public int getNumConsumedMsgs() {
        return numConsumedMsgs;
    }

    /**
     * Private method to asynchronously consume AX25Frame objects for parsing. This tries to
     * prevent back-pressuring any sources (particularly fast-moving TCP connections to
     * APRS-IS servers). Can only be executed from the singleton thread created by this class.
     */
    public void run() {
        final AX25Frame[] deqBuf = new AX25Frame[MAX_FRAMES_BEFORE_FREEZE_CHECK];
        while (Thread.currentThread() == parserThread) {
            processQueuedFrames(deqBuf);
        }
    }

    /**
     * Private method to process frames queued for processing. Factored from above method into
     * a separate method so hotspot JIT compiler will be inspired to compile this method for better
     * performance.
     *
     * @param deqBuf array to buffer dequeued AX.25 frames
     */
    private void processQueuedFrames(AX25Frame[] deqBuf) {
        final FastBlockingQueue<AX25Frame> frameParserQueue = this.frameParserQueue;
        int numFrames;
        if ((numFrames = frameParserQueue.drainTo(deqBuf)) > 0) {
            for (int i = 0; i < numFrames; i++) {
                try {
                    AX25Frame frame = deqBuf[i];
                    consumeFrameNow(frame.sourcePort, frame);
                } catch (Throwable e) {
                    LOG.error(" AX25Stack parser thread reported exception while parsing an AX25Frame: ", e);
                }
            }
            numConsumedMsgs += numFrames;
        }

        // allow stalling processing so you can look at the state without it moving
        if (frozen) {
            synchronized (this) {
                while (frozen) {
                    try {
                        wait(5000L);
                    } catch (InterruptedException e) {
                        // do nothing, the loop will handle it
                    }
                }
            }
        } else {
            synchronized (frameParserQueue) {
                if (frameParserQueue.fastSize() == 0) {
                    try {
                        frameParserQueue.wait(2000L);
                    } catch (InterruptedException e) {
                        // do nothing, the loop will handle it
                    }
                }
            }
        }
    }

    /**
     * Report if parser thread is frozen.
     *
     * @return boolean true if parser thread is stopped
     */
    public synchronized boolean isFrozen() {
        return frozen;
    }

    /**
     * Specify whether the AX.25 parser thread should be paused (frozen) to allow the user to view
     * the output without it moving constantly
     *
     * @param frozen boolean true to freeze the parser thread, or false to resume execution
     */
    public synchronized void setFrozen(boolean frozen) {
        boolean oldFrozen = this.frozen;
        this.frozen = frozen;
        if (oldFrozen && !frozen) {
            notifyAll(); // wake it up if it was frozen
        }
    }

    /**
     * Process an incoming AX.25 frame.
     *
     * @param connector Connector that received the frame
     * @param frame     AX25Frame to process
     */
    public synchronized void consumeFrameNow(Connector connector, AX25Frame frame) {
        boolean toMe = isLocalDest(frame.dest);
        boolean msgReported = false;

        // decode the frame
        int frameType;
        ConnState state;
        if ((frameType = frame.getFrameType()) == AX25Frame.FRAMETYPE_U) {
            int uType;
            if ((uType = frame.getUType()) == AX25Frame.UTYPE_UI) {
                msgReported = processIBody(frame, true, connector, frame.rcptTime);
            } else if (uType == AX25Frame.UTYPE_DISC) {
                LOG.debug(debugTag + " rcvd " + frame.getFrameTypeString() + (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest);
                if ((state = getConnState(frame.dest, frame.sender, false)) == null) {
                    // it doesn't matter who started the connection; either end can terminate it
                    state = getConnState(frame.sender, frame.dest, false);
                }
                if (state != null) {
                    state.transition = ConnState.ConnTransition.STEADY;
                    state.setConnType(ConnState.ConnType.CLOSED);
                    state.connector = null;
                    if (toMe) {
                        transmitUA(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                        if (state.listener != null) {
                            state.listener.connectionClosed(state.sessionIdentifier, true);
                        }
                    }
                    state.clearResendableFrame();
                    removeConnState(state);
                    fireConnStateAddedOrRemoved();
                }
            } else if (uType == AX25Frame.UTYPE_DM) {
                LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " F " : ' ') + frame.sender + "->" + frame.dest);
                if ((state = getConnState(frame.dest, frame.sender, false)) == null) {
                    // it doesn't matter who started the connection; either end can terminate it
                    state = getConnState(frame.sender, frame.dest, false);
                }
                if (state != null) {
                    switch (state.transition) {
                        case LINK_UP:
                            state.transition = ConnState.ConnTransition.STEADY;
                            state.setConnType(ConnState.ConnType.CLOSED);
                            if (state.listener != null) {
                                state.listener.connectionNotEstablished(state.sessionIdentifier, new ConnectException("rejected by remote station"));
                            }
                            break;
                        case LINK_DOWN:
                            state.transition = ConnState.ConnTransition.STEADY;
                            state.setConnType(ConnState.ConnType.CLOSED);
                            if (state.listener != null) {
                                state.listener.connectionClosed(state.sessionIdentifier, false);
                            }
                            break;
                        default:
                            break;
                    }
                    state.clearResendableFrame();
                    removeConnState(state);
                    fireConnStateAddedOrRemoved();
                }
            } else if (uType == AX25Frame.UTYPE_SABM) {
                LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest);
                boolean isNewSession = false;
                state = getConnState(frame.sender, frame.dest, false);
                if (state == null) {
                    state = getConnState(frame.sender, frame.dest, true);
                    isNewSession = true;
                }
                if (frame.digipeaters != null) {
                    state.via = Arrays.copyOf(frame.digipeaters, frame.digipeaters.length);
                }
                if (toMe) {
                    if (connector instanceof TransmittingConnector) {
                        if (!state.isOpen()) {
                            if (allowInboundConnectedMode && connectionRequestListener != null &&
                                    connectionRequestListener.acceptInbound(state, frame.sender, connector)) {
                                state.setConnType(ConnState.ConnType.MOD8);
                                state.transition = ConnState.ConnTransition.STEADY;
                                state.reset();
                                state.connector = (TransmittingConnector) connector;
                                transmitUA(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                                try {
                                    // ensure incoming I frames don't get lost
                                    state.getInputStream();
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                                if (state.listener != null) {
                                    state.listener.connectionEstablished(state.sessionIdentifier, state);
                                }
                            } else {
                                transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                                removeConnState(state);
                                isNewSession = true; // not really, but it makes the fireXXX() logic work correctly
                            }
                        } else {
                            transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                            removeConnState(state);
                            isNewSession = true; // not really, but it makes the fireXXX() logic work correctly
                        }
                    }
                } else {
                    // record state change of other stations' connection attempt
                    state.transition = ConnState.ConnTransition.LINK_UP;
                    state.connType = ConnState.ConnType.MOD8;
                }
                if (isNewSession) {
                    fireConnStateAddedOrRemoved();
                } else {
                    fireConnStateUpdated(state);
                }
            } else if (uType == AX25Frame.UTYPE_SABME) {
                LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest);
                boolean isNewSession = false;
                state = getConnState(frame.sender, frame.dest, false);
                if (state == null) {
                    state = getConnState(frame.sender, frame.dest, true);
                    isNewSession = true;
                }
                if (frame.digipeaters != null) {
                    state.via = Arrays.copyOf(frame.digipeaters, frame.digipeaters.length);
                }
                if (toMe) {
                    if (connector instanceof TransmittingConnector) {
                        if (!state.isOpen()) {
                            if (allowInboundConnectedMode && connectionRequestListener != null &&
                                    connectionRequestListener.acceptInbound(state, frame.sender, connector)) {
                                state.setConnType(ConnState.ConnType.MOD128);
                                state.transition = ConnState.ConnTransition.STEADY;
                                state.reset();
                                state.connector = (TransmittingConnector) connector;
                                transmitUA(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                                try {
                                    // ensure incoming I frames don't get lost
                                    state.getInputStream();
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                                if (state.listener != null) {
                                    state.listener.connectionEstablished(state.sessionIdentifier, state);
                                }
                            } else {
                                transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                                removeConnState(state);
                                isNewSession = true; // not really, but it makes the fireXXX() logic work correctly
                            }
                        } else {
                            //TODO: should be link reset per AX.25 spec section 6.2
                            transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), false);
                            removeConnState(state);
                            isNewSession = true; // not really, but it makes the fireXXX() logic work correctly
                        }
                    }
                } else {
                    // record state change of other stations' connection attempt
                    state.transition = ConnState.ConnTransition.LINK_UP;
                    state.connType = ConnState.ConnType.MOD128;
                }
                if (isNewSession) {
                    fireConnStateAddedOrRemoved();
                } else {
                    fireConnStateUpdated(state);
                }
            } else if (uType == AX25Frame.UTYPE_UA) {
                LOG.debug(debugTag + " UTYPE_UA rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " F " : ' ') + frame.sender + "->" + frame.dest);
                if ((state = getConnState(frame.dest, frame.sender, false)) != null) {
                    LOG.debug("Connstate: " + state);
                    switch (state.transition) {
                        case LINK_UP:
                            state.transition = ConnState.ConnTransition.STEADY;
                            state.clearResendableFrame();
                            if (toMe) {
                                try {
                                    // ensure incoming I frames don't get lost
                                    state.getInputStream();
                                } catch (IOException e) {
                                    LOG.error(e.getMessage(), e);
                                }
                            }
                            // type remains MOD8 or MOD128 as it was set when sending the SABM(E)
                            if (state.listener != null) {
                                state.listener.connectionEstablished(state.sessionIdentifier, state);
                            }
                            state.connector = (TransmittingConnector) connector;
                            state.updateSessionTime();
                            fireConnStateUpdated(state);
                            break;
                        case STEADY: // ijh
                            state.transition = ConnState.ConnTransition.LINK_DOWN;
                            state.setConnType(ConnState.ConnType.CLOSED);
                            if (state.listener != null) {
                                state.listener.connectionClosed(state.sessionIdentifier, false);
                            }
                            state.clearResendableFrame();
                            removeConnState(state);
                            fireConnStateAddedOrRemoved();
                            break;
                        case LINK_DOWN:
                            state.transition = ConnState.ConnTransition.STEADY;
                            state.setConnType(ConnState.ConnType.CLOSED);
                            if (state.listener != null) {
                                state.listener.connectionClosed(state.sessionIdentifier, false);
                            }
                            state.clearResendableFrame();
                            removeConnState(state);
                            fireConnStateAddedOrRemoved();
                            break;
                        default:
                            break;
                    }
                }
            } else if (uType == AX25Frame.UTYPE_TEST) {
                LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest);
                if (toMe && frame.getP()) {
                    frame = frame.dup();
                    frame.sourcePort = null;
                    AX25Callsign swap = frame.sender;
                    frame.sender = frame.dest;
                    frame.dest = swap;
                    frame.digipeaters = reverseDigipeaters(frame.digipeaters);
                    frame.ctl &= ~AX25Frame.MASK_U_P;
                    // transmit to appropriate channel
                    if (connector != null) {
                        transmitting.queue(new FrameWrapper(frame, null, connector));
                    }
                }
            } else if (uType == AX25Frame.UTYPE_XID) {
                LOG.debug(debugTag + "xid rcvd " + frame.getFrameTypeString() + (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest);
                if (frame.isCmd) {
                    if (toMe) {
                        // all clear, so generate our XID response
                        AX25Frame resp = new AX25Frame();
                        resp.sender = frame.dest.dup();
                        resp.dest = frame.sender.dup();
                        resp.setCmd(false);
                        resp.digipeaters = reverseDigipeaters(frame.digipeaters);
                        resp.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_XID | (frame.ctl & ~AX25Frame.MASK_U_P));
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(bos);
                        XIDGroup g = new XIDGroup();
                        g.paramList.add(new XIDParameter((byte) 2, (short) 0x21)); // classes of procedures: balanced-ABM, half-duplex
                        g.paramList.add(new XIDParameter((byte) 3, 0x02AC86, true)); // optional functions
                        g.paramList.add(new XIDParameter((byte) 5, (short) 0x0800)); // I Field Length max = 2048 bits = 256 octets
                        g.paramList.add(new XIDParameter((byte) 6, (short) 0x0800)); // I Field Length max = 2048 bits = 256 octets
                        g.paramList.add(new XIDParameter((byte) 8, (byte) 7)); // Window Size Receive 7 frames (since we support AX.25 v2.2)
                        g.paramList.add(new XIDParameter((byte) 9, (short) WAIT_FOR_ACK_T1_TIMER)); // Wait for Acknowledge timer (T1) = 3000 milliseconds
                        int numRetries = (transmitting != null) ? transmitting.getRetransmitCount() : 3;
                        g.paramList.add(new XIDParameter((byte) 10, (byte) numRetries)); // Retries 3 times
                        try {
                            g.write(dos);
                            resp.body = bos.toByteArray();
                            // if P/F not set in command, we can queue this instead of jumping the queue
                            if (!frame.isCmd && transmitting != null) {
                                transmitting.queue(resp);
                            } else {
                                ((TransmittingConnector) connector).sendFrame(resp);
                            }
                        } catch (IOException e) {
                            // report transmission failure???
                            LOG.error(e.getMessage(), e);
                        }
                        msgReported = true;
                    }
                } else {
                    // log and process receiving a XID response from somebody
                    ByteArrayInputStream bais = new ByteArrayInputStream(frame.body);
                    DataInputStream dis = new DataInputStream(bais);
                    try {
                        XIDGroup g = XIDGroup.read(dis);

                        StringBuilder sb = new StringBuilder();
                        for (XIDParameter p : g.paramList) {
                            sb.append(p);
                            sb.append(" ");
                            //TODO: do something with the XID response
                        }
                        LOG.debug(debugTag + "recv xid addressed to " + frame.dest + " XID:" + sb);

                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            } else if (uType == AX25Frame.UTYPE_FRMR) {
                LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " F " : ' ') + frame.sender + "->" + frame.dest);
                // if we tried to open a V2.2 connection to a V2.0 station, try again falling back to v2.0 limitations
                if (toMe) {
                    if ((state = getConnState(frame.dest, frame.sender, false)) != null &&
                            state.transition == ConnState.ConnTransition.LINK_UP &&
                            state.connType == ConnState.ConnType.MOD128 &&
                            transmitting != null) {
                        LOG.debug(debugTag + "Transmitter.openConnection(" + frame.dest + ',' + frame.sender + ',' + Arrays.toString(state.via) + "): sending SABM U-frame");
                        state.connType = ConnState.ConnType.MOD8;
                        AX25Frame sabmFrame = new AX25Frame();
                        sabmFrame.sender = frame.dest.dup();
                        sabmFrame.dest = frame.sender.dup();
                        sabmFrame.setCmd(true);
                        sabmFrame.digipeaters = state.via;
                        sabmFrame.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_SABM);
                        sabmFrame.body = new byte[0];
                        transmitting.queue(sabmFrame);
                        state.setResendableFrame(sabmFrame, transmitting.getRetransmitCount());
                        state.updateSessionTime();
                        fireConnStateUpdated(state);
                    }
                }
            } else {
                LOG.debug(debugTag + " rcvd: unrecognized " + frame.getFrameTypeString() + (frame.getP() ? " P" : ' ') + ' ' + frame.sender + "->" + frame.dest);
            }
        } else if (frameType == AX25Frame.FRAMETYPE_I) {
            if ((state = getConnState(frame.dest, frame.sender, false)) == null) {
                // it doesn't matter who started the connection; either end can send I-frames
                state = getConnState(frame.sender, frame.dest, false);
            }
            if (state != null && state.isOpen()) {
                if (toMe) {
                    LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() +
                            (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest + " NS=" + frame.getNS() +
                            " NR=" + frame.getNR() + " #=" + frame.body.length + " VR=" + state.modReceivedFrameIndex + " body:" + AX25Tools.byteArrayToHexString(frame.body));
                    // check frame number against flow control
                    int ns = frame.getNS();

                    if (ns == state.modReceivedFrameIndex) {
                        if (state.localRcvBlocked) {
                            transmitRNR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, false);
                        } else {
                            state.modReceivedFrameIndex = (state.modReceivedFrameIndex + 1) % (state.getConnType() == ConnState.ConnType.MOD128 ? 128 : 8);
                            //TODO: wait until we have enough processed to be worth wasting airtime on an RR packet
                            transmitRR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, false);
                            msgReported = processIBody(frame, false, connector, System.currentTimeMillis());
                            if (state.in != null) {
                                // send frame to AX25InputStream for processing
                                state.in.add(frame);
                            }
                        }
                    } else {
                        //TODO: delay a bit until we can send a SREJ in case it's only a one-packet drop, and also to debounce sending this multiple times
                        transmitREJ(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, false);
                    }
                    // update received state for other end
                    int nr = frame.getNR();
                    boolean ackFrames = false;
                    while (state.modAcknowledgedFrameIndex != nr) {
                        if (state.transmitWindow != null) {
                            if (state.transmitWindow[state.modAcknowledgedFrameIndex] != null) {
                                state.transmitWindow[state.modAcknowledgedFrameIndex] = null;
                                ackFrames = true; //TODO: did we ack the frame we are currently sending?

                            }
                        }
                        state.modAcknowledgedFrameIndex = (state.modAcknowledgedFrameIndex + 1) % (state.getConnType() == ConnState.ConnType.MOD128 ? 128 : 8);
                    }

                    if (ackFrames) {
                        state.clearResendableFrame();
                        state.xmtToRemoteBlocked = false;
                    }
                }
                state.updateSessionTime();
                fireConnStateUpdated(state);
            } else {
                if (toMe && frame.isCmd) {
                    transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), true);
                }
            }

            // Supervisory control frame field
        } else if (frameType == AX25Frame.FRAMETYPE_S) {
            LOG.debug(debugTag + " rcvd: " + frame.getFrameTypeString() + (frame.getP() ? " P " : ' ') + frame.sender + "->" + frame.dest + " NR=" + frame.getNR());

            if ((state = getConnState(frame.sender, frame.dest, false)) == null) {
                state = getConnState(frame.dest, frame.sender, false);
            }
            if (state != null && state.isOpen()) {
                ConnState.ConnType connType;
                if (((connType = state.getConnType()) == ConnState.ConnType.MOD128 || connType == ConnState.ConnType.MOD8)) {
                    int nr;
                    boolean ackFrames;
                    switch (frame.getSType()) {

                        // Receive Ready
                        //  - indicates that the sender of the RR is now able to receive more I frames.
                        //  - acknowledges all I frames up to N(R)-1.
                        //  - clears a previously set RNR busy condition.
                        case AX25Frame.STYPE_RR:
                            state.xmtToRemoteBlocked = false;
                            if (frame.getP() && frame.isCmd) {
                                if (toMe) {
                                    // tell other end what our state is
                                    if (state.localRcvBlocked) {
                                        transmitRNR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, true);
                                    } else {
                                        transmitRR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, true);
                                    }
                                }
                            }
                            // update received state for other end
                            nr = frame.getNR();
                            ackFrames = false;
                            while (state.modAcknowledgedFrameIndex != nr) {
                                if (state.transmitWindow != null) {
                                    if (state.transmitWindow[state.modAcknowledgedFrameIndex] != null) {
                                        state.transmitWindow[state.modAcknowledgedFrameIndex] = null;
                                        ackFrames = true;
                                    }
                                }
                                state.modAcknowledgedFrameIndex = (state.modAcknowledgedFrameIndex + 1) % (state.getConnType() == ConnState.ConnType.MOD128 ? 128 : 8);
                            }

                            // Must resend frames if we the acked frame counter is not the same as we have sent.
                            //ackFrames = ackFrames | (state.modAcknowledgedFrameIndex != state.modSentFrameIndex);
                            if (ackFrames) {
                                state.clearResendableFrame();
                                AX25Frame f;
                                if (state.transmitWindow != null && (f = state.transmitWindow[state.modAcknowledgedFrameIndex]) != null) {                                        // send the next frame
                                    if (state.connector != null) {
                                        try {
                                            state.connector.sendFrame(f);
                                        } catch (IOException e) {
                                            LOG.error(e.getMessage(), e);
                                        }
                                    } else {
                                        transmitting.queue(f);
                                    }
                                    // Once it's been successfully queued(as the queue may block), we can set the resendable frame timer off!
                                    //state.setResendableFrame(f, state.getNumTransmitsBeforeDecay());

                                    LOG.debug(debugTag + " sending I frame " + f.sender + "->" + f.dest + " NS=" + f.getNS() + " NR=" + f.getNR() + " #=" + f.body.length);
                                }
                            }
                            //TODO: note that this might not include all frames we have sent, if so, requeue T1 timer
                            break;

                        // RNR frames are used to tell the other end that we are not ready to receive more I frames.
                        // Frames up to N(R)-1 are acknowledged. Frames at N(R) and above are considered discarded and must be retransmitted when
                        // the busy condition clears.
                        // The RNR condition is cleared by the sending of a UA, RR, REJ, or SABM(E) frame.
                        // The status fo the TNC at the other end of the link is requested by sending an RNR command frame with the P bit set.
                        case AX25Frame.STYPE_RNR:
                            if (frame.getP() && frame.isCmd) {
                                if (toMe) {
                                    // tell other end what our state is
                                    if (state.localRcvBlocked) {
                                        transmitRNR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, true);
                                    } else {
                                        transmitRR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, true);
                                    }
                                }
                            }
                            state.xmtToRemoteBlocked = true;
                            // update received state for other end
                            nr = frame.getNR();
                            ackFrames = false;
                            while (state.modAcknowledgedFrameIndex != nr) {
                                if (state.transmitWindow != null) {
                                    if (state.transmitWindow[state.modAcknowledgedFrameIndex] != null) {
                                        state.transmitWindow[state.modAcknowledgedFrameIndex] = null;
                                        ackFrames = true; //TODO: did we ack the frame we are currently sending?
                                    }
                                }
                                state.modAcknowledgedFrameIndex = (state.modAcknowledgedFrameIndex + 1) % (state.getConnType() == ConnState.ConnType.MOD128 ? 128 : 8);
                            }
                            if (ackFrames) {
                                state.clearResendableFrame();
                            }
                            break;

                        // REJ frames are used to request retransmission of I frames starting with the N(R).
                        // Additional I frames that may exist after this may be appended to the retransmission of the requested N(R) I frame.
                        // Only 1 REJ frame is allowed in each direction at a time. The REJ condition is cleared by the proper reception of I frames
                        // up to the I frame that caused the reject condition
                        // The status of the TNC at the other end of the link is requested by sending a REJ command frame with the P bit set.
                        case AX25Frame.STYPE_REJ:
                            if (frame.getP()) {
                                if (toMe) {
                                    // tell other end what our state is
                                    if (state.localRcvBlocked) {
                                        transmitRNR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, true);
                                    } else {
                                        transmitRR(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), state, true);
                                    }
                                }
                            }

                        {

                            LOG.debug(debugTag + " 1:  state.va=" + state.modAcknowledgedFrameIndex + "  nr=" + frame.getNR());

                            int newVA = frame.getNR();
                            state.xmtToRemoteBlocked = false;
                            int nextVA = state.modAcknowledgedFrameIndex;
                            // mark off the frames that were acknowledged
                            while (nextVA != newVA) {

                                LOG.debug(debugTag + " 2:markingOff:" + state.transmitWindow + "  newVA=" + newVA + "    nextVA=" + nextVA);

                                if (state.transmitWindow != null) {
                                    LOG.debug(debugTag + " 2:markingOff:" + state.transmitWindow[nextVA]);
                                    state.transmitWindow[nextVA] = null;
                                }
                                state.modAcknowledgedFrameIndex = nextVA;
                                nextVA = (nextVA + 1) % (connType == ConnState.ConnType.MOD128 ? 128 : 8);
                            }

                            if (state.transmitWindow != null) {
                                // force retransmissions of rejected frames
                                do {
                                    AX25Frame f = state.transmitWindow[nextVA];
                                    if (f != null) {
                                        f.setNR(state.modReceivedFrameIndex);
                                        f.setNS(nextVA);
                                        if (state.connector != null) {
                                            try {
                                                state.connector.sendFrame(f);
                                            } catch (IOException e) {
                                                LOG.error(e.getMessage(), e);
                                            }
                                        } else {
                                            transmitting.queue(f);
                                        }


                                        LOG.debug(debugTag + "(REJ) resending I frame " + f.sender + "->" + f.dest + " NS=" + f.getNS() + " NR=" + f.getNR() + " #=" + f.body.length);

                                    } else {
                                        break; // ran out of frames to resend
                                    }
                                    nextVA = (nextVA + 1) % (connType == ConnState.ConnType.MOD128 ? 128 : 8);
                                } while (nextVA != state.modSentFrameIndex);
                                state.modSentFrameIndex = nextVA;
                            }
                        }
                        break;

                        // SREJ frames are used to request retransmission of a single frame.
                        // If P/F is set in an SREJ frame, then frames up to N(R)-1 are considered acknowledged.
                        // If P/F is not set then N(R) does not indicate acknowledged I frames.
                        case AX25Frame.STYPE_SREJ:
                            if (frame.getP()) {
                                int newVA;
                                if (connType == ConnState.ConnType.MOD128) {
                                    newVA = (frame.getNR() - 1 + 128) % 128;
                                } else {
                                    newVA = (frame.getNR() - 1 + 8) % 8;
                                }
                                int nextVA = (state.modAcknowledgedFrameIndex + 1) % (connType == ConnState.ConnType.MOD128 ? 128 : 8);
                                while (nextVA != newVA) {
                                    if (state.transmitWindow != null) {
                                        state.transmitWindow[state.modAcknowledgedFrameIndex] = null;
                                    }
                                    state.modAcknowledgedFrameIndex = nextVA;
                                    nextVA = (state.modAcknowledgedFrameIndex + 1) % (connType == ConnState.ConnType.MOD128 ? 128 : 8);
                                }
                            }
                            if (state.transmitWindow != null) {
                                AX25Frame f;
                                if ((f = state.transmitWindow[frame.getNR()]) != null) {
                                    f.setNR(state.modReceivedFrameIndex);
                                    f.setNS(frame.getNR());
                                    if (state.connector != null) {
                                        try {
                                            state.connector.sendFrame(f);
                                        } catch (IOException e) {
                                            LOG.error(e.getMessage(), e);
                                        }
                                    } else {
                                        transmitting.queue(f);
                                    }
                                    LOG.debug(debugTag + "(SREJ) resending I frame " + f.sender + "->" + f.dest + " NS=" + f.getNS() + " NR=" + f.getNR() + " #=" + f.body.length);
                                }
                            }
                            break;
                        default:
                            // can't ever get here
                            break;
                    }
                    state.updateSessionTime();
                    fireConnStateUpdated(state);
                } else {
                    if (toMe) {
                        transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), true);
                    }
                }
            } else {
                if (toMe && frame.isCmd) {
                    transmitDM(connector, frame.dest, frame.sender, reverseDigipeaters(frame.digipeaters), true);
                }
            }
        } else {
//            throw new IllegalArgumentException("impossible frametype=" + frameType);
        }

        if (!msgReported) {
            fireConsumeAX25Frame(frame, connector);
        }
    }

    /**
     * Get the map of all outstanding I-frame connected sessions observed by or end-pointed at this
     * station.
     *
     * @return Map by initiating callsign of Maps by destination callsign of connection states
     */
    public Map<AX25Callsign, Map<AX25Callsign, ConnState>> getConnectionMap() {
        return Collections.unmodifiableMap(connMap);
    }

    /**
     * Get the connection state of an I-frame connection session between the specified two
     * callsigns.
     *
     * @param src AX25Callsign of originating end of session
     * @param dst AX25Callsign of receiving end of session
     * @return ConnType of the session, or ConnType.NONE if there is no such connection session
     * known to this protocol stack
     */
    public ConnState.ConnType getStateOf(AX25Callsign src, AX25Callsign dst) {
        ConnState.ConnType answer = ConnState.ConnType.NONE; // assume no connection until we find one
        ConnState state;
        if ((state = getConnState(src, dst, false)) == null) {
            state = getConnState(dst, src, false);
        }
        if (state != null) {
            answer = state.getConnType();
        }
        return answer;
    }

    /**
     * Get the connection state of an I-frame connection session between the specified two
     * callsigns, optionally creating the state object if it did not previously exist.
     *
     * @param src                AX25Callsign of originating end of session
     * @param dst                AX25Callsign of receiving end of session
     * @param createIfNotPresent boolean true if a new ConnState object should be created and
     *                           registered if it does not already exist
     * @return ConnState object for the requested session, or null if no such session exists
     * and the caller did not request creating one
     */
    public synchronized ConnState getConnState(AX25Callsign src, AX25Callsign dst, boolean createIfNotPresent) {
        Map<AX25Callsign, ConnState> connsOnSrcMap = connMap.get(src);
        ConnState state = null;
        if (connsOnSrcMap != null) {
            if ((state = connsOnSrcMap.get(dst)) == null && createIfNotPresent) {
                connsOnSrcMap.put(dst, state = new ConnState(src, dst, this));
            }
        } else if (createIfNotPresent) {
            connMap.put(src, connsOnSrcMap = new LinkedHashMap<>());
            connsOnSrcMap.put(dst, state = new ConnState(src, dst, this));
        }
        return state;
    }

    /**
     * Remove the specified ConnState object from the map of ConnState maps.
     * Usually done when a connection is closed.
     *
     * @param connState ConnState object to remove from map
     */
    public synchronized void removeConnState(ConnState connState) {
        Map<AX25Callsign, ConnState> connsOnSrcMap = connMap.get(connState.src);
        if (connsOnSrcMap != null) {
            if (connsOnSrcMap.remove(connState.dst) != null &&
                    connsOnSrcMap.size() == 0) {
                connMap.remove(connState.src);
            }
        }
    }

    /**
     * Transmit a UA frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for UA frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param finish      boolean true if final bit should be set
     */
    private void transmitUA(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, boolean finish) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(false);
        resp.digipeaters = digipeaters;
        resp.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_UA | (finish ? AX25Frame.MASK_U_P : 0));
        resp.body = new byte[0];
        try {
            ((TransmittingConnector) connector).sendFrame(resp);
            LOG.debug(debugTag + "sending U UA" + (finish ? " F" : "") + " to " + resp.dest);
        } catch (IOException e) {
            LOG.error("unable to send UA frame to " + remote, e);
        }
    }

    /**
     * Transmit a DISC (session disconnect) frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for DISC frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param poll        boolean true if poll bit should be set
     * @return the transmitted AX25Frame
     */
    AX25Frame transmitDISC(TransmittingConnector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, boolean poll) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(true);
        resp.digipeaters = digipeaters;
        resp.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_DISC | (poll ? AX25Frame.MASK_U_P : 0));
        resp.body = new byte[0];
        if (connector != null) {
            try {
                connector.sendFrame(resp);
                LOG.debug(debugTag + "sending U DISC" + (poll ? " P" : "") + " to " + resp.dest);
            } catch (IOException e) {
                LOG.error("unable to send DISC frame to " + remote, e);
            }
        } else {
            transmitting.queue(resp);
            LOG.debug(debugTag + "queuing U DISC to " + resp.dest);
        }
        return resp;
    }

    /**
     * Transmit a DM frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for DM frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param finish      boolean true if final bit should be set
     */
    private void transmitDM(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, boolean finish) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(false);
        resp.digipeaters = digipeaters;
        resp.ctl = (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_DM | (finish ? AX25Frame.MASK_U_P : 0));
        resp.body = new byte[0];
        try {
            ((TransmittingConnector) connector).sendFrame(resp);
            LOG.debug(debugTag + "sending U DM" + (finish ? " F" : "") + " to " + resp.dest);
        } catch (IOException e) {
            LOG.error("unable to send DM frame to " + remote, e);

        }
    }

    /**
     * Transmit a RR supervisory frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for RR frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param state       ConnState of connection for which this is being sent
     * @param poll        boolean true if poll bit should be set
     */
    void transmitRR(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, ConnState state, boolean poll) {
        transmitRR(connector, local, remote, digipeaters, state, poll, false);
    }

    /**
     * Transmit a RR supervisory frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for RR frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param state       ConnState of connection for which this is being sent
     * @param poll        boolean true if poll bit should be set
     * @param cmd         boolean true if command bits should be set, false if response bits
     */
    void transmitRR(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, ConnState state, boolean poll, boolean cmd) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(cmd);
        resp.digipeaters = digipeaters;
        resp.mod128 = state.getConnType() == ConnState.ConnType.MOD128;
        if (!resp.mod128) {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_RR | (poll ? AX25Frame.MASK_U_P : 0) | ((state.modReceivedFrameIndex & 0x07) << 5));
        } else {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_RR);
            resp.ctl2 = (byte) (((state.modReceivedFrameIndex & 0x7F) << 1) | (poll ? AX25Frame.MASK_U_P128 : 0));
        }
        resp.body = new byte[0];
        try {
            ((TransmittingConnector) connector).sendFrame(resp);
            LOG.debug(debugTag + "sending S RR" + (poll ? " P" : "") + " NR=" + state.modReceivedFrameIndex + " to " + resp.dest);
        } catch (Exception e) {
            LOG.error("unable to send RR frame to " + remote, e);
        }
    }

    /**
     * Transmit a RNR supervisory frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for RNR frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param state       ConnState of connection for which this is being sent
     * @param poll        boolean true if poll bit should be set
     */
    void transmitRNR(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, ConnState state, boolean poll) {
        transmitRNR(connector, local, remote, digipeaters, state, poll, false);
    }

    /**
     * Transmit a RNR supervisory frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for RNR frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param state       ConnState of connection for which this is being sent
     * @param poll        boolean true if poll bit should be set
     * @param cmd         boolean true if command bits should be set, false if response bits
     */
    void transmitRNR(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, ConnState state, boolean poll, boolean cmd) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(cmd);
        resp.digipeaters = digipeaters;
        resp.mod128 = state.getConnType() == ConnState.ConnType.MOD128;
        if (!resp.mod128) {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_RNR | (poll ? AX25Frame.MASK_U_P : 0) | ((state.modReceivedFrameIndex & 0x07) << 5));
        } else {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_RNR);
            resp.ctl2 = (byte) (((state.modReceivedFrameIndex & 0x7F) << 1) | (poll ? AX25Frame.MASK_U_P128 : 0));
        }
        resp.body = new byte[0];
        try {
            ((TransmittingConnector) connector).sendFrame(resp);
            LOG.debug(debugTag + "sending S RNR" + (poll ? " P" : "") + " NR=" + state.modReceivedFrameIndex + " to " + resp.dest);
        } catch (Exception e) {
            LOG.error("unable to send RR frame to " + remote, e);
        }
    }

    /**
     * Transmit a REJ supervisory frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for REJ frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param poll        boolean true if poll bit should be set
     */
    private void transmitREJ(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, ConnState state, boolean poll) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(false);
        resp.digipeaters = digipeaters;
        resp.mod128 = state.getConnType() == ConnState.ConnType.MOD128;
        if (!resp.mod128) {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_REJ | (poll ? AX25Frame.MASK_U_P : 0) | ((state.modReceivedFrameIndex & 0x07) << 5));
        } else {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_REJ);
            resp.ctl2 = (byte) (((state.modReceivedFrameIndex & 0x7F) << 1) | (poll ? AX25Frame.MASK_U_P128 : 0));
        }
        resp.body = new byte[0];
        try {
            ((TransmittingConnector) connector).sendFrame(resp);
            LOG.debug(debugTag + "sending S REJ" + (poll ? " P" : "") + " NR=" + state.modReceivedFrameIndex + " to " + resp.dest);
        } catch (IOException e) {
            LOG.error("unable to send REJ frame to " + remote, e);
        }
    }

    /**
     * Transmit a SREJ supervisory frame to the requested remote station.
     *
     * @param connector   Connector through which the message should be sent
     * @param local       originating AX25Callsign for SREJ frame
     * @param remote      recipient AX25Callsign
     * @param digipeaters digipeater path (if any) that went from recipient to sender
     * @param poll        boolean true if poll bit should be set
     */
    private void transmitSREJ(Connector connector, AX25Callsign local, AX25Callsign remote, AX25Callsign[] digipeaters, ConnState state, boolean poll) {
        AX25Frame resp = new AX25Frame();
        resp.sender = local.dup();
        resp.dest = remote.dup();
        resp.setCmd(false);
        resp.digipeaters = digipeaters;
        resp.mod128 = state.getConnType() == ConnState.ConnType.MOD128;
        if (!resp.mod128) {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_SREJ | (poll ? AX25Frame.MASK_U_P : 0) | ((state.modReceivedFrameIndex & 0x07) << 5));
        } else {
            resp.ctl = (byte) (AX25Frame.FRAMETYPE_S | AX25Frame.STYPE_SREJ);
            resp.ctl2 = (byte) (((state.modReceivedFrameIndex & 0x7F) << 1) | (poll ? AX25Frame.MASK_U_P128 : 0));
        }
        resp.body = new byte[0];
        try {
            ((TransmittingConnector) connector).sendFrame(resp);
            LOG.debug(debugTag + "sending S SREJ" + (poll ? " P" : "") + " NR=" + state.modReceivedFrameIndex + " to " + resp.dest);
        } catch (IOException e) {
            LOG.error("unable to send SREJ frame to " + remote, e);
        }
    }

    /**
     * Reverse the order of a sequence of digipeaters (explicit and APRS aliases).
     *
     * @param srcRelays array of AX25Callsign indicating the path used to get from sender to recipient
     * @return array of AX25Callsigns needed to go from recipient back to sender
     */
    public AX25Callsign[] reverseDigipeaters(AX25Callsign[] srcRelays) {
        if (srcRelays == null || srcRelays.length == 0) {
            return null;
        }
        int rptCount = 0;
        //TODO: need to make this account for partially used New-N aliases
        for (int i = 0; i < srcRelays.length; i++) {
            if (srcRelays[i].hasBeenRepeated()) {
                rptCount++;
            } else {
                // send path didn't need to use this repeater, so stop here without it unless it's a partially used New-N
                if (srcRelays[i].isNewNParadigmAlias()) {
                    String srcBase = srcRelays[i].getBaseCallsign();
                    int maxHops = srcBase.charAt(srcBase.length() - 1) - '0';
                    int ssid = srcRelays[i].getSSID();
                    if (maxHops > ssid) {
                        if (i > 0 && !srcRelays[i - 1].isNewNParadigmAlias()) {
                            ssid++; // traced real digi (legit callsign or a tactical name), so don't count that used SSID
                        }
                        if (maxHops > ssid) {
                            rptCount++; // count this one as well (it was partially used)
                        }
                    }
                }
                break;
            }
        }
        if (rptCount == 0) {
            return null; // still have a direct path
        }
        AX25Callsign[] returnPath = new AX25Callsign[rptCount];
        for (int i = 0; i < rptCount; i++) {
            AX25Callsign r = returnPath[rptCount - i - 1] = srcRelays[i].dup();
            if (r.h_c) {
                r.h_c = false;
            }
            if (r.isNewNParadigmAlias()) {
                String srcBase = r.getBaseCallsign();
                int maxHops = srcBase.charAt(srcBase.length() - 1) - '0';
                r.setSSID(maxHops - r.getSSID());
            }
        }
        LOG.debug(debugTag + "reversing " + Arrays.toString(srcRelays) + "->" + Arrays.toString(returnPath));
        return returnPath;
    }

    /**
     * This method consumes one information frame. The caller handles any flow
     * control issues for I (versus UI) frames.
     *
     * @param frame        AX25Frame to be decoded
     * @param isUI         where frame was received in a UI AX.25 frame instead of an I frame
     * @param connector    PortConnector over which message arrived, or null for file playback
     * @param rcvTimestamp time (in Java-standard milliseconds since Jan 1 1970 UTC) that message was received
     * @return boolean true if message has already been reported to listeners
     */
    public boolean processIBody(AX25Frame frame, boolean isUI, Connector connector, long rcvTimestamp) {
        boolean msgReported = false;
        final AX25Callsign sender = frame.sender;
        final AX25Callsign dest = frame.dest;
        byte pid = frame.getPid();
        byte[] body = frame.body;

        if (pid == AX25Frame.PID_NOLVL3) {
            // determine if it is APRS or something else
            if (isUI) {
                if (body.length >= 2 && body.length <= 256 && aprsParser != null) {
                    try {
                        // should be a valid APRS packet, so parse it
                        AX25Message aprsMsg = aprsParser.parse(body, sender, dest, frame.digipeaters, rcvTimestamp, connector);
                        frame.parsedAX25Msg = aprsMsg;
                        if (!frame.isValid()) {
                            aprsMsg.setInvalid(true);
                        }
                        if (connector != null) {
                            aprsParser.processParsedAX25Packet(frame, aprsMsg);
                        } else {
                            aprsMsg.setAx25Frame(frame);
                            aprsMsg.extractSource();
                        }
                        msgReported = true;
                    } catch (Exception e) {
                        LOG.error(connector + " invalid APRS frame: " + new String(body, StandardCharsets.UTF_8), e);
                        if (connector != null) {
                            connector.getStats().numBadRcvFrames++;
                        }
                    }
                }
            } else if (getConnState(sender, dest, false) != null) {
                // note that I frames are handled further up the stack once confirmed they are addressed to this station
                msgReported = true; // don't process any further here
            }
        }
        if (!msgReported) {
            // fall into default handler
            int maskedPid;
            if ((maskedPid = pid & 0x30) == 0x10 || maskedPid == 0x20) {
                //TODO: handle AX.25 Level 3
            } else if (isUI) {
                // look up the protocol handler for this PID
                AX25Parser parser;
                if ((parser = protocolParserMap.get(pid)) != null) {
                    try {
                        AX25Message parsedMsg;
                        if ((parsedMsg = parser.parse(body, sender, dest, frame.digipeaters, rcvTimestamp, connector)) != null) {
                            frame.parsedAX25Msg = parsedMsg;
                            if (!frame.isValid()) {
                                parsedMsg.setInvalid(true);
                            }
                            if (pid == AX25Frame.PID_NOLVL3) {
                                parsedMsg.setInvalid(true); // we should only get here for an over-max-length APRS frame
                            }
                            if (connector != null) {
                                if (pid == AX25Frame.PID_NOLVL3 && parser instanceof AX25ParserWithDistributor) {
                                    ((AX25ParserWithDistributor) parser).processParsedAX25Packet(frame, parsedMsg);
                                }
                                processParsedAX25Message(frame, parsedMsg);
                            } else {
                                parsedMsg.setAx25Frame(frame);
                                parsedMsg.extractSource();
                            }
                            msgReported = true;
                        }
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                } else {
                    // log unhandleable/unrecognized protocol
                    LOG.debug(connector + ": ignored " + body.length + "-byte protocol PID=0x" + Integer.toHexString(pid & 0xFF) + " msg from " + sender + "->" + dest);
                }
            }
        }
        return msgReported;
    }

    /**
     * Dispatch an AX.25 frame that was parsed into a higher-level protocol to all
     * registered listeners.
     *
     * @param frame     AX25Frame of incoming message
     * @param parsedMsg AX25Message subclass containing protocol decoding of message
     */
    public void processParsedAX25Message(AX25Frame frame, AX25Message parsedMsg) {
        parsedMsg.setAx25Frame(frame);
        ArrayList<ParsedAX25MessageListener> parsedAX25MessageListenerList = this.parsedAX25MessageListenerList;
        for (int i = 0; i < parsedAX25MessageListenerList.size(); i++) {
            parsedAX25MessageListenerList.get(i).parsedAX25MessageReceived(frame.getPid(), parsedMsg);
        }
    }

    /**
     * Register an ParsedAX25MessageListener to be notified of incoming parsed level 3 protocol
     * messages (other than APRS).
     *
     * @param listener ParsedAX25MessageListener to register
     */
    public void addParsedAX25MessageListener(ParsedAX25MessageListener listener) {
        if (!parsedAX25MessageListenerList.contains(listener)) {
            parsedAX25MessageListenerList.add(listener);
        }
    }

    /**
     * Unregister an ParsedAX25MessageListener to be notified of incoming parsed level 3 protocol
     * messages (other than APRS).
     *
     * @param listener ParsedAX25MessageListener to unregister
     */
    public void removeParsedAX25MessageListener(ParsedAX25MessageListener listener) {
        parsedAX25MessageListenerList.remove(listener);
    }

    /**
     * Test if this callsign is addressed to the local station.
     *
     * @param dest AX25Callsign to test as a destination
     * @return boolean true if this callsign is for the local station
     */
    public boolean isLocalDest(AX25Callsign dest) {
        return dest != null && transmitting.isLocalDest(dest.toString());
    }

    /**
     * Test if this callsign is addressed to the local station.
     *
     * @param destCallsign String of AX.25 callsign-SSID to test as a destination
     * @return boolean true if this callsign is for the local station
     */
    public boolean isLocalDest(String destCallsign) {
        return transmitting.isLocalDest(destCallsign);
    }

    /**
     * Register a listener to be informed when AX.25 connected-mode session state
     * changes.
     *
     * @param l ConnStateChangeListener to register
     */
    public void addConnStateChangeListener(ConnStateChangeListener l) {
        if (l != null && !connStateListeners.contains(l)) {
            connStateListeners.add(l);
        }
    }

    /**
     * Unregister a listener to be informed when AX.25 connected-mode session state
     * changes.
     *
     * @param l ConnStateChangeListener to unregister
     */
    public void removeConnStateChangeListener(ConnStateChangeListener l) {
        if (l != null) {
            connStateListeners.remove(l);
        }
    }

    void fireConnStateUpdated(ConnState connState) {
        //ArrayList<ConnStateChangeListener> connStateListeners = AX25Stack.connStateListeners;
        for (int i = connStateListeners.size() - 1; i >= 0; i--) {
            try {
                connStateListeners.get(i).updateConnStateRow(connState.src, connState.dst);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    void fireConnStateAddedOrRemoved() {
        // ArrayList<ConnStateChangeListener> connStateListeners = connStateListeners;
        for (int i = connStateListeners.size() - 1; i >= 0; i--) {
            try {
                connStateListeners.get(i).updateWholeConStateTable();
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Set the debug tag - this could be anything useful, most notably frequency when using multiple stack instances.
     *
     * @param tag String to use as debug tag
     */
    public void setDebugTag(String tag) {
        debugTag = tag;
    }
}
