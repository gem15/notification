package com.severtrans.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Список товаров
 */
@Data
// @AllArgsConstructor
// @NoArgsConstructor
public class NotificationItem {
    private int LineNumber;// номер по порядку вставить в запрос rownum
    private String Article;
    private String Name;//наименование
    private String ExpirationDate;//-дата окончания срока годности
    private String ProductionDate;// дата производства
    private String Lot="";// партия
    private String SerialNum;// серийный номер
    private String Marker = "-";
    private String Marker2 = "-";
    private String Marker3 = "-";
    private int Count;// количество
    private String Comment="comment";

    public NotificationItem(){
    }
}
