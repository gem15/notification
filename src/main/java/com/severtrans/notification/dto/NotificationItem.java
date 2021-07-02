package com.severtrans.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Список товаров
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationItem {
    int LineNumber;// номер по порядку вставить в запрос rownum
    String Article;
    String Name;//наименование
    String ExpirationDate="====";//-дата окончания срока годности
    String ProductionDate="====";// дата производства
    String Lot="";// партия
    String SerialNum="-----------";// серийный номер
    String Marker = "-";
    String Marker2 = "-";
    String Marker3 = "-";
    int Count;// количество
    String Comment="comment";
}
