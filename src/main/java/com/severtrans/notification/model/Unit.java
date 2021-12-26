package com.severtrans.notification.model;

import org.springframework.data.annotation.Id;

import lombok.Data;

@Data
public class Unit {
    @Id
    String id;
    String code;
    String name;
}
