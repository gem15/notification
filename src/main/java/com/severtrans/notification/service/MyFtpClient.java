package com.severtrans.notification.service;

import org.apache.commons.net.ftp.*;

import java.io.*;
import java.util.stream.Collectors;

public class MyFtpClient {

    private String server;
    private int port;
    private String user;
    private String password;
    FTPClient ftp;

    public MyFtpClient(String server, int port, String user, String password) {
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public void open() throws FTPException {
        ftp = new FTPClient();
        try {
            ftp.setFileType(FTP.ASCII_FILE_TYPE);
            ftp.connect(server, port);

            ftp.enterLocalPassiveMode();
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                //throw new IOException("Exception in connecting to FTP Server");
            }

            // TODO enhance ftp.login
            if (!ftp.login(user, password)) {
                ftp.logout();
                //throw new Exception("Login Error");// TODO make customt exception
            }
        } catch (IOException e) {
            //e.printStackTrace();
            throw new FTPException(e.getMessage());
        }
    }

    public void close() throws IOException {
        ftp.disconnect();
    }

    public FTPFile[] listFiles(String path) throws IOException {
        FTPFile[] files = ftp.listFiles(path, filter);
        return files;
    }

    FTPFileFilter filter = new FTPFileFilter() {

        @Override
        public boolean accept(FTPFile ftpFile) {
            return (ftpFile.isFile() && ftpFile.getName().endsWith(".xml"));
        }
    };

    public void putFileToPath(File file, String path) throws IOException {
        ftp.storeFile(path, new FileInputStream(file));
    }

    public void putFileToPath(InputStream content, String path) throws IOException {
        ftp.storeFile(path, content);
    }

    public void downloadFile(String source, String destination) throws IOException {
        FileOutputStream out = new FileOutputStream(destination);
        ftp.retrieveFile(source, out);
        out.close();
    }

    public String get(String fileName) throws IOException {
        InputStream remoteInput = ftp.retrieveFileStream(fileName);
//        completePendingCommand
        if (!ftp.completePendingCommand()) {
            System.out.println("Completing Pending Commands Not Successfull");
        }

        String result = new BufferedReader(new InputStreamReader(remoteInput)).lines()
                .collect(Collectors.joining("\n"));
        remoteInput.close();
        return result;
    }
}