package org.prowl.kisset.gui.ui;

import org.prowl.kissterm.gui.terminal.ConfigurationRepositoryFS;
import org.prowl.kissterm.gui.terminal.Connection;
import org.prowl.kissterm.gui.terminal.JCTermSwing;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private JCTermSwing term;

    public MainWindow() {
        super("KISSTerm - none");

        buildUI();
    }

    public void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JCTermSwing.setCR(new ConfigurationRepositoryFS());
        term = new JCTermSwing();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(BorderLayout.CENTER, term);
        pack();
        setVisible(true);
//
//        // Connect the terminal to our ax25 TNC emulator.
//        term.start(new Connection() {
//            @Override
//            public InputStream getInputStream() {
//                return null;
//            }
//
//            @Override
//            public OutputStream getOutputStream() {
//                return null;
//            }
//
//            @Override
//            public void requestResize(Term term) {
//
//            }
//
//            @Override
//            public void close() {
//
//            }
//        });


    }

    public void setConnection(Connection connection) {
        term.start(connection);
    }
}
