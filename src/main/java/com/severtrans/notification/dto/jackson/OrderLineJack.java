package com.severtrans.notification.dto.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.Date;

@JsonPropertyOrder({"LineNumber","Article","Name","Category","StorageLife","Marker","Marker2","Marker3","Lot","Count","Comment"}) //,"ExpirationDate","ProductionDate","Lot","SerialNum"
@Data
public class OrderLineJack {

    @JsonProperty("LineNumber")
    int lineNumber;
    @JsonProperty("Article")
    String article;
    @JsonProperty("Name")
    String name;
    @JsonProperty("Count")
    int qty;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy")
    @JsonProperty("StorageLife")
    Date storageLife;
    @JsonProperty("Category")
    int category;
    @JsonProperty("Marker")
    String mark;
    @JsonProperty("Marker2")
    String mark2;
    @JsonProperty("Marker3")
    String mark3;
    @JsonProperty("Lot") String lot ;// партия
    @JsonProperty("Comment")
    String comment;
}
/*
<Goods> - список товаров на отгрузку
        <LineNumber>1</LineNumber> - порядковый номер
        <Article>CUPBCDGGECRE16</Article> - артикул товара
        <Name>CUPBCDGGECRE16 ESPRESSO CREMOSO 16</Name> - наименование
        <Category>0</Category> - категория товара
        <StorageLife/> - срок годности
        <Mark/> 
        <Mark2>ТС-00000011</Mark2>
        <Mark3/>
        <Lot/> - партия
        <Count>18</Count> количество
        <Comment/> комментарий


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