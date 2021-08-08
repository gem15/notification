package com.severtrans.notification.dto.jackson;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Data;

@JsonIgnoreProperties({ "Error", "TypeCar", "Customer", "IDCarrier" })
@JsonPropertyOrder({ "NumberDoc", "DateDoc", "Customer", "OrderType", "TypeOfDelivery", "PlannedDeliveryDate", "IDSupplier",
        "NameSupplier", "AdressSupplier", "VN", "NumberCar", "Driver", "Comment","Goods", })
@Data
public class OrderJackIn {
    @JsonProperty("VN")
    private int clientID;
    @JsonProperty("NumberDoc")
    private String orderNo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("DateDoc")
    private Date orderDate;
    @JsonProperty("Customer")
    String clientName;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("PlannedDeliveryDate")//TODO PlannedShipmentDate
    private Date plannedDate;
    @JsonProperty("OrderType")
    private String orderType;
    @JsonProperty("TypeOfDelivery")
    private String deliveryType;
    @JsonProperty("IDSupplier")//TODO IDConsignee
    private String contrCode;
    @JsonProperty("NameSupplier")//TODO NameConsignee
    private String contrName;
    @JsonProperty("AdressSupplier")//TODO AdressConsignee
    private String contrAddress;
/*
     TODO <TypeCar>10</TypeCar> нафига? private String vehicleType;
        <IDCarrier>ТС-ТС000022</IDCarrier> - код перевозчика
    <TypeCar>15</TypeCar> тип машины
*/
    @JsonProperty("NumberCar")
    private String licencePlate;
    @JsonProperty("Driver")
    private String driver;
    @JsonProperty("Comment")
    String comment;

    /*
     * private Contractor contractor; private Vehicle vehicle;
     */
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