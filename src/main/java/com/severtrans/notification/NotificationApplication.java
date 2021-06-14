package com.severtrans.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
//@EnableScheduling
@SpringBootApplication
public class NotificationApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

    @Autowired
    SendNotifications notifications;

    @Override
    public void run(String... args) throws Exception {
        log.info("Start...");
//        SendNotifications notifications = new SendNotifications();
        notifications.send();
    }
}
