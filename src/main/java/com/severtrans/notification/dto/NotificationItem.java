package com.severtrans.notification.dto;

import lombok.Data;

import java.util.Date;

/**
 * Товары
 */
@Data
public class NotificationItem {
    int LineNumber;// номер по порядку
    String Article;
    String Name;//наименование
    Date ExpirationDate;//-дата окончания срока годности
    Date ProductionDate;// дата производства
    String Lot;// партия
    String SerialNum;// серийный номер
    String Marker;
    String Marker2;
    String Marker3;
    int Count;// количество
    String Comment;
}
