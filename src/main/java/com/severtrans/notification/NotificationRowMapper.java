package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationRowMapper implements RowMapper<Notification> {

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
//12 > 10 ? ++exp1 : ++exp2
    @Override
    public Notification mapRow(ResultSet rs, int i) throws SQLException {
        Notification notification=new Notification();
        notification.setDu(rs.getString("id_du"));
//         notification.setOrderID(rs.getString("id_obsl"));
//         notification.setDate(dateFormat.format(rs.getTimestamp("dt_sost")));
//         notification.setVehicleFactlArrivalTime(dateFormat1.format(
//                 rs.getTimestamp("dt_sost_end") == null ?new Date():rs.getTimestamp("dt_sost_end")));
//         notification.setFactDeliveryDate(dateFormat1.format(
//                 rs.getTimestamp("dt_veh") == null ?new Date():rs.getTimestamp("dt_veh")));
        // notification.setNumber(rs.getString("sost_doc"));
//         notification.setCustomer(rs.getString("n_zak"));
// //        notification.setOrderType(rs.getString("")); //<OrderType>Поставка</OrderType>');
// //        notification.set(rs.getString("")); //<TypeOfDelivery>Поставка</TypeOfDelivery>');
//        notification.setIDSupplier(rs.getString("id_suppl"));
//         notification.setNameSupplier(rs.getString("n_zak")); //IDSupplier - от кого пришёл товар (с какого завода хеламну) сотри 4101 куда схранялтсь данные теги
//         notification.setAdressSupplier(rs.getString("ur_adr"));
//         notification.setVN(rs.getInt("id_klient"));
//         notification.setNumberCar(rs.getString("n_avto"));
//         notification.setDriver(rs.getString("vodit"));
        return notification;
    }
}
