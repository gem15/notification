package com.severtrans.notification.model;

import java.util.Date;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
// @RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public class MonitorLog {
    @Id
    long id;
    String orderUID;
    String status;
    int msgType;
    String fileName;
    // @NonNull
    Date startDate = new Date();
    Date endDate;
    String msg;
    int vn;
    String info;

}
