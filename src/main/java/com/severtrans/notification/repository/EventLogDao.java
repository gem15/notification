package com.severtrans.notification.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventLogDao {
    @Autowired
    JdbcTemplate jdbcTemplate;

     public void findByEventCode(int custID,String msgID, String eventID) {
        String sql = "SELECT count(*) FROM kb_sost st " 
        + " INNER JOIN kb_spros sp ON st.id_obsl = sp.ID"
        + " INNER JOIN kb_zak z ON z.ID = sp.id_zak" + " WHERE z.id_klient = :custID"
        + " AND z.id_usr IS NOT NULL" + " AND  st.id_sost = :eventID" //'KB_USL99770'
        + " AND UPPER(st.id_du)= UPPER(:msgID)";
// MapSqlParameterSource params = new MapSqlParameterSource().addValue("custID", 1)
//         .addValue("msgID", shell.getMsgID());

// if (namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class) > 0)
//     throw new MonitorException("Заказ уже существует");

        
    }
}
