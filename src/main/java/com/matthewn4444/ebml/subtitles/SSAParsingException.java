package com.matthewn4444.ebml.subtitles;

public class SSAParsingException extends RuntimeException {
    private static final long serialVersionUID = 3268759252977020091L;
    public SSAParsingException() {}
    public SSAParsingException(String message) {
        super(message);
    }
}