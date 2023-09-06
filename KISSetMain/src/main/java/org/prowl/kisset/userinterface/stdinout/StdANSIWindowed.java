package org.prowl.kisset.userinterface.stdinout;


import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.bundle.LanternaThemes;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.prowl.kisset.util.Tools;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * This terminal uses lanterna to provide menus and a line buffer.
 */
public class StdANSIWindowed extends StdTerminal implements TerminalResizeListener {

    private static final Log LOG = LogFactory.getLog("StdANSIWindowed");

    private static boolean running = false;

    private TextBox inputPanel;
    private TextBox outputPanel;
    private Terminal terminal;
    private TerminalScreen screen;
    private MultiWindowTextGUI gui;
    private BasicWindow desktop;


    private TextBox output = new TextBox();


    public StdANSIWindowed(InputStream stdIn, OutputStream stdOut) {
        super(stdIn, stdOut);

        try {
            DefaultTerminalFactory factory = new DefaultTerminalFactory();
            factory.setTerminalEmulatorTitle("Kisset");

            terminal = new UnixTerminal(stdIn,stdOut, Charset.defaultCharset());//factory.createTerminal();
            terminal.enterPrivateMode();

            screen = new TerminalScreen(terminal);

            screen.startScreen();
            terminal.clearScreen();

            gui = new MultiWindowTextGUI(screen);

            gui.setTheme(LanternaThemes.getRegisteredTheme("blaster"));
            terminal.putCharacter('l');
            buildDesktop();

        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public StdANSIWindowed() {
        super();
    }

    public void buildDesktop() {

        Panel content = new Panel();
        content.setLayoutManager(new BorderLayout());

        inputPanel = new TextBox("", TextBox.Style.SINGLE_LINE);
        inputPanel.setPreferredSize(new TerminalSize(80, 1));
        inputPanel.setInputFilter(new InputFilter() {

            @Override
            public boolean onInput(Interactable interactable, KeyStroke keyStroke) {
                if (keyStroke.getKeyType() == KeyType.Enter) {
                    // Submit text
                    processInput(inputPanel.getText());
                    inputPanel.setText("");
                } else if (keyStroke.getKeyType() == KeyType.ArrowUp) {
                    // History
                    outputPanel.handleKeyStroke(keyStroke);
                    return false;
                } else if (keyStroke.getKeyType() == KeyType.ArrowDown) {
                    // History
                    outputPanel.handleKeyStroke(keyStroke);
                    return false;
                }
                return true;
            }
        });

        outputPanel = new TextBox("", TextBox.Style.MULTI_LINE);
        outputPanel.setEnabled(false);
        outputPanel.withBorder(Borders.singleLine());

        content.addComponent(outputPanel, BorderLayout.Location.CENTER);
        content.addComponent(inputPanel, BorderLayout.Location.BOTTOM);

        desktop = new BasicWindow();
        desktop.setComponent(content);
        desktop.setHints(Arrays.asList(Window.Hint.FULL_SCREEN));

        inputPanel.takeFocus();

        //showGreeting();

        gui.addWindow(desktop);

    }

    public void processInput(String input) {
        try {
            tncIn.getOutputStream().write(input.getBytes());
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void write(String s) {
        outputPanel.addLine(s);
        outputPanel.handleKeyStroke(new KeyStroke(KeyType.ArrowDown));
    }


    @Override
    public void onResized(Terminal terminal, TerminalSize terminalSize) {
        LOG.debug("Terminal resized to " + terminalSize);
    }

    // Default std terminal is just a passthrough.
    public void start() {
        running = true;
        // Take std input and pass to TNC host
        Tools.runOnThread(() -> {
            try {
                while (running) {
                    int b = stdIn.read();
                    if (b == -1)
                        break;
                    tncIn.getOutputStream().write(b);
                }
                running = false;
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
        });


        // Take output from TNC host and pass to stdout
        Tools.runOnThread(() -> {
            int lastNewlineByte = 0;
            try {
                while (running) {
                    if (tncOut.available() == 0) {
                        stdOut.flush();
                    }
                    int b = tncOut.read();
                    if (b == -1)
                        break;
                    if (b == 0x0a && lastNewlineByte != 0x0d) {
                        stdOut.write(System.lineSeparator().getBytes());
                        lastNewlineByte = b;
                    } else if (b == 0x0d && lastNewlineByte != 0x0a) {
                        stdOut.write(System.lineSeparator().getBytes());
                        lastNewlineByte = b;
                    } else if (b != 0x0d && b != 0x0a) {
                        stdOut.write(b);
                        lastNewlineByte = b;
                    }
                }
                running = false;
            } catch (Throwable e) {
                LOG.debug(e.getMessage(), e);
            }
        });


    }

    public void stop() {
        running = false;
    }
}
