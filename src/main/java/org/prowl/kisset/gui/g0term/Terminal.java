package org.prowl.kisset.gui.g0term;

import javafx.scene.text.Font;

public interface Terminal {


    void append(int i);

    void clearSelection();

    void copySelectedTextToClipboard();

    boolean hasSelectedArea();

    void setFont(Font f);
}
