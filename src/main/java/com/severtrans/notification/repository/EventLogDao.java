package com.severtrans.notification.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventLogDao {

    private static final String SELECT_4110 = "SELECT st.id_obsl FROM kb_sost st WHERE st.id_sost = 'KB_USL60183'"
            + " AND id_obsl = (SELECT id_obsl FROM kb_sost WHERE id_sost = 'KB_USL99770'  AND id_du = ?)";

    @Autowired
    JdbcTemplate jdbcTemplate;

    /**
     * Проверка на наличие события <b>4110 Заказ в работе на СОХ</b>
     * @param orderID - уникальный ID заказа
     * @return true/false
     */
    public boolean check4110(String orderID) {
        String sql = "SELECT COUNT(*) FROM kb_sost st" + " WHERE st.id_sost = 'KB_USL60183' AND " //--4110 Заказ в работе на СОХ
                + " id_obsl = ( SELECT id_obsl FROM kb_sost" + "  WHERE id_sost = 'KB_USL99770'  AND id_du = ?)";//--Получено входящее сообщение 4301 "19c0a03-2817-11ec-8101-00155d57bcb9
        return jdbcTemplate.queryForObject(sql, Integer.class, orderID) > 0;
    }

    public String findOrderIDByOrderGuid(String id) {

        try {
            return jdbcTemplate.queryForObject(SELECT_4110, String.class, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
