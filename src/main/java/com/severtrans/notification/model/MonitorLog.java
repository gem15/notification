package com.severtrans.notification.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitorLog {
    String id;
    String status;
    int msgType;
    String fileName;
    Date startDate;
    Date endDate;
    String msg;
    String vn;
    String info;

}
