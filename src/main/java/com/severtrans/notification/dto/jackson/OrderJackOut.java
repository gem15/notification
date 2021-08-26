package com.severtrans.notification.dto.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

@JsonIgnoreProperties({"Error", "TypeCar", "Customer", "IDCarrier"})
@JsonPropertyOrder({"VN", "NumberDoc", "DateDoc", "OrderType", "TypeOfDelivery", "PlannedShipmentDate", "IDConsignee",
        "NameConsignee", "AdressConsignee", "NumberCar", "Driver", "GUID","Comment", "Goods",})
@Data
public class OrderJackOut {
    @JsonProperty("GUID")
    private String guid;
    @JsonProperty("VN")
    private int clientID;
    @JsonProperty("NumberDoc")
    private String orderNo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("DateDoc")
    private Date orderDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("PlannedShipmentDate")
    private Date plannedDate;
    @JsonProperty("OrderType")
    private String orderType;
    @JsonProperty("TypeOfDelivery")
    private String deliveryType;
    @JsonProperty("IDConsignee")
    private String contrCode;
    @JsonProperty("NameConsignee")
    private String contrName;
    @JsonProperty("AdressConsignee")
    private String contrAddress;
    //    private String carrierId;
    // TODO <TypeCar>10</TypeCar> нафига? private String vehicleType;
    @JsonProperty("NumberCar")
    private String licencePlate;
    @JsonProperty("Driver")
    private String driver;
    @JsonProperty("Comment")
    String comment;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Goods")
    private List<OrderLineJack> orderLine;

}
/**
 * <Error> </Error> <NumberDoc>ТС-00000019</Number> <DateDoc>14.10.2020</Date>
 * <Customer>Хеллманн</Customer> <OrderType>Поставка</OrderType>
 * <TypeOfDelivery>Поставка</TypeOfDelivery>
 * <PlannedDeliveryDate>15.10.2020</PlannedDeliveryDate>
 * <IDSupplier>УТ-УТ000047</IDSupplier> <NameSupplier>Winch Solutions
 * Ltd</NameSupplier> <AdressSupplier>109044, Москва </AdressSupplier>
 * <VN>300185</VN> <IDCarrier>ТС-УТ000051</IDCarrier> <TypeCar>10</TypeCar>
 * <NumberCar>ПР896Г/98</NumberCar> <Driver>Иванов Константин
 * Александрович</Driver>
 */