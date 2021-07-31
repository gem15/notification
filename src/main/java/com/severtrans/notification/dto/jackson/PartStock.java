package com.severtrans.notification.dto.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonPropertyOrder({"CustomerID", "timeStamp"})
@JacksonXmlRootElement(localName = "PART_STOCK")
@Data
public class PartStock {
    @JsonProperty("VN")
    int clientId;
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd.MM.yyyy hh:mm:ss")//,timezone = "Europe/Moscow")
    @JsonProperty("TIME_STAMP")
    Date timeStamp;

    @JacksonXmlElementWrapper(localName = "PART_STOCK_LINE_LIST")
    @JsonProperty("PART_STOCK_LINE_TEMPL")
    List<PartStockLine> stockLines = new ArrayList<>();
}

    /*<?xml version="1.0" encoding="UTF-8"?>
    <PART_STOCK>
        <TIME_STAMP>2020-11-21 00:35:34</TIME_STAMP>
        <VN>300223</VN>
        <PART_STOCK_LINE_LIST>
            <PART_STOCK_LINE_TEMPL>
                <LINE_NO>1</LINE_NO>
                <ARTICLE>COGENT1</ARTICLE>
                <UPC>2000001720875</UPC>
                <NAME>Матрас</NAME>
                <QTY>12</QTY>
            </PART_STOCK_LINE_TEMPL>
    */