package com.severtrans.notification.repository;

import com.severtrans.notification.model.MonitorLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;

@Repository
public class MonitorLogDao {
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

    public MonitorLog findByID(String id) {
        String sql ="";
        try {
            return jdbcTemplate.queryForObject(sql,new Object[]{id},
            (rs, rowNum) ->
            new MonitorLog( //ID,STATUS,MSG_TYPE,FILE_NAME,MSG,VN,INFO
                    rs.getString("id"),
                    rs.getString("status"),
                    rs.getInt("MSG_TYPE"),
                    rs.getString("FILE_NAME"),
                    rs.getDate("START_DATE"),
                    rs.getDate("END_DATE"),
                    rs.getString("MSG"),
                    rs.getInt("VN"),
                    rs.getString("INFO")
            ));
        } catch (EmptyResultDataAccessException  e) {
            return null;
            
        }
        
    }

}
