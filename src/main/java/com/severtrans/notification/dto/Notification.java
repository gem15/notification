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
@JsonPropertyOrder({"Date","VehicleFactlArrivalTime","FactDeliveryDate","Number","Customer","OrderType","TypeOfDelivery","IDSupplier","NameSupplier","AdressSupplier","VN","NumberCar","Driver","items",})
@JsonIgnoreProperties({"du","orderID"})
@Data
public class Notification {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("Date")
    Date Date;
    @JsonProperty("VehicleFactlArrivalTime")
    String VehicleFactlArrivalTime; // фактическое время отгрузки
    @JsonProperty("FactDeliveryDate")
    String FactDeliveryDate;//время прибытия машины
    @JsonProperty("Number")
    String Number;//номер документа клиента
    @JsonProperty("Customer")
    String Customer;// заказчик
    @JsonProperty("OrderType")
    String OrderType;// тип заказа
    @JsonProperty("TypeOfDelivery")
    String TypeOfDelivery;// тип отгрузки
    @JsonProperty("IDSupplier")
    String IDSupplier;// получатель
    @JsonProperty("NameSupplier")
    String NameSupplier;// наименование получателя
    @JsonProperty("AdressSupplier")
    String AdressSupplier;// адрес получателя
    @JsonProperty("VN")
    int VN;// код клиента
    @JsonProperty("NumberCar")
    String NumberCar;// номе машины
    @JsonProperty("Driver")
    String Driver;// имя водителя
    
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Goods")
     List<NotificationItem> items = new ArrayList<>();// спсиок отгруженных товаров

    String du;//link two tables
    String orderID;
}
