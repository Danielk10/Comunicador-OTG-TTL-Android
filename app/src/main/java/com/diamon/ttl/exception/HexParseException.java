package com.diamon.ttl.exception;

public class HexParseException extends Exception {
    public HexParseException(String message) {
        super(message);
    }

    public HexParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
