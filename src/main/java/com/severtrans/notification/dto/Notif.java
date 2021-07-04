package com.severtrans.notification.dto;

import lombok.Data;

@Data
public class Notif {
    private String du;//link two tables
    // private String orderID;

    // private String Date;
    // private String VehicleFactlArrivalTime; // фактическое время отгрузки
    // private String FactDeliveryDate;//время прибытия машины
    // private String Number;//номер документа клиента
    // private String Customer;// заказчик
    private String OrderType = "Отгрузка";// тип заказа
    private String TypeOfDelivery = "Отгрузка";// тип отгрузки
}
