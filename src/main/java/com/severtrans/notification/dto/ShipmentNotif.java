//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.10.05 at 09:07:03 PM MSK 
//


package com.severtrans.notification.dto;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Уведомление о подтверждении отгрузки
 * 
 * <p>Java class for ShipmentNotif complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ShipmentNotif">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.severtrans.com}Notification">
 *       &lt;sequence>
 *         &lt;element name="line" type="{http://www.severtrans.com}NotificationLine" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShipmentNotif", propOrder = {
    "line"
})
public class ShipmentNotif
    extends Notification
{

    @XmlElement(required = true)
    protected List<NotificationLine> line;

    /**
     * Gets the value of the line property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the line property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getLine().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link NotificationLine }
     * 
     * 
     */
    public List<NotificationLine> getLine() {
        if (line == null) {
            line = new ArrayList<NotificationLine>();
        }
        return this.line;
    }

}
