package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.service.NotificationType;
import com.thoughtworks.xstream.XStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    InputStream is;

    public InputStream send(NotificationType type) throws IOException {
        String sqlHeader="", alias="";

        switch (type){
            case E4102:
                sqlHeader = "select * from notif";
                alias="IssueReceiptForGoods";
                break;
            case E4104:
                sqlHeader = "select * from notif";
                break;
            case E4111:
                break;
        }

//        String sqlHeader = "select * from notif";
        List<Notification> list = jdbcTemplate.query(sqlHeader, new NotificationRowMapper());
        for (Notification not : list) {
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
            xs.omitField(Notification.class, "du");
            xs.alias(alias, Notification.class);
            xs.alias("Goods", NotificationItem.class);
            xs.addImplicitCollection(Notification.class, "Goods");

            try (Writer writer = new StringWriter()) {
                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                xs.toXML(not, writer);//        Notification notification = (Notification) xs.fromXML(xml);
//                System.out.println(writer.toString());
                is = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        return is;
    }
}
