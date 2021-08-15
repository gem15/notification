package com.severtrans.notification;

import java.io.IOException;
import java.util.TimeZone;

import javax.mail.MessagingException;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

import org.apache.commons.net.ftp.FTPClient;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class NotificationApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }

/*
    @Autowired
    Email email;
*/
    @Override
    public void run(String... args) throws Exception {
        
/*         try {
            
            email.sendEmail();
            email.sendEmailWithAttachment();
            
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
 */  
      log.info(">>> Start MONITOR");
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

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        return modelMapper;
    }

}
