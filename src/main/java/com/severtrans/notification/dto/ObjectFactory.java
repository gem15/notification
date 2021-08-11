//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2021.08.11 at 07:49:23 AM MSK 
//


package com.severtrans.notification.dto;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.severtrans.notification.dto package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Shell_QNAME = new QName("http://www.severtrans.com", "Shell");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.severtrans.notification.dto
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Shell }
     * 
     */
    public Shell createShell() {
        return new Shell();
    }

    /**
     * Create an instance of {@link Order }
     * 
     */
    public Order createOrder() {
        return new Order();
    }

    /**
     * Create an instance of {@link UpcList }
     * 
     */
    public UpcList createUpcList() {
        return new UpcList();
    }

    /**
     * Create an instance of {@link DeliveryNotifLine }
     * 
     */
    public DeliveryNotifLine createDeliveryNotifLine() {
        return new DeliveryNotifLine();
    }

    /**
     * Create an instance of {@link PartStockRs }
     * 
     */
    public PartStockRs createPartStockRs() {
        return new PartStockRs();
    }

    /**
     * Create an instance of {@link PickNotifLine }
     * 
     */
    public PickNotifLine createPickNotifLine() {
        return new PickNotifLine();
    }

    /**
     * Create an instance of {@link PartStockRq }
     * 
     */
    public PartStockRq createPartStockRq() {
        return new PartStockRq();
    }

    /**
     * Create an instance of {@link ListSKU }
     * 
     */
    public ListSKU createListSKU() {
        return new ListSKU();
    }

    /**
     * Create an instance of {@link ShipmentNotifLine }
     * 
     */
    public ShipmentNotifLine createShipmentNotifLine() {
        return new ShipmentNotifLine();
    }

    /**
     * Create an instance of {@link PartStockLine }
     * 
     */
    public PartStockLine createPartStockLine() {
        return new PartStockLine();
    }

    /**
     * Create an instance of {@link PickNotif }
     * 
     */
    public PickNotif createPickNotif() {
        return new PickNotif();
    }

    /**
     * Create an instance of {@link OrderLine }
     * 
     */
    public OrderLine createOrderLine() {
        return new OrderLine();
    }

    /**
     * Create an instance of {@link ShipmentNotif }
     * 
     */
    public ShipmentNotif createShipmentNotif() {
        return new ShipmentNotif();
    }

    /**
     * Create an instance of {@link SKU }
     * 
     */
    public SKU createSKU() {
        return new SKU();
    }

    /**
     * Create an instance of {@link DeliveryNotif }
     * 
     */
    public DeliveryNotif createDeliveryNotif() {
        return new DeliveryNotif();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Shell }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.severtrans.com", name = "Shell")
    public JAXBElement<Shell> createShell(Shell value) {
        return new JAXBElement<Shell>(_Shell_QNAME, Shell.class, null, value);
    }

}
