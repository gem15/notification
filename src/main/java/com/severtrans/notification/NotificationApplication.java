package com.severtrans.notification;

import com.severtrans.notification.service.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class NotificationApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

    @Autowired
    SendNotifications notifications;
    @Autowired
    Scheduler scheduler;

    @Override
    public void run(String... args) throws Exception {
        log.info("Start...");
//        SendNotifications notifications = new SendNotifications();
//        notifications.send();
//        scheduler.fixedDelaySch();
    }
}
