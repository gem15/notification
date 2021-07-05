package com.severtrans.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Список товаров
 */
@Data
@JsonPropertyOrder({"LineNumber","Article","Name","ExpirationDate","ProductionDate","Lot","SerialNum","Marker","Marker2","Marker3","Count","Comment"})
public class NotificationItem {
    @JsonProperty("LineNumber") int LineNumber;// номер по порядку вставить в запрос rownum
    @JsonProperty("Article") String Article;
    @JsonProperty("Name") String Name;//наименование
    @JsonProperty("ExpirationDate") String ExpirationDate;//-дата окончания срока годности
    @JsonProperty("ProductionDate") String ProductionDate;// дата производства
    @JsonProperty("Lot") String Lot="";// партия
    @JsonProperty("SerialNum") String SerialNum;// серийный номер
    @JsonProperty("Marker") String Marker = "-";
    @JsonProperty("Marker2") String Marker2 = "-";
    @JsonProperty("Marker3") String Marker3 = "-";
    @JsonProperty("Count") int Count;// количество
    @JsonProperty("Comment") String Comment="comment";

}
