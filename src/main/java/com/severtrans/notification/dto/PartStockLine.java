package com.severtrans.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonPropertyOrder({"lineNo","article","upc","name","qty"})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PartStockLine {
    @JsonProperty("LINE_NO") int lineNo;
    @JsonProperty("ARTICLE") String article;
    @JsonProperty("UPC") String upc;
    @JsonProperty("NAME") String name;
    @JsonProperty("QTY") int qty;
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
