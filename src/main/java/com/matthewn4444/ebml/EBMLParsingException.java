package com.matthewn4444.ebml;

import java.io.IOException;

public class EBMLParsingException extends IOException {
    private static final long serialVersionUID = -1315434993629045646L;
    public EBMLParsingException() {}
    public EBMLParsingException(String message) {
        super(message);
    }
}