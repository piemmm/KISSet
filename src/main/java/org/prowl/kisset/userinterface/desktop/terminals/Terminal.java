package org.prowl.kisset.userinterface.desktop.terminals;

import javafx.scene.Node;
import javafx.scene.text.Font;

public interface Terminal {

    void append(int i);

    void clearSelection();

    void copySelectedTextToClipboard();

    boolean hasSelectedArea();

    void setFont(Font f);

    Node getNode();

    String getName();

}
