package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class NotificationRowMapper implements RowMapper<Notification> {

    SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
    SimpleDateFormat dateFormat1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    @Override
    public Notification mapRow(ResultSet rs, int i) throws SQLException {
        Notification notification=new Notification();
        notification.setDate(dateFormat.format(rs.getTimestamp("dt_sost")));
        notification.setVehicleFactlArrivalTime(dateFormat1.format(rs.getTimestamp("dt_sost_end")));
        notification.setFactDeliveryDate(dateFormat1.format(rs.getTimestamp("dt_veh")));
        notification.setNumber(rs.getString("sost_doc"));
        notification.setCustomer(rs.getString("zak_name"));
//        notification.set(rs.getString("")); //<OrderType>Поставка</OrderType>');
//        notification.set(rs.getString("")); //<TypeOfDelivery>Поставка</TypeOfDelivery>');
        notification.setIDSupplier(rs.getString("n_zak"));
        notification.setNameSupplier(rs.getString("id_suppl")); //IDSupplier - от кого пришёл товар (с какого завода хеламну) сотри 4101 куда схранялтсь данные теги
        notification.setAdressSupplier(rs.getString("n_zak"));
        notification.setAdressSupplier(rs.getString("ur_adr"));
        notification.setVN(rs.getInt("id_klient"));
        notification.setNumberCar(rs.getString("n_avto"));
        notification.setDriver(rs.getString("vodit"));
        return notification;
    }
}
