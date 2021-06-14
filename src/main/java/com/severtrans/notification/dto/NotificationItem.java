package com.severtrans.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Товары
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationItem {
    //TODO int LineNumber;// номер по порядку вставить в запрос rownum
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
