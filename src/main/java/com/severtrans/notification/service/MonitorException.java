package com.severtrans.notification.service;

public class MonitorException extends Exception {

    private int customerID;
    private  String docNo;
    private int msgType;

    public int getCustomerID() {
        return customerID;
    }

    public String getDocNo() {
        return docNo;
    }

    public int getMsgType() {
        return msgType;
    }


    public MonitorException(String message) {
        super(message);
    }

    public MonitorException(String message, int customerID,int msgType,String docNo) {
		super(message);
		this.docNo = docNo;
        this.msgType = msgType;
        this.customerID = customerID;
	}
    public MonitorException(String message, int msgType) {
		super(message);
        this.msgType = msgType;
	}

}

