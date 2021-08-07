//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.08.07 at 01:16:23 PM MSK 
//


package com.severtrans.notification.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * Уведомление о подтверждении отгрузки
 * 
 * <p>Java class for DeliveryNotif complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="DeliveryNotif">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="guid" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="orderNo" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="orderDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="plannedDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="contrrCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="contrName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="contrAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="licencePlate" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="driver" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="actualArrivalTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="actualDeliveryTime" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="orderLine" type="{http://www.severtrans.com}DeliveryNotifLine" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "DeliveryNotif", propOrder = {
    "guid",
    "orderNo",
    "orderDate",
    "plannedDate",
    "contrrCode",
    "contrName",
    "contrAddress",
    "licencePlate",
    "driver",
    "actualArrivalTime",
    "actualDeliveryTime",
    "orderLine"
})
public class DeliveryNotif {

    @XmlElement(required = true)
    protected String guid;
    @XmlElement(required = true)
    protected String orderNo;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar orderDate;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar plannedDate;
    protected String contrrCode;
    protected String contrName;
    protected String contrAddress;
    @XmlElement(required = true)
    protected String licencePlate;
    @XmlElement(required = true)
    protected String driver;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar actualArrivalTime;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar actualDeliveryTime;
    @XmlElement(required = true)
    protected List<DeliveryNotifLine> orderLine;

    /**
     * Gets the value of the guid property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getGuid() {
        return guid;
    }

    /**
     * Sets the value of the guid property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setGuid(String value) {
        this.guid = value;
    }

    /**
     * Gets the value of the orderNo property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * Sets the value of the orderNo property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrderNo(String value) {
        this.orderNo = value;
    }

    /**
     * Gets the value of the orderDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getOrderDate() {
        return orderDate;
    }

    /**
     * Sets the value of the orderDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setOrderDate(XMLGregorianCalendar value) {
        this.orderDate = value;
    }

    /**
     * Gets the value of the plannedDate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getPlannedDate() {
        return plannedDate;
    }

    /**
     * Sets the value of the plannedDate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setPlannedDate(XMLGregorianCalendar value) {
        this.plannedDate = value;
    }

    /**
     * Gets the value of the contrrCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContrrCode() {
        return contrrCode;
    }

    /**
     * Sets the value of the contrrCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContrrCode(String value) {
        this.contrrCode = value;
    }

    /**
     * Gets the value of the contrName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContrName() {
        return contrName;
    }

    /**
     * Sets the value of the contrName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContrName(String value) {
        this.contrName = value;
    }

    /**
     * Gets the value of the contrAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContrAddress() {
        return contrAddress;
    }

    /**
     * Sets the value of the contrAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContrAddress(String value) {
        this.contrAddress = value;
    }

    /**
     * Gets the value of the licencePlate property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLicencePlate() {
        return licencePlate;
    }

    /**
     * Sets the value of the licencePlate property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLicencePlate(String value) {
        this.licencePlate = value;
    }

    /**
     * Gets the value of the driver property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDriver() {
        return driver;
    }

    /**
     * Sets the value of the driver property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDriver(String value) {
        this.driver = value;
    }

    /**
     * Gets the value of the actualArrivalTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getActualArrivalTime() {
        return actualArrivalTime;
    }

    /**
     * Sets the value of the actualArrivalTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setActualArrivalTime(XMLGregorianCalendar value) {
        this.actualArrivalTime = value;
    }

    /**
     * Gets the value of the actualDeliveryTime property.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getActualDeliveryTime() {
        return actualDeliveryTime;
    }

    /**
     * Sets the value of the actualDeliveryTime property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setActualDeliveryTime(XMLGregorianCalendar value) {
        this.actualDeliveryTime = value;
    }

    /**
     * Gets the value of the orderLine property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the orderLine property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrderLine().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DeliveryNotifLine }
     * 
     * 
     */
    public List<DeliveryNotifLine> getOrderLine() {
        if (orderLine == null) {
            orderLine = new ArrayList<DeliveryNotifLine>();
        }
        return this.orderLine;
    }

}
