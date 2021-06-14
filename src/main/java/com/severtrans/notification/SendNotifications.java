package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.thoughtworks.xstream.XStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
//@Transactional(readOnly = true) must be placed in a service
public class SendNotifications {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    public SendNotifications() {
    }

    protected void send() {
        log.info("Main loop starting...");

        String sqlHeader = "select * from notif";
        List<Notification> list = jdbcTemplate.query(sqlHeader, new NotificationRowMapper());
        for (Notification not : list) {
            System.out.println(not.getCustomer());

//            String sqlItems = "select * from notifdet where iddu = '"+not.getDu()+"'";
            String sqlItems = "select * from notifdet where iddu =:id";
            int lineNo = 0;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");// HH:mm:ss
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id", not.getDu());
            List<NotificationItem> items = jdbcTemplate.query(sqlItems,
                    mapSqlParameterSource,
                    (rs, rowNum) -> new NotificationItem(
//TODO rownum                            rs.getInt(1),
                            rs.getString("SKU_ID"),
                            rs.getString("NAME"),
                            dateFormat.format(rs.getDate("EXPIRATION_DATE") == null ? new Date() : rs.getTimestamp("EXPIRATION_DATE")),
                            dateFormat.format(rs.getDate("PRODUCTION_DATE") == null ? new Date() : rs.getTimestamp("PRODUCTION_DATE")),
                            rs.getString("LOT"),
                            rs.getString("SERIAL_NUM"),
                            rs.getString("MARKER"),
                            rs.getString("MARKER2"),
                            rs.getString("MARKER3"),
                            rs.getInt("QTY"),
                            rs.getString("COMMENTS")
                    )
            );
            not.setGoods(items);
            XStream xs = new XStream();
            xs.omitField(Notification.class, "du"); //TODO check it
            xs.alias("IssueReceiptForGoods", Notification.class); //IssueReceiptForGoods
            xs.alias("Goods", NotificationItem.class);
            xs.addImplicitCollection(Notification.class, "Goods");
            System.out.println(xs.toXML(not));

        }
    }
}
