package com.severtrans.notification.model;

import java.util.Date;

import org.springframework.data.annotation.Id;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorLog {
    @Id
    long id;
    String orderUID;
    @NonNull String status;
    int msgType;
    String fileName;
    Date startDate = new Date();
    Date endDate;
    String msg;
    int vn;
    String info;

}

/*
// @RequiredArgsConstructor
// @NoArgsConstructor
// @AllArgsConstructor
@Table("MONITOR_LOG")
public class MonitorLog {
    @Column("ORDER_UID") String orderUID;
    @Column("STATUS")  String status; //@NonNull
    @Column("MSG_TYPE") int msgType;
    @Column("FILE_NAME") String fileName;
    @Column("START_DATE") Date startDate = new Date();
    @Column("END_DATE") Date endDate;
    @Column("MSG") String msg;
    @Column("VN") int vn;
    @Column("INFO") String info;
    @Id
    @Column("ID") long id;

*/
