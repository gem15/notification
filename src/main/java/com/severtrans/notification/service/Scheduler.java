package com.severtrans.notification.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Component
public class Scheduler {
//    @Scheduled(fixedDelay = 10000, initialDelay = 3000)
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void fixedDelaySch() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
//        System.out.println("Fixed Delay scheduler:: " + strDate);
        log.info("log "+strDate);

        //        Connect and login to the server.
//        Enter local passive mode for data connection.
//        Set file type to be transferred to binary.
//        Create an InputStream for the local file.
//                Construct path of the remote file on the server. The path can be absolute or relative to the current working directory.
//        Call one of the storeXXX()methods to begin file transfer.
        log.info("FTP");
        FTPClient ftp = new FTPClient();
//        ftp.connect(server, port);
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

        } catch (IOException e) {
            log.error(e.getMessage());
            e.printStackTrace();
        }

    }
}
