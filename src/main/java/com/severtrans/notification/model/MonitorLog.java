package com.severtrans.notification.model;

import java.util.Date;

public class MonitorLog {
    /**
     (	"ID" VARCHAR2(36 BYTE) NOT NULL ENABLE, 
	"CUST_ID" VARCHAR2(38 BYTE), 
	"STATUS" VARCHAR2(20 BYTE) NOT NULL ENABLE, 
	"MSG_TYPE" NUMBER(*,0), 
	"FILE_NAME" VARCHAR2(120 BYTE), 
	"START_DATE" DATE, 
	"END_DATE" DATE, 
     */
    String id;
    String status;
    int msgType;
    String fileName;
    Date startDate;
    Date endDate;
    
}
