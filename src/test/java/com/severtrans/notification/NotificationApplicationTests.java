package com.severtrans.notification;

import com.severtrans.notification.dto.jackson.Notification;
import com.severtrans.notification.dto.jackson.NotificationItem;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@SpringBootTest
class NotificationApplicationTests {

    @Test
    void SqlTest() {
        System.out.println("test");
    }

    @Test
    void printXMLTest() throws IOException {
        String _4102 = "IssueReceiptForGoods";
        // String _4104 = "IssueOrderForGoods";
        // String _4111 = "PickOrderForGoods";


        Notification not = new Notification();//"Отгрузка", "Отгрузка"

        not.setDu("1212122"); //omitted field
        // not.setDate("");
        not.setActualArrivalTime(new Date());
        not.setActualDeliveryTime(new Date());
        not.setOrderNo("");
        not.setCustomerName("MyCustomer");
        not.setOrderType("");
        not.setTypeOfDelivery("");
        not.setContrCode("");
        not.setContrName("");
        not.setContrAddress("");
        not.setClientID(300227);
        not.setLicencePlate("");
        not.setDriver("");
        // not.setGoods(new ArrayList<>());

        NotificationItem i=new NotificationItem();
//        i.setLineNumber(1);
        i.setArticle("HLGAD 161010");
        i.setName("Tesla крепление");
//        i.setExpirationDate(new Date());
//        i.setProductionDate(new Date());
//        i.setLot();
        i.setSn("777");
        i.setMark("");
//        i.setMarker2("");
        i.setMark3("");
        i.setQty(5);
        i.setComment("My comment");
        ArrayList<NotificationItem> items =new ArrayList<>();
        items.add(i);
        // not.setGoods(items);

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
