package com.example.testvideostream.exception;

public class NotFoundException extends TestVideoStreamException {
    public NotFoundException() {
        super("Not Found");
    }

    public NotFoundException(String message) {
        super(message);
    }
}

