//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.12.25 at 09:36:24 PM MSK 
//


package com.severtrans.notification.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * <p>Java class for Shell complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Shell">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence minOccurs="0">
 *         &lt;element name="customerID" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="msgID" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="msgType" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="ts" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/>
 *         &lt;element name="confirmation" type="{http://www.severtrans.com}Confirmation" minOccurs="0"/>
 *         &lt;element name="notification" type="{http://www.severtrans.com}Notification" minOccurs="0"/>
 *         &lt;element name="skuList" type="{http://www.severtrans.com}ListSKU" minOccurs="0"/>
 *         &lt;element name="partStockRq" type="{http://www.severtrans.com}PartStockRq" minOccurs="0"/>
 *         &lt;element name="partStockRs" type="{http://www.severtrans.com}PartStockRs" minOccurs="0"/>
 *         &lt;element name="order" type="{http://www.severtrans.com}Order" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Shell", propOrder = {
    "customerID",
    "msgID",
    "msgType",
    "ts",
    "confirmation",
    "notification",
    "skuList",
    "partStockRq",
    "partStockRs",
    "order"
})
public class Shell {

    protected Integer customerID;
    protected String msgID;
    protected Integer msgType;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar ts;
    protected Confirmation confirmation;
    protected Notification notification;
    protected ListSKU skuList;
    protected PartStockRq partStockRq;
    protected PartStockRs partStockRs;
    protected Order order;

    /**
     * Gets the value of the customerID property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getCustomerID() {
        return customerID;
    }

    /**
     * Sets the value of the customerID property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setCustomerID(Integer value) {
        this.customerID = value;
    }

    /**
     * Gets the value of the msgID property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMsgID() {
        return msgID;
    }

    /**
     * Sets the value of the msgID property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMsgID(String value) {
        this.msgID = value;
    }

    /**
     * Gets the value of the msgType property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getMsgType() {
        return msgType;
    }

    /**
     * Sets the value of the msgType property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setMsgType(Integer value) {
        this.msgType = value;
    }

    /**
     * Gets the value of the ts property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTs() {
        return ts;
    }

    /**
     * Sets the value of the ts property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTs(XMLGregorianCalendar value) {
        this.ts = value;
    }

    /**
     * Gets the value of the confirmation property.
     * 
     * @return
     *     possible object is
     *     {@link Confirmation }
     *     
     */
    public Confirmation getConfirmation() {
        return confirmation;
    }

    /**
     * Sets the value of the confirmation property.
     * 
     * @param value
     *     allowed object is
     *     {@link Confirmation }
     *     
     */
    public void setConfirmation(Confirmation value) {
        this.confirmation = value;
    }

    /**
     * Gets the value of the notification property.
     * 
     * @return
     *     possible object is
     *     {@link Notification }
     *     
     */
    public Notification getNotification() {
        return notification;
    }

    /**
     * Sets the value of the notification property.
     * 
     * @param value
     *     allowed object is
     *     {@link Notification }
     *     
     */
    public void setNotification(Notification value) {
        this.notification = value;
    }

    /**
     * Gets the value of the skuList property.
     * 
     * @return
     *     possible object is
     *     {@link ListSKU }
     *     
     */
    public ListSKU getSkuList() {
        return skuList;
    }

    /**
     * Sets the value of the skuList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ListSKU }
     *     
     */
    public void setSkuList(ListSKU value) {
        this.skuList = value;
    }

    /**
     * Gets the value of the partStockRq property.
     * 
     * @return
     *     possible object is
     *     {@link PartStockRq }
     *     
     */
    public PartStockRq getPartStockRq() {
        return partStockRq;
    }

    /**
     * Sets the value of the partStockRq property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartStockRq }
     *     
     */
    public void setPartStockRq(PartStockRq value) {
        this.partStockRq = value;
    }

    /**
     * Gets the value of the partStockRs property.
     * 
     * @return
     *     possible object is
     *     {@link PartStockRs }
     *     
     */
    public PartStockRs getPartStockRs() {
        return partStockRs;
    }

    /**
     * Sets the value of the partStockRs property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartStockRs }
     *     
     */
    public void setPartStockRs(PartStockRs value) {
        this.partStockRs = value;
    }

    /**
     * Gets the value of the order property.
     * 
     * @return
     *     possible object is
     *     {@link Order }
     *     
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Sets the value of the order property.
     * 
     * @param value
     *     allowed object is
     *     {@link Order }
     *     
     */
    public void setOrder(Order value) {
        this.order = value;
    }

}
