package com.severtrans.notification.dto.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@JsonPropertyOrder({"CustomerID","article","upc","name","measure","productLife","storagePos","billingClass"})
@JacksonXmlRootElement(localName = "AddingGoods")
@Data
public class SKU {
    /**
     *  ВН
     */
    @JsonProperty("VN") int clientId;
    @JsonProperty("ARTICLE") String article;
    @JsonProperty("UPC") String upc;
    @JsonProperty("NAME") String name;
    /**
     * unit of measure
     */
    @JsonProperty("MEASURE") String measure;
    @JsonProperty("PRODUCT_LIFE") String productLife;
    @JsonProperty("STORAGE_POS") String storagePos;
    @JsonProperty("BILLING_CLASS") String billingClass;

}
