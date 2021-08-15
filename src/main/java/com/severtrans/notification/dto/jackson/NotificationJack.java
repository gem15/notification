package com.severtrans.notification.dto.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * УВЕДОМЛЕНИЯ о подтверждении отгрузки на выход уходит файл с префиксом «OUT»
 */
@JsonPropertyOrder({ "Date", "VehicleFactlArrivalTime", "FactDeliveryDate", "Number", "Customer", "OrderType",
        "TypeOfDelivery", "IDSupplier", "NameSupplier", "AdressSupplier", "VN", "NumberCar", "Driver", "Goods", })
@JsonIgnoreProperties({ "du", "orderID","guid"})
@Data
public class NotificationJack {
    /**
     * GUID заказа
     */
    String guid;
    @JsonProperty("NumberDoc")
    String orderNo;// номер документа клиента
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("DateDoc")
    Date orderDate;
    @JsonProperty("IDSupplier")
    String contrCode;// получатель
    @JsonProperty("NameSupplier")
    String contrName;// наименование получателя
    @JsonProperty("AdressSupplier")
    String contrAddress;// адрес получателя
    @JsonProperty("VN")
    int clientID;// код клиента
    @JsonProperty("NumberCar")
    String licencePlate;// номе машины
    @JsonProperty("Driver")
    String driver;// имя водителя

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy hh:mm:ss")
    @JsonProperty("VehicleFactlArrivalTime")
    Date actualArrivalTime; // фактическое время прибытия/отбытия машины
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy hh:mm:ss")
    @JsonProperty("FactDeliveryDate")
    Date actualDeliveryTime;// время разгрузки/погрузки    //время прибытия машины

    @JsonProperty("Customer")
    String customerName;// заказчик нафига ?
    @JsonProperty("OrderType")
    String orderType;// тип заказа // нафига?
    @JsonProperty("TypeOfDelivery")
    String TypeOfDelivery;// тип отгрузки // нафига?

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Goods")
    List<NotificationItem> orderLine = new ArrayList<>();// спсиок отгруженных товаров

    String du;// link two tables
    String orderID;
}
