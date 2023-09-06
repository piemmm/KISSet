package org.prowl.kisset.userinterface.stdinout;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.PipedIOStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Pipe;

/**
 * This interface so we can support different types of text based terminals.
 * <p>
 * It will be assumed that the hosting terminal will be at least ANSI compliant as that seems to be the standard
 * for a lot of terminals nowdays on Linux, Mac and Windows.
 */
public abstract class StdTerminal {

    private static Log LOG = LogFactory.getLog("StdTerminal");

    // STDIN from the console
    protected InputStream stdIn;

    // STDOUT to the console
    protected OutputStream stdOut;

    // The TNC host inpurt stream
    protected PipedIOStream tncIn;

    // The TNC host output stream
    protected PipedIOStream tncOut;


    public StdTerminal(InputStream stdIn, OutputStream stdOut) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;

        this.tncIn = new PipedIOStream();
        this.tncOut = new PipedIOStream();

    }

    public StdTerminal() {

        this.tncIn = new PipedIOStream();
        this.tncOut = new PipedIOStream();

    }

    public void setIOStreams(InputStream stdIn, OutputStream stdOut) {
        this.stdIn = stdIn;
        this.stdOut = stdOut;

    }


    /**
     * Start the terminal this should deal with filtering any data
     */
    public abstract void start();

    /**
     * Stop the terminal
     */
    public abstract void stop();

    /**
     * The input stream that talks to the TNC Host
     *
     * @return
     */
    public InputStream getInputStream() {
        return tncIn;
    }


    /**
     * The output stream that talks to the TNC Host
     *
     * @return
     */
    public OutputStream getOutputStream() {
        return tncOut.getOutputStream();
    }


}
