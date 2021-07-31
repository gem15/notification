package com.severtrans.notification;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.severtrans.notification.dto.jackson.Notification;

import org.springframework.jdbc.core.RowMapper;

public class NotificationRowMapper implements RowMapper<Notification> {

        @Override
        public Notification mapRow(ResultSet rs, int i) throws SQLException {
                Notification notification = new Notification();
                notification.setDu(rs.getString("id_du"));
                notification.setOrderID(rs.getString("id_obsl"));
                notification.setOrderDate(rs.getTimestamp("dt_sost"));
                notification.setVehicleFactlArrivalTime(rs.getTimestamp("dt_sost_end"));
                notification.setFactDeliveryDate(rs.getTimestamp("dt_veh"));
                notification.setOrderNo(rs.getString("sost_doc"));
                notification.setCustomerName(rs.getString("n_zak"));
                // notification.setOrderType(rs.getString(""));
                // //<OrderType>Поставка</OrderType>');
                // notification.set(rs.getString(""));
                // //<TypeOfDelivery>Поставка</TypeOfDelivery>');
                notification.setIDSupplier(rs.getString("id_suppl"));
                notification.setContractorName(rs.getString("n_zak")); // IDSupplier - от кого пришёл товар (с какого
                                                                     // завода хеламну) сотри 4101 куда схранялтсь
                                                                     // данные теги
                notification.setContractorAddress(rs.getString("ur_adr"));
                notification.setClientID(rs.getInt("id_klient"));
                notification.setLicencePlate(rs.getString("n_avto"));
                notification.setDriver(rs.getString("vodit"));
                return notification;
        }

 }
