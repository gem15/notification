package com.severtrans.notification.repository;

import com.severtrans.notification.model.MonitorLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.dao.EmptyResultDataAccessException;

@Repository
public class MonitorLogDao {
    /**
     *
     */
    private static final String SELECT_MONITOR_LOG = "SELECT ID,STATUS,MSG_TYPE,START_DATE,END_DATE,FILE_NAME,MSG,VN,INFO FROM MONITOR_LOG WHERE ID = :id";

    private static final String INSERT_QUERY = "Insert into MONITOR_LOG (ID,STATUS,MSG_TYPE,FILE_NAME,MSG,VN,INFO) values (?,?,?,?,?,?,?)";//START_DATE,END_DATE,

    private static final String UPDATE_STATUS_QUERY = "update MONITOR_LOG set STATUS = ?, INFO = ?, END_DATE = sysdate where ID = ?";

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Сохранение статуса 
     * @param ml - MonitorLog
     * @return количество сохранёных записей
     */
    public int save(MonitorLog ml) {
        return jdbcTemplate.update(INSERT_QUERY, ml.getId(), ml.getStatus(), ml.getMsgType(), ml.getFileName(),
                ml.getMsg(), ml.getVn(), ml.getInfo());
    }

    /**
     * Обновление статуса сообщения и доп. информации (при наличии)
     * @param status
     * @param info
     * @param id
     * @return количество обновлённых записей
     */
    public int updateStatus(String status, String info, String id) {
        return jdbcTemplate.update(UPDATE_STATUS_QUERY, status, info, id);
    }

    /**
     * Поиск сообщения по уникальному ID
     * @param id - msgID from message
     * @return null or MonitorLog instance
     */
    public MonitorLog findByID(String id) {
        try {
            return jdbcTemplate.queryForObject(SELECT_MONITOR_LOG,
                    (rs, rowNum) -> new MonitorLog(rs.getString("id"), rs.getString("status"), rs.getInt("MSG_TYPE"),
                            rs.getString("FILE_NAME"), rs.getDate("START_DATE"), rs.getDate("END_DATE"),
                            rs.getString("MSG"), rs.getInt("VN"), rs.getString("INFO")),
                    new Object[] { id });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

}
