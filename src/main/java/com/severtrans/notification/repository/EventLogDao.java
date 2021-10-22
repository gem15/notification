package com.severtrans.notification.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventLogDao {
    @Autowired
    JdbcTemplate jdbcTemplate;
/**
 * Проверка на наличие события <b>4110 Заказ в работе на СОХ</b>
 * @param orderID - уникальный ID заказа
 * @return true/false
 */
    public boolean check4101(String orderID) {
        String sql = "SELECT COUNT(*) FROM kb_sost st"
                + " WHERE st.id_sost = 'KB_USL60183' AND " //--4110 Заказ в работе на СОХ
                + " id_obsl = ( SELECT id_obsl FROM kb_sost"
                + "  WHERE id_sost = 'KB_USL99770'  AND id_du = ?)";//--Получено входящее сообщение 4301 "19c0a03-2817-11ec-8101-00155d57bcb9
             return jdbcTemplate.queryForObject(sql, Integer.class, orderID) > 0;
    }
}
