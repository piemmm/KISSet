package org.prowl.kisset.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Interface {


    public abstract void start() throws IOException;

    public abstract void stop();

    public abstract String getName();


}
