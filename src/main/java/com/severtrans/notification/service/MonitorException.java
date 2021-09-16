package com.severtrans.notification.service;

public class MonitorException extends Exception {

    private  String docNo;
    private int msgType;

    public String getDocNo() {
        return docNo;
    }

    public int getMsgType() {
        return msgType;
    }


    public MonitorException(String message) {
        super(message);
    }

    public MonitorException(String message, int msgType,String docNo) {
		super(message);
		this.docNo = docNo;
        this.msgType = msgType;
	}
    public MonitorException(String message, int msgType) {
		super(message);
        this.msgType = msgType;
	}

}

