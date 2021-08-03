package com.severtrans.notification.model;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerRowMapper implements RowMapper<Customer> {
    @Override
    public Customer mapRow(ResultSet rs, int i) throws SQLException {
        Customer customer=new Customer();
        customer.setId(rs.getString("ID"));
        customer.setDefaultPlatform(rs.getString("ID_SVH"));
        customer.setHolderID(rs.getInt("ID_WMS"));
        customer.setGateId(rs.getString("ID_USR"));
        customer.setCustomerName(rs.getString("N_ZAK"));
        customer.setClientId(rs.getInt("ID_KLIENT"));
        customer.setPrefix(rs.getString("PRF_WMS"));
        return customer;
    }
    //ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT
}
