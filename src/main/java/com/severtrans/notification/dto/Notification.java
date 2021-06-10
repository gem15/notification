package com.severtrans.notification.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * УВЕДОМЛЕНИЯ о подтверждении отгрузки на выход уходит файл с префиксом «OUT»
 */
@Data
//@RequiredArgsConstructor()
public class Notification {
    String Date;
    String VehicleFactlArrivalTime; // фактическое время отгрузки
    String FactDeliveryDate;//время прибытия машины
    String Number;//номер документа клиента
    String Customer;// заказчик
    String OrderType = "Отгрузка";// тип заказа
    String TypeOfDelivery = "Отгрузка";// тип отгрузки
    String IDSupplier;// получатель
    String NameSupplier;// наименование получателя
    String AdressSupplier;// адрес получателя
    int VN;// код клиента
    String NumberCar;// номе машины
    String Driver;// имя водителя
    List<NotificationItem> Goods;// спсиок отгруженных товаров

    public Notification(String s, String s1) {
        this.OrderType = s;
        this.TypeOfDelivery = s1;
    }
}
