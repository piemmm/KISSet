package org.prowl.util;

import org.junit.jupiter.api.Test;
import org.prowl.kisset.util.Tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ToolsTest {

    @Test
    public void testLocator() {
        assertEquals("JN58TD", Tools.toLocator(48.14666, 11.60833));
        assertEquals("GF15VC", Tools.toLocator(-34.91, -56.21166));
        assertEquals("FM18LW", Tools.toLocator(38.92, -77.065));
        assertEquals("RE78IR", Tools.toLocator(-41.28333, 174.745));
    }



}
