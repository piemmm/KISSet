package org.prowl.kisset.objects;

public class InvalidMessageException extends Exception {

    public InvalidMessageException(String message, Throwable e) {
        super(message, e);
    }

}
