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
import org.prowl.ax25.util.ReschedulableTimerTask;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

/**
 * This class keeps track of the state of one AX.25 connection-oriented session.
 *
 * @author Andrew Pavlin, KA2DDO
 */
public class ConnState implements AX25FrameSource, Closeable {

    private static final Log LOG = LogFactory.getLog("ConnState");
    private static final Object MONITOR = new Object();
    /**
     * Originator of session.
     */
    public final AX25Callsign src;
    /**
     * Recipient of session.
     */
    public final AX25Callsign dst;
    /**
     * The current state of this session.
     */
    public ConnTransition transition = ConnTransition.STEADY;
    /**
     * Digipeater path to use for transmitting from source to destination station. Only meaningful when
     * source is the local station.
     */
    public AX25Callsign[] via = null;
    /**
     * Listener to be asynchronously informed of state changes in the session.
     */
    public ConnectionEstablishmentListener listener = null;
    /**
     * Arbitrary identifier for a particular connected-mode session.
     */
    public Object sessionIdentifier = null;
    ConnType connType = ConnType.NONE;
    /**
     * Modulo index of last received frame. Only used for sessions to or from this station.
     * AX.25 spec section 4.2.2.4.
     */
    int modReceivedFrameIndex = 0;
    /**
     * Modulo index of last transmitted frame. Only used for sessions to or from this station.
     * AX.25 spec section 4.2.2.2,
     */
    int modSentFrameIndex = 0;
    /**
     * Modulo index of last acknowledged frame. Only used for sessions to or from this station.
     * AX.25 spec section 4.2.2.6.
     */
    int modAcknowledgedFrameIndex = 0;

    volatile boolean localRcvBlocked = false;
    volatile boolean xmtToRemoteBlocked = false;
    /**
     * Window of I-frames being sent from this station to the other end of the connection. Only used
     * for sessions to or from this station. {@link #modSentFrameIndex} and {@link #modAcknowledgedFrameIndex} are indexes into this array.
     */
    AX25Frame[] transmitWindow = null;
    AX25Stack stack;
    /**
     * PortConnector from which this connected-mode session was heard, or null if port not identified yet.
     */
    TransmittingConnector connector = null;
    transient ReschedulableTimerTask t1TimerTask = null;
    transient int retriesRemaining = 0;
    transient AX25Frame frameToResend = null;
    /**
     * Last time this connection was updated.
     */
    transient long lastUpdateInSession = System.currentTimeMillis();
    AX25InputStream in = null;
    AX25OutputStream out = null;

    ConnState(AX25Callsign src, AX25Callsign dst, AX25Stack stack) {
        this.src = src;
        this.dst = dst;
        this.stack = stack;
    }

    /**
     * Reset the windowing sequence counters and flags.
     */
    void reset() {
        synchronized (MONITOR) {
            modReceivedFrameIndex = 0;
            modSentFrameIndex = 0;
            modAcknowledgedFrameIndex = 0;
            localRcvBlocked = false;
            xmtToRemoteBlocked = false;
            if (transmitWindow != null) {
                for (int i = transmitWindow.length - 1; i >= 0; i--) {
                    transmitWindow[i] = null;
                }
            }
        }

    }

    /**
     * Get the current state of this connection.
     *
     * @return boolean true if connection is up and operating
     */
    public boolean isOpen() {
        return ConnType.NONE != connType && ConnType.CLOSED != connType && ConnTransition.STEADY == transition;
    }

    /**
     * Get the connection windowing type.
     *
     * @return ConnType enum describing the sliding window mode
     */
    public ConnType getConnType() {
        return connType;
    }

    /**
     * Set the connection windowing type.
     *
     * @param connType ConnType enum describing the sliding window mode
     */
    public void setConnType(ConnType connType) {
        this.connType = connType;
    }

    /**
     * Get a Java-style InputStream associated with this connection.
     *
     * @return InputStream for the connection
     * @throws IOException if session not to or from this station
     */
    public InputStream getInputStream() throws IOException {
        if (in == null) {
            if (!stack.isLocalDest(src) && !stack.isLocalDest(dst)) {
                throw new IOException("cannot open stream to session this station is not part of (" +
                        src + "->" + dst + ')');
            }
            in = new AX25InputStream(this);
        }
        return in;
    }

    /**
     * Get a Java-style OutputStream for writing to this connection.
     *
     * @return OutputStream to this connection
     * @throws IOException if session not to or from this station
     */
    public OutputStream getOutputStream() throws IOException {
        if (out == null) {
            if (!stack.isLocalDest(src) && !stack.isLocalDest(dst)) {
                throw new IOException("cannot open stream to session this station is not part of (" +
                        src + "->" + dst + ')');
            }
            out = new AX25OutputStream(this, stack.pacLen);
        }
        return out;
    }

    /**
     * Get the callsign of the station that originated this connection session.
     *
     * @return AX25Callsign of originating station
     */
    public AX25Callsign getSrc() {
        return src;
    }

    /**
     * Get the callsign of the station that received this connection session.
     *
     * @return AX25Callsign of receiving station
     */
    public AX25Callsign getDst() {
        return dst;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return "ConnState[" + paramString() + ']';
    }

    String paramString() {
        return src + "->" + dst + ',' + getStateOfConn();
    }

    /**
     * Provide textual description of connection state.
     *
     * @return String connection state text
     */
    public String getStateOfConn() {
        return String.valueOf(connType) + ',' + transition + ",vr=" + modReceivedFrameIndex + ",vs=" + modSentFrameIndex + ",va=" + modAcknowledgedFrameIndex
                + (localRcvBlocked ? ",rcvBlock" : "") + (xmtToRemoteBlocked ? ",xmtBlock" : "");
    }

    /**
     * Specify that this transmitted unconnected frame needs a timeout timer in case the appropriate
     * response does not come back.
     *
     * @param frame      AX25Frame that was transmitted and is expecting a response
     * @param retryCount int number of times this frame should be retried before giving up on a response
     *                   and failing whatever condition the frame was trying to set up
     */
    public void setResendableFrame(final AX25Frame frame, int retryCount) {
        synchronized (MONITOR) {
            clearResendableFrame();
            frameToResend = frame;
            retriesRemaining = retryCount;
            t1TimerTask = new ReschedulableTimerTask() {
                @Override
                public void run() {
                    // if the connection is no longer open, don't bother
                    if (transition == ConnTransition.LINK_DOWN) {
                        LOG.debug("T1 timeout on " + ConnState.this + " but connection is down, so it will be ignored");
                        cancel();  // also cancel the timer
                        return;
                    }

                    LOG.debug("T1 timeout on " + ConnState.this + " retriesRemaining=" + retriesRemaining + " for frame:" + frame + "   frameToResend:" + frameToResend);
                    if (retriesRemaining-- > 0) {

                        // SABM frame.
                        if (frame.ctl == (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_SABM) || frame.getPid() == (byte) (AX25Frame.FRAMETYPE_U | AX25Frame.UTYPE_SABME)) {
                            try {
                                connector.sendFrame(frame);
                            } catch (Exception e) {
                                LOG.error("unable to send SABM frame to " + frame.dest, e);
                            }
                            // This shouldn't be here it should just be RR/RNR to be sent
//                        } else if (frame.getFrameType() ==  AX25Frame.FRAMETYPE_I) {
//                                // I frames
//                                // Just resend the bloody thing
//                            try {
//                                connector.sendFrame(frame);
//                            } catch(IOException e){
//                                LOG.error("unable to send I frame to " + frame.dest, e);
//                            }
                        } else {
                            // RNR / RR frames
                            if (localRcvBlocked) {
                                stack.transmitRNR((Connector) connector, frameToResend.sender, frameToResend.dest, frameToResend.digipeaters, ConnState.this, true, true);
                            } else {
                                stack.transmitRR((Connector) connector, frameToResend.sender, frameToResend.dest, frameToResend.digipeaters, ConnState.this, true, true);
                            }
                        }
                    } else {
                        if (listener != null) {
                            if (transition == ConnTransition.LINK_UP) {
                                listener.connectionNotEstablished(sessionIdentifier, new TimeoutException("no response from " + dst));
                            } else {
                                listener.connectionLost(sessionIdentifier, new TimeoutException("no response from " + dst));
                            }
                        }
                        close();
                        cancel();
                        stack.removeConnState(ConnState.this);
                    }
                }
            };
            t1TimerTask.resched(stack.getRetransTimer(), stack.getWaitForAckT1Timer(), stack.getWaitForAckT1Timer());
        }
    }

    /**
     * Cancel an outstanding resendable frame's timer. This is due to either receiving a valid response
     * for the frame, or timing out for the last retry attempt and giving up.
     */
    public void clearResendableFrame() {
        synchronized (MONITOR) {
            if (t1TimerTask != null) {
                t1TimerTask.cancel();
                t1TimerTask = null;
            }
            frameToResend = null;
            retriesRemaining = 0;
        }
    }


    /**
     * Get one or more AX25Frames of the data to transmit. Note this should only
     * be used for AX.25 connections with one end terminated at the local station;
     * Man-In-The-Middle (MitM) attacks are not friendly behavior.
     *
     * @param incrementXmtCount indicate whether the transmit counter (used to cycle through
     *                          proportional pathing) should be incremented
     * @param protocolId        indicate the protocol to generate this frame for
     * @param senderCallsign    String of local callsign sending this message (may be ignored if digipeating
     *                          a message from another station)
     * @return array of AX25Frame objects to transmit (may be zero-length)
     */
    @Override
    public AX25Frame[] getFrames(boolean incrementXmtCount, ProtocolFamily protocolId, String senderCallsign) {
        synchronized (MONITOR) {

            if (protocolId == ProtocolFamily.RAW_AX25 && frameToResend != null && retriesRemaining > 0) {
                if (incrementXmtCount) {
                    retriesRemaining--;
                }
                AX25Frame[] answer = new AX25Frame[]{frameToResend};
                if (retriesRemaining <= 0) {
                    frameToResend = null;
                    retriesRemaining = 0;
                }
                return answer;
            }
            return NO_FRAMES;
        }
    }

    /**
     * Get number of times frame will be retransmitted before inter-packet delay is increased.
     *
     * @return transmission count before interval increase
     */
    @Override
    public int getNumTransmitsBeforeDecay() {
        return retriesRemaining;
    }

    /**
     * Report the Connector this message should be transmitted through.
     *
     * @return a specific Connector instance to transmit through, or null for all
     * applicable ports (Connector.CAP_XMT_PACKET_DATA and not rejecting
     * this specific packet [such as IGateConnectors shouldn't re-transmit
     * something received from the IGate])
     * @see Connector#CAP_XMT_PACKET_DATA
     */
    @Override
    public Connector getConnector() {
        return (Connector) connector;
    }

    /**
     * Set the Connector through which frames for this connected-mode session should be transmitted.
     *
     * @param connector Connector that is capable of transmitting AX.25 frames
     */
    public void setConnector(TransmittingConnector connector) {
        this.connector = connector;
    }

    /**
     * Closes this connection and releases any system resources associated
     * with it. If the connection is already closed then invoking this
     * method has no effect.
     */
    @Override
    public void close() {
        synchronized (MONITOR) {

            if (isOpen()) {
                AX25Frame closeFrame = null;
                if (in != null) {
                    try {
                        in.close();
                        in = null;
                    } catch (IOException e) {
                        e.printStackTrace(System.out);
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                        out = null;
                    } catch (IOException e) {
                        e.printStackTrace(System.out);
                    }
                }
                if (stack.isLocalDest(src)) {
                    LOG.debug("ConnState.close(): closing open connection we initiated");
                    closeFrame = stack.transmitDISC(connector, src, dst, via, false);
                    transition = ConnTransition.LINK_DOWN;
                } else if (stack.isLocalDest(dst)) {
                    closeFrame = stack.transmitDISC(connector, dst, src, stack.reverseDigipeaters(via), false);
                    transition = ConnTransition.LINK_DOWN;
                }
                if (closeFrame != null) {
                    setResendableFrame(closeFrame, 10);
                } else {
                    stack.removeConnState(this);
                    stack.fireConnStateAddedOrRemoved();
                }
                if (listener != null) {
                    listener.connectionClosed(sessionIdentifier, false);
                }
            } else {
                LOG.debug("ConnState.close(): cancelling incompletely opened connection");
                transition = ConnTransition.STEADY;
                connType = ConnType.CLOSED;
                stack.removeConnState(this);
                stack.fireConnStateAddedOrRemoved();
            }
        }
    }

    /**
     * Report the last time this connected-mode session was updated.
     *
     * @return time (in milliseconds since epoch) of the last update to this session
     */
    public long getLastUpdateInSession() {
        return lastUpdateInSession;
    }

    /**
     * Update the last-updated timestamp of this session.
     */
    public void updateSessionTime() {
        lastUpdateInSession = System.currentTimeMillis();
    }

    /**
     * Enum identifying the transitional condition of the connection.
     *
     * @author Andrew Pavlin, KA2DDO
     */
    public enum ConnTransition {
        /**
         * No changes in the state of this connection.
         */
        STEADY,
        /**
         * Connection in the process of coming up,
         */
        LINK_UP,
        /**
         * Connection in the process of going down.
         */
        LINK_DOWN
    }

    /**
     * Enumeration specifying the different types of connection-oriented AX.25 sessions.
     *
     * @author Andrew Pavlin, KA2DDO
     */
    public enum ConnType {
        /**
         * Not currently connection-oriented (still in initial negotiation).
         */
        NONE,
        /**
         * Using an eight-frame sliding window (all that AX.25 V2.0 can handle).
         */
        MOD8,
        /**
         * Using a 128-frame sliding window (if both ends speak AX.25 v2.2).
         */
        MOD128,
        /**
         * Either negotiation failed or the connection has been closed. Used to tell {@link AX25InputStream}
         * to report end-of-file.
         */
        CLOSED
    }
}
