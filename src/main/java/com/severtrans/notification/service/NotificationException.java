package com.severtrans.notification.service;

/**
 * https://www.baeldung.com/java-new-custom-exception
 */
public class NotificationException extends Exception {
    public NotificationException(String message) {
        super(message);
    }
}
