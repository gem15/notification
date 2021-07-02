package com.severtrans.notification.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class NotificationItemRowMapper implements RowMapper<NotificationItem>{
    
    @Override
    public NotificationItem mapRow(ResultSet rs, int i) throws SQLException {
        NotificationItem ni=new NotificationItem();
        ni.setLineNumber(rs.getInt(""));
        ni.setArticle(rs.getString(""));
ni.setName(rs.getString(""));//
ni.setExpirationDate(rs.getString(""));
ni.setProductionDate(rs.getString(""));
ni.setLot(rs.getString(""));="";
ni.setSerialNum(rs.getString(""));
ni.setMarker(rs.getString("")); 
ni.setMarker2(rs.getString("")); 
ni.setMarker3(rs.getString("")); 
ni.setCount(rs.getInt(""));
ni.setComment(rs.getString(""));
        return null;
        
    }
}
