package com.severtrans.notification.repository;

import java.util.Date;
import java.util.List;

import com.severtrans.notification.model.MonitorLog;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Deprecated(forRemoval = true)
@Repository
public class MonitorLogDaoOld {
    /**
     *
     */
    // private static final String SELECT_MONITOR_LOG = "SELECT
    // ORDER_UID,STATUS,MSG_TYPE,START_DATE,END_DATE,FILE_NAME,MSG,VN,INFO FROM
    // MONITOR_LOG WHERE ID = :id";

    // private static final String INSERT_QUERY = "Insert into MONITOR_LOG
    // (ORDER_UID,STATUS,MSG_TYPE,FILE_NAME,MSG,VN,INFO) values
    // (?,?,?,?,?,?,?)";//START_DATE,END_DATE,

    private static final String UPDATE_STATUS_QUERY = "update MONITOR_LOG set STATUS = ?, INFO = ?, END_DATE = sysdate where ID = ?";

    // @Autowired
    // JdbcTemplate jdbcTemplate;

    // @Autowired
    // private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private JdbcTemplate jdbcTemplate;

 
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    /**
     * Сохранение лога
     * 
     * @param ml - MonitorLog
     * @return присвоенный ID
     */
    public long save(MonitorLog ml) {

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("monitor_log")
                .usingGeneratedKeyColumns("id");
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("ORDER_UID", ml.getOrderUID())
                .addValue("STATUS", ml.getStatus()).addValue("MSG_TYPE", ml.getMsgType())
                .addValue("FILE_NAME", ml.getFileName()).addValue("MSG", ml.getMsg()).addValue("VN", ml.getVn())
                .addValue("INFO", ml.getInfo()).addValue("START_DATE", new Date());
        Number id = simpleJdbcInsert.executeAndReturnKey(params);
        return id.longValue();
    }

    /**
     * Обновление статуса сообщения и доп. информации (при наличии)
     * 
     * @param status
     * @param info
     * @param id
     * @return количество обновлённых записей
     */
    public int updateStatus(String status, String info, long id) {
        return jdbcTemplate.update(UPDATE_STATUS_QUERY, status, info, id);
    }

 /*    public List<MonitorLog> findCompletedOrders() {

        // MapSqlParameterSource param = new MapSqlParameterSource();
        // param.addValue("name", "%" + status + "%");
        // param.addValue("price", 0);

        return jdbcTemplate.query(
                "select * from monitor_log where status not in ('S','E')",
                // param,
                (rs, rowNum) -> new MonitorLog(
                        rs.getLong("ID"),
                        rs.getString("ORDER_UID"),
                        rs.getString("STATUS"),
                        rs.getInt("MSG_TYPE"),
                        rs.getString("FILE_NAME"),
                        rs.getDate("START_DATE"),
                        rs.getDate("END_DATE"),
                        rs.getString("MSG"),
                        rs.getInt("VN"),
                        rs.getString("INFO")));
    }
 */
    /**
     * Поиск сообщения по уникальному ID
     * 
     * @param id - msgID from message
     * @return null or MonitorLog instance
     */
    // public MonitorLog findByID(String id) {
    // try {
    // return jdbcTemplate.queryForObject(SELECT_MONITOR_LOG,
    // (rs, rowNum) -> new MonitorLog(rs.getString("id"), rs.getString("status"),
    // rs.getInt("MSG_TYPE"),
    // rs.getString("FILE_NAME"), rs.getDate("START_DATE"), rs.getDate("END_DATE"),
    // rs.getString("MSG"), rs.getInt("VN"), rs.getString("INFO")),
    // new Object[] { id });
    // } catch (EmptyResultDataAccessException e) {
    // return null;
    // }
    // }

    // ищем в логе
    // MonitorLog monitorLog = namedParameterJdbcTemplate.queryForObject(
    // "SELECT * FROM MONITOR_LOG WHERE ID = :ID",
    // new MapSqlParameterSource().addValue("ID", shell.getMsgID()),
    // (rs, rowNum) -> new MonitorLog(rs.getString("id"), rs.getString("name"),
    // rs.getInt("age"),
    // rs.getString("e"), rs.getDate("e"), rs.getDate("e")));

}
