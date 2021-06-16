package com.severtrans.notification.service;

import com.severtrans.notification.SendNotifications;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class Scheduler {
    @Autowired
    SendNotifications notifications;

    private FTPClient ftp = new FTPClient();
    //    @Scheduled(fixedDelay = 10000, initialDelay = 3000)
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void fixedDelaySch() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
//        System.out.println("Fixed Delay scheduler:: " + strDate);
        log.info("log " + strDate);

/*
        Set file type to be transferred to binary.
        Create an InputStream for the local file.
        Construct path of the remote file on the server. The path can be absolute or relative to the current working directory.
        Call one of the storeXXX()methods to begin file transfer.
        Close the opened InputStream and OutputStream.
        Call completePendingCommand() method to complete transaction.
        Logout and disconnect from the server.
*/
        log.info("FTP");
        try {
            ftp.connect("localhost", 21);
            ftp.enterLocalPassiveMode();
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
//            throw new NotificationException("Exception in connecting to FTP Server");
            }

            if (!ftp.login("anonymous", "")) {
                // if (!ftp.login(user, password)) {
                ftp.logout();
            }

            putFile(NotificationType.E4102);
            putFile(NotificationType.E4104);
            putFile(NotificationType.E4111);

//TODO when disconnect ?

            ftp.logout();
            ftp.disconnect();

        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.logout();
                    ftp.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
//TODO create file name
    private void putFile(NotificationType type) throws IOException {
        InputStream inputStream = notifications.send(type);
        boolean done = ftp.storeFile("test.xml", inputStream);
        inputStream.close();

/* ??
        boolean completed = ftp.completePendingCommand();
        if (completed) {
            System.out.println("The second file is uploaded successfully.");
        }
*/


        int reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
//            throw new NotificationException("FTP error")
        }
        if (done) {
            System.out.println("The first file is uploaded successfully.");
        }
    }
}
