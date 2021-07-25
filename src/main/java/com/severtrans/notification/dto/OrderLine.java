package com.severtrans.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonPropertyOrder({"LineNumber","Article","Name","Marker","Marker2","Marker3","Count","Comment"}) //,"ExpirationDate","ProductionDate","Lot","SerialNum"
@Data
public class OrderLine {

    @JsonProperty("LineNumber")
    int lineNo;
    @JsonProperty("Article")
    String article;
    @JsonProperty("Name")
    String name;
    @JsonProperty("Count")
    int qty;
    @JsonProperty("Category")
    String category;
    @JsonProperty("Marker")
    String mark;
    @JsonProperty("Marker2")
    String mark2;
    @JsonProperty("Marker3")
    String mark3;
    @JsonProperty("Comment")
    String comment;

}
/*
  <Goods>');
  <LineNumber>' || TO_CHAR(i)</LineNumber>');
  <Article>' || substr(rec_dt.sku_id, 4)</Article>');
  <Name>' || rec_dt.name</Name>');
  <ExpirationDate>' || TO_CHAR(rec_dt.expiration_date, 'DDMMYYYY')</ExpirationDate>');
  <ProductionDate>' || TO_CHAR(rec_dt.production_date, 'DDMMYYYY')</ProductionDate>');
  <Lot>' || rec_dt.lot</Lot>');
   <SerialNum>'||rec_dt.serial_num||'</SerialNum>');
  <Marker>' || rec_dt.marker</Marker>');
  <Marker2>' || rec_dt.marker2</Marker2>');
  <Marker3>' || rec_dt.marker3</Marker3>');
  <Count>' || rec_dt.qty</Count>');
  <Comment>' || rec_dt.comments</Comment>');
  </Goods>');

* */