package com.severtrans.notification.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.jdbc.core.RowMapper;

public class NotificationItemRowMapper implements RowMapper<NotificationItem> {
 
        @Override
        public NotificationItem mapRow(ResultSet rs, int i) throws SQLException {
                NotificationItem ni = new NotificationItem();
                ni.setLineNumber(rs.getInt("ROWNUM"));
                ni.setArticle(rs.getString("SKU_ID"));
                ni.setName(rs.getString("NAME"));
                ni.setExpirationDate(d2s(rs.getDate("EXPIRATION_DATE")));
                ni.setProductionDate(d2s(rs.getDate("PRODUCTION_DATE")));
                ni.setLot(rs.getString("LOT"));
                ni.setMarker(rs.getString("MARKER"));
                ni.setMarker2(rs.getString("MARKER2"));
                ni.setMarker3(rs.getString("MARKER3"));
                ni.setCount(rs.getInt("QTY"));
                ni.setComment(rs.getString("COMMENTS"));
                ni.setSerialNum(rs.getString("SERIAL_NUM"));
                return ni;

        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        private String d2s(Date date) {
                if (date == null)
                        return "";
                else
                        return dateFormat.format(date);
        }
}
