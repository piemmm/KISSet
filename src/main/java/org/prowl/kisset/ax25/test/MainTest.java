package org.prowl.kisset.ax25.test;

import org.prowl.kissterm.ax25.*;
import org.prowl.kissterm.ax25.io.BasicTransmittingConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * A Basic start point for using the library, as a very crude example.
 */
public class MainTest {

    private BasicTransmittingConnector connector;
    //AX25InputStream

    public static void main(String[] args) {

        System.out.println("Starting");
        MainTest mainTest = new MainTest();
        mainTest.start();


    }


    public void start() {
        try {
            // Connect to kiiss port on direwolf

            System.out.println("Connecting to kiss port");
            Socket s = new Socket(InetAddress.getByName("127.0.0.1"), 8001);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            System.out.println("Connected to kiss port");

            // Our default callsign. acceptInbound can determine if we actually want to accept any callsign requests,
            // not just this one.
            AX25Callsign defaultCallsign = new AX25Callsign("N0CALL-5");

            connector = new BasicTransmittingConnector(255, 7, 1200, 6,defaultCallsign, in, out, new ConnectionRequestListener() {

                /**
                 * Determine if we want to respond to this connection request (to *ANY* callsign) - usually we only accept
                 * if we are interested in the callsign being sent a connection request.
                 *
                 * @param state ConnState object describing the session being built
                 * @param originator AX25Callsign of the originating station
                 * @param port Connector through which the request was received
                 * @return
                 */
                @Override
                public boolean acceptInbound(ConnState state, AX25Callsign originator, Connector port) {

                    System.out.println("Incoming connection from: " + originator.toString());

                    // If we're going to accept then add a listener so we can keep track of the connection
                    state.listener = new ConnectionEstablishmentListener() {
                        @Override
                        public void connectionEstablished(Object sessionIdentifier, ConnState conn) {

                            Thread tx = new Thread(() -> {

                                // Do inputty and outputty stream stuff here
                                try {

                                    // Get the input stream and handle incoming data in its own thread.
                                    InputStream in = state.getInputStream();
                                    Thread t = new Thread(() -> {
                                        while (state.isOpen()) {
                                            try {
                                                System.out.println("IN:" + in.read());
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                        System.out.println("RX finished");
                                    });
                                    t.start();

                                    // Get the output stream and send something to the client (dont forget to call flush!)
                                    // will auto-'flush' when paclen is reached (or max frame size is reached)
                                    OutputStream out = state.getOutputStream();
                                    out.write("You have connected1!\r".getBytes());
                                    out.flush();
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                    }

                                    // This is how we disconnect the remote node!
                                    state.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            });

                            tx.start();


                        }

                        @Override
                        public void connectionNotEstablished(Object sessionIdentifier, Object reason) {

                        }

                        @Override
                        public void connectionClosed(Object sessionIdentifier, boolean fromOtherEnd) {

                        }

                        @Override
                        public void connectionLost(Object sessionIdentifier, Object reason) {

                        }
                    };
                    return true;
                }


            });


        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }


    }


}
