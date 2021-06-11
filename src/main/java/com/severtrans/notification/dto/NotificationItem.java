package com.severtrans.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Товары
 */
@Data
public class NotificationItem {
    int LineNumber;// номер по порядку
    String Article;
    String Name;//наименование
    String ExpirationDate;//-дата окончания срока годности
    String ProductionDate;// дата производства
    String Lot;// партия
    String SerialNum;// серийный номер
    String Marker = "-";
    String Marker2 = "-";
    String Marker3 = "-";
    int Count;// количество
    String Comment;
}
