package com.matthewn4444.ebml;

public class EBMLException extends RuntimeException {
    private static final long serialVersionUID = 4586054820636243018L;
    public EBMLException() {}
    public EBMLException(String message) {
        super(message);
    }
}
