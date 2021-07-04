package com.severtrans.notification.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.jdbc.core.RowMapper;

public class NotificationItemRowMapper implements RowMapper<NotificationItem> {

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

        @Override
        public NotificationItem mapRow(ResultSet rs, int i) throws SQLException {
                NotificationItem ni = new NotificationItem();
                ni.setLineNumber(rs.getInt("ROWNUM"));
                ni.setArticle(rs.getString("SKU_ID"));
                ni.setName(rs.getString("NAME"));
                ni.setExpirationDate(dateFormat.format(
                                rs.getDate("EXPIRATION_DATE") == null ? new Date() : rs.getDate("EXPIRATION_DATE")));
                ni.setProductionDate(dateFormat.format(
                                rs.getDate("PRODUCTION_DATE") == null ? new Date() : rs.getDate("PRODUCTION_DATE")));

                ni.setLot(rs.getString("LOT"));
                ni.setMarker(rs.getString("MARKER"));
                ni.setMarker2(rs.getString("MARKER2"));
                ni.setMarker3(rs.getString("MARKER3"));
                ni.setCount(rs.getInt("QTY"));
                ni.setComment(rs.getString("COMMENTS") == null ? "NA" : rs.getString("COMMENTS"));
                ni.setSerialNum(rs.getString("SERIAL_NUM") == null ? "NA" : rs.getString("SERIAL_NUM"));
                return ni;

        }
        /*
         * new NotificationItem(rs.getInt("ROWNUM"), rs.getString("SKU_ID"),
         * rs.getString("SKU_ID"), dateFormat.format(rs.getDate("EXPIRATION_DATE") ==
         * null ? new Date() : rs.getTimestamp("EXPIRATION_DATE")),
         * dateFormat.format(rs.getDate("PRODUCTION_DATE") == null ? new Date() :
         * rs.getTimestamp("PRODUCTION_DATE")), rs.getString("LOT"),
         * rs.getString("SERIAL_NUM"), rs.getString("MARKER"), rs.getString("MARKER2"),
         * rs.getString("MARKER3"), rs.getInt("QTY"), rs.getString("COMMENTS")));
         */}
