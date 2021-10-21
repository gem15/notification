package com.severtrans.notification.model;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class MonitorLog {
    final String  id;
    final String status;
    final int msgType;
    final String fileName;
    Date startDate;
    Date endDate;
    final String msg;
    final int vn;
    /**
     * Error messages and any other info
     */
    final String info;

}
