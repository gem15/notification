//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.12.25 at 09:36:24 PM MSK 
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
 * Заказ на поставку/отгрузку
 * 
 * <p>Java class for Order complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Order">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="guid" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="action" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="orderType" type="{http://www.w3.org/2001/XMLSchema}boolean"/>
 *         &lt;element name="orderKind" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="dopInfConsignee" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="orderNo" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="orderDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="plannedDate" type="{http://www.w3.org/2001/XMLSchema}dateTime"/>
 *         &lt;element name="contrCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="contrName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="contrAddress" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="licencePlate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="driver" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="comment" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="orderLine" type="{http://www.severtrans.com}OrderLine" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Order", propOrder = {
    "guid",
    "action",
    "orderType",
    "orderKind",
    "dopInfConsignee",
    "orderNo",
    "orderDate",
    "plannedDate",
    "contrCode",
    "contrName",
    "contrAddress",
    "licencePlate",
    "driver",
    "comment",
    "orderLine"
})
public class Order {

    protected String guid;
    @XmlElement(defaultValue = "0")
    protected Integer action;
    protected boolean orderType;
    protected String orderKind;
    protected String dopInfConsignee;
    @XmlElement(required = true)
    protected String orderNo;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar orderDate;
    @XmlElement(required = true)
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar plannedDate;
    protected String contrCode;
    protected String contrName;
    protected String contrAddress;
    protected String licencePlate;
    protected String driver;
    protected String comment;
    @XmlElement(required = true)
    protected List<OrderLine> orderLine;

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
     * Gets the value of the action property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getAction() {
        return action;
    }

    /**
     * Sets the value of the action property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setAction(Integer value) {
        this.action = value;
    }

    /**
     * Gets the value of the orderType property.
     * 
     */
    public boolean isOrderType() {
        return orderType;
    }

    /**
     * Sets the value of the orderType property.
     * 
     */
    public void setOrderType(boolean value) {
        this.orderType = value;
    }

    /**
     * Gets the value of the orderKind property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrderKind() {
        return orderKind;
    }

    /**
     * Sets the value of the orderKind property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrderKind(String value) {
        this.orderKind = value;
    }

    /**
     * Gets the value of the dopInfConsignee property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDopInfConsignee() {
        return dopInfConsignee;
    }

    /**
     * Sets the value of the dopInfConsignee property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDopInfConsignee(String value) {
        this.dopInfConsignee = value;
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
     * Gets the value of the contrCode property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getContrCode() {
        return contrCode;
    }

    /**
     * Sets the value of the contrCode property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setContrCode(String value) {
        this.contrCode = value;
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
     * Gets the value of the comment property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getComment() {
        return comment;
    }

    /**
     * Sets the value of the comment property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setComment(String value) {
        this.comment = value;
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
     * {@link OrderLine }
     * 
     * 
     */
    public List<OrderLine> getOrderLine() {
        if (orderLine == null) {
            orderLine = new ArrayList<OrderLine>();
        }
        return this.orderLine;
    }

}
