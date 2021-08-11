package com.severtrans.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MailSenderTest {

    @Autowired
    MyMailSender myMailSender;

    @Test
    void send() {
        myMailSender.send("subs_gem@mail.ru","TEST","Test message");
    }
}