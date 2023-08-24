package org.prowl.kisset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ResourceBundle;

public final class Messages {
    private static final Log LOG = LogFactory.getLog("Messages");


    private static ResourceBundle bundle;

    public static void init() {
        bundle = ResourceBundle.getBundle("messages");
    }

    public static String get(String key) {
        String s;
        try {
            s = bundle.getString(key);
            if (s == null || s.length() == 0) {
                return "Missing: " + key;
            }
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            s = e.getMessage();
        }
        return s;
    }

}
