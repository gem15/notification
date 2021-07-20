package com.severtrans.notification;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.severtrans.notification.service.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class NotificationApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

//    @Autowired
//    SendNotifications notifications;
//    @Autowired
//    Scheduler scheduler;

    @Override
    public void run(String... args) throws Exception {
        log.info(">>> Start notifications");
    //    SendNotifications notifications = new SendNotifications();
    //    notifications.reply();
//        scheduler.fixedDelaySch();
    }

    @Bean
    public FTPClient ftp() {
        return new FTPClient();
    }

    @Bean
    public XmlMapper xmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        xmlMapper.setTimeZone(TimeZone.getDefault());
        return xmlMapper;
    }

}
