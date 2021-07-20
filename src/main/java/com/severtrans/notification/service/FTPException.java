package com.severtrans.notification.service;

/**
 * https://www.baeldung.com/java-new-custom-exception
 */
public class FTPException extends Exception {
    public FTPException(String message) {
        super(message);
    }
}
