package com.severtrans.notification.dto;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

/**
 * Список товаров
 */
@Data
@JsonPropertyOrder({"LineNumber","Article","Name","ExpirationDate","ProductionDate","Lot","SerialNum","Marker","Marker2","Marker3","Count","Comment"})
public class NotificationItem {
    @JsonProperty("LineNumber") int LineNumber;// номер по порядку вставить в запрос rownum
    @JsonProperty("Article") String Article;
    @JsonProperty("Name") String Name;//наименование
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy hh:mm:ss")
    @JsonProperty("ExpirationDate") Date ExpirationDate;//-дата окончания срока годности
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy hh:mm:ss")
    @JsonProperty("ProductionDate") Date ProductionDate;// дата производства
    @JsonProperty("Lot") String Lot="";// партия
    @JsonProperty("SerialNum") String SerialNum;// серийный номер
    @JsonProperty("Marker") String Marker = "-";
    @JsonProperty("Marker2") String Marker2 = "-";
    @JsonProperty("Marker3") String Marker3 = "-";
    @JsonProperty("Count") int Count;// количество
    @JsonProperty("Comment") String Comment="comment";

}
