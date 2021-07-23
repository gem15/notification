package com.severtrans.notification.dto;

import java.util.Date;

import lombok.Data;

@Data
public class Order {
    
    protected String orderNo;
    protected Date orderDate;
    
    protected Date plannedDate;
    
    protected String orderType;
    
    protected String deliveryType;
    protected Contractor contractor;
    protected Vehicle vehicle;
    
    protected List<OrderLineItem> lineItem;
    
}
