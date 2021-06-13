package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Slf4j
@Repository
public class SendNotifications {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public SendNotifications() {
    }

    protected void MainLoop() {
        log.info("Main loop starting...");

        String sqlHeader = "select * from notif";
        List<Notification> list = jdbcTemplate.query(sqlHeader, new NotificationRowMapper());
        for (Notification not : list) {
            System.out.println(not.getCustomer());

            String sqlItems = "select * from notifdet where iddu = '"+not.getDu()+"'";
            int lineNo = 0;

            List<NotificationItem> items = jdbcTemplate.query(sqlItems,
                    (rs, rowNum) -> new NotificationItem(
//                            rs.getInt(1),
                            rs.getString("SKU_ID"),
                            rs.getString("NAME"),
                            rs.getDate("EXPIRATION_DATE"),
                            rs.getDate("PRODUCTION_DATE"),
                            rs.getString("LOT"),
                            rs.getString("SERIAL_NUM"),
                            rs.getString("MARKER"),
                            rs.getString("MARKER2"),
                            rs.getString("MARKER3"),
                            rs.getInt("QTY"),
                            rs.getString("COMMENTS")
                    )//,
//                    new MapSqlParameterSource() //.registerSqlType("id", Types.VARCHAR))
//                            .addValue("id", not.getDu().toString(), Types.VARCHAR)
            );
//            System.out.println(items);
            if (items.size() != 0){
            for (NotificationItem item : items) {
                    System.out.println(item.getArticle()+"   "+item.getName());
            }} else System.out.print("---");
        }
    }
}
