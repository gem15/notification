package com.severtrans.notification;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.severtrans.notification.dto.jackson.NotificationJack;

import org.springframework.jdbc.core.RowMapper;

public class NotificationRowMapper implements RowMapper<NotificationJack> {

        @Override
        public NotificationJack mapRow(ResultSet rs, int i) throws SQLException {
                NotificationJack notificationJack = new NotificationJack();
                notificationJack.setGuid(rs.getString("order_id")); //GUID
               notificationJack.setDu(rs.getString("id_du"));
                notificationJack.setOrderID(rs.getString("id_obsl"));
                notificationJack.setOrderDate(rs.getTimestamp("dt_sost"));
                notificationJack.setActualArrivalTime(rs.getTimestamp("dt_veh"));
                notificationJack.setOrderNo(rs.getString("sost_doc"));
                 notificationJack.setActualDeliveryTime(rs.getTimestamp("dt_sost_end"));
                notificationJack.setCustomerName(rs.getString("n_zak"));
                // notification.setOrderType(rs.getString(""));
                // //<OrderType>Поставка</OrderType>');
                // notification.set(rs.getString(""));
                // //<TypeOfDelivery>Поставка</TypeOfDelivery>');
                notificationJack.setContrCode(rs.getString("id_suppl"));
                notificationJack.setContrName(rs.getString("n_zak")); // IDSupplier - от кого пришёл товар (с какого
                                                                     // завода хеламну) сотри 4101 куда схранялтсь
                                                                     // данные теги
                notificationJack.setContrAddress(rs.getString("ur_adr"));
                notificationJack.setClientID(rs.getInt("id_klient"));
                notificationJack.setLicencePlate(rs.getString("n_avto"));
                notificationJack.setDriver(rs.getString("vodit"));
                return notificationJack;
        }

 }
