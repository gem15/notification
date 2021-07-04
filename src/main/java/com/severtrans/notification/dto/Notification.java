package com.severtrans.notification.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * УВЕДОМЛЕНИЯ о подтверждении отгрузки на выход уходит файл с префиксом «OUT»
 */
// @XmlRootElement
@Data
//@RequiredArgsConstructor()
public class Notification {

    private String du;//link two tables
    private String orderID;

    private String Date;
    private String VehicleFactlArrivalTime; // фактическое время отгрузки
    private String FactDeliveryDate;//время прибытия машины
    private String Number;//номер документа клиента
    private String Customer;// заказчик
    private String OrderType = "Отгрузка";// тип заказа
    private String TypeOfDelivery = "Отгрузка";// тип отгрузки
    private String IDSupplier;// получатель
    private String NameSupplier;// наименование получателя
    private String AdressSupplier;// адрес получателя
    private int VN;// код клиента
    private String NumberCar;// номе машины
    private String Driver;// имя водителя
    private List items = new ArrayList();// спсиок отгруженных товаров

    public Notification() {
    }
}
