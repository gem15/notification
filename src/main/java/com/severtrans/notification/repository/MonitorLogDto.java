package com.severtrans.notification.repository;

import com.severtrans.notification.model.MonitorLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MonitorLogDto {
    private static final String INSERT_QUERY = "Insert into MONITOR_LOG (ID,STATUS,MSG_TYPE,FILE_NAME,MSG,VN,INFO) values (?,?,?,?,?,?,?)";//START_DATE,END_DATE,

    private static final String UPDATE_STATUS_QUERY = "update MONITOR_LOG set STATUS = ?, INFO = ?, END_DATE = sysdate where ID = ?";

    @Autowired
    JdbcTemplate jdbcTemplate;

    public int save(MonitorLog ml) {
        return jdbcTemplate.update(INSERT_QUERY, ml.getId(), ml.getStatus(), ml.getMsgType(), ml.getFileName(),
                ml.getMsg(), ml.getVn(), ml.getInfo());
    }

    public int updateStatus(String status, String info, String id) {
        return jdbcTemplate.update(UPDATE_STATUS_QUERY, status, info, id);

    }

}
