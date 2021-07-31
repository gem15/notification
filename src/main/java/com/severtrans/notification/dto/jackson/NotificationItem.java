package com.severtrans.notification.dto.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.Date;

/**
 * Список товаров
 */
@Data
@JsonPropertyOrder({"LineNumber","Article","Name","ExpirationDate","ProductionDate","Lot","SerialNum","Marker","Marker2","Marker3","Count","Comment"})
public class NotificationItem {
    @JsonProperty("LineNumber") int LineNumber;// номер по порядку вставить в запрос rownum
    @JsonProperty("Article") String article;
    @JsonProperty("Name") String name;//наименование
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd.MM.yyyy")
    @JsonProperty("ExpirationDate") Date expirationDate;//-дата окончания срока годности
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd.MM.yyyy")
    @JsonProperty("ProductionDate") Date productionDate;// дата производства
    @JsonProperty("Lot") String lot ="";// партия
    @JsonProperty("SerialNum") String sn;// серийный номер
    @JsonProperty("Marker") String marker = "-";
    @JsonProperty("Marker2") String marker2 = "-";
    @JsonProperty("Marker3") String marker3 = "-";
    @JsonProperty("Count") int qty;// количество
    @JsonProperty("Comment") String comment ="comment";

}
