package com.severtrans.notification.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;

/**
 * УВЕДОМЛЕНИЯ о подтверждении отгрузки на выход уходит файл с префиксом «OUT»
 */
@JsonPropertyOrder({ "Date", "VehicleFactlArrivalTime", "FactDeliveryDate", "Number", "Customer", "OrderType",
        "TypeOfDelivery", "IDSupplier", "NameSupplier", "AdressSupplier", "VN", "NumberCar", "Driver", "Goods", })
@JsonIgnoreProperties({ "du", "orderID" })
@Data
public class Notification {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("DateDoc")
    Date orderDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @JsonProperty("VehicleFactlArrivalTime")
    Date VehicleFactlArrivalTime; // фактическое время отгрузки
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    @JsonProperty("FactDeliveryDate")
    Date FactDeliveryDate;// время прибытия машины
    @JsonProperty("NumberDoc")
    String orderNo;// номер документа клиента
    @JsonProperty("Customer")
    String customerName;// заказчик
    @JsonProperty("OrderType")
    String orderType;// тип заказа
    @JsonProperty("TypeOfDelivery")
    String TypeOfDelivery;// тип отгрузки
    @JsonProperty("IDSupplier")
    String IDSupplier;// получатель
    @JsonProperty("NameSupplier")
    String contractorName;// наименование получателя
    @JsonProperty("AdressSupplier")
    String contractorAddress;// адрес получателя
    @JsonProperty("VN")
    int clientID;// код клиента
    @JsonProperty("NumberCar")
    String licencePlate;// номе машины
    @JsonProperty("Driver")
    String driver;// имя водителя

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Goods")
    List<NotificationItem> items = new ArrayList<>();// спсиок отгруженных товаров

    String du;// link two tables
    String orderID;
}
