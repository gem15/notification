package com.severtrans.notification.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.charset.StandardCharsets;

@JdbcTest
class FtpTest {
    @Test
    void ftpTest() throws IOException {
        String server = "localhost";
        int port = 21;
        String user = "anonymous";
        String pass = "";


        FTPClient ftpClient = new FTPClient();
        try {
//            ftpClient.setAutodetectUTF8( true );
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.sendCommand("OPTS UTF8 ON");
            System.out.println(ftpClient.printWorkingDirectory());


            String fileName = "INЩ_PO_MK00-010610_2021-04-18-08-00-59.xml";
            InputStream is = new FileInputStream("src\\test\\resources\\files\\" + fileName);
            String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            if (!ftpClient.changeWorkingDirectory("/in"))
                System.out.println("error");
            boolean ok = ftpClient.storeFile(fileName, is);
            is.close();
            if (ok) {
                System.out.println("The file is uploaded successfully.");
            }else
                System.out.println(ftpClient.getReplyCode());

            ok = ftpClient.rename("/in/"+fileName, "/response/"+fileName);
            if (ok) {
                System.out.println("The file is renamed successfully.");
            }

            System.out.println("Stop");


/*
            // APPROACH #1: uploads first file using an InputStream
            File firstLocalFile = new File("D:/Test/Projects.zip");

            String firstRemoteFile = "Projects.zip";
            InputStream inputStream = new FileInputStream(firstLocalFile);

            System.out.println("Start uploading first file");
            boolean done = ftpClient.storeFile(firstRemoteFile, inputStream);
            inputStream.close();
            if (done) {
                System.out.println("The first file is uploaded successfully.");
            }
*/

/*
            // APPROACH #2: uploads second file using an OutputStream
            File secondLocalFile = new File("E:/Test/Report.doc");
            String secondRemoteFile = "test/Report.doc";
            inputStream = new FileInputStream(secondLocalFile);

            System.out.println("Start uploading second file");
            OutputStream outputStream = ftpClient.storeFileStream(secondRemoteFile);
            byte[] bytesIn = new byte[4096];
            int read = 0;

            while ((read = inputStream.read(bytesIn)) != -1) {
                outputStream.write(bytesIn, 0, read);
            }
            inputStream.close();
            outputStream.close();
*/

//            boolean completed = ftpClient.completePendingCommand();
//            if (completed) {
//                System.out.println("The second file is uploaded successfully.");
//            }

        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
