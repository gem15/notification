package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class NotificationItemRowMapper implements RowMapper<NotificationItem> {
    @Override
    public NotificationItem mapRow(ResultSet resultSet, int i) throws SQLException {

        return null;
    }
}
