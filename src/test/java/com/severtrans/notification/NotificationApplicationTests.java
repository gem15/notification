package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;

@SpringBootTest
class NotificationApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void printXMLTest() {
        String _4102 = "IssueReceiptForGoods";
        String _4104 = "IssueOrderForGoods";
        String _4111 = "PickOrderForGoods";


        Notification not = new Notification("Отгрузка", "Отгрузка");
        not.setDate("");
        not.setVehicleFactlArrivalTime("");
        not.setFactDeliveryDate("");
        not.setNumber("");
        not.setCustomer("MyCustomer");
        not.setOrderType("");
        not.setTypeOfDelivery("");
        not.setIDSupplier("");
        not.setNameSupplier("");
        not.setAdressSupplier("");
        not.setVN(0);
        not.setNumberCar("");
        not.setDriver("");
        not.setGoods(new ArrayList<>());

        XStream xs = new XStream();
        xs.alias(_4102, Notification.class);
        System.out.println(xs.toXML(not));

        String xml = xs.toXML(not);
        Notification notification = (Notification) xs.fromXML(xml);
        System.out.println("stop");

    }


    /*
    @Test
    void printXmlTest() {

        String  _4102="IssueReceiptForGoods";
        String _4104="IssueOrderForGoods";
        String _4111="PickOrderForGoods";
*/
/*


        Order order=new Order();
        order.setOrderNo("ТС-00000019");
        order.setDeliveryType("Поставка");
        order.setOrderType("Поставка");
//        order.setOrderDate(new Date());//"14.10.2020"
        XStream xs=new XStream();
        xs.alias(_4102,Order.class);
        xs.aliasField("Date",Order.class,"orderDate");
//        xs.aliasField("VehicleFactlArrivalTime",Order.class,"");
//        xs.aliasField("FactDeliveryDate",Order.class,"");
        xs.aliasField("Number",Order.class,"orderNo");
        xs.aliasField("Customer",Order.class,"customer");
        xs.aliasField("OrderType",Order.class,"orderType");
        xs.aliasField("TypeOfDelivery",Order.class,"deliveryType");
        xs.aliasField("IDSupplier",Order.class,"contractor");
        xs.aliasField("NameSupplier",Order.class,"name");
        xs.aliasField("AdressSupplier",Order.class,"address");
//        xs.aliasField("VN",Order.class,"");
//        xs.aliasField("NumberCar",Order.class,"");
//        xs.aliasField("Driver",Order.class,"");
//        xs.aliasField("",Order.class,"");
//        xs.aliasField("",Order.class,"");
//
//        xs.aliasField("PlannedDeliveryDate",Order.class,"plannedDate");
//        xs.aliasField("Number",Order.class,"orderNo");
//        xs.aliasField("Number",Order.class,"orderNo");
//        xs.aliasField("Number",Order.class,"orderNo");
//        xs.aliasField("Number",Order.class,"orderNo");


        System.out.println(xs.toXML(order));
    }
*/
}
