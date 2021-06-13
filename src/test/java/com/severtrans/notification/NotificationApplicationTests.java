package com.severtrans.notification;

import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.thoughtworks.xstream.XStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;

@JdbcTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
//@SpringBootTest
class NotificationApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

//    @Sql({"/data.sql"})
    @Test
    void SqlTest() {
        System.out.println("test");
    }

    @Test
    void printXMLTest() {
        String _4102 = "IssueReceiptForGoods";
        String _4104 = "IssueOrderForGoods";
        String _4111 = "PickOrderForGoods";


        Notification not = new Notification("Отгрузка", "Отгрузка");

/*
        //TODO experiment
        System.out.println(new Date());
        Date ldt =  new Date();
        not.setLdt(ldt);
        SimpleDateFormat dateFormat_ = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        System.out.println("date: " + dateFormat_.format( new Date() ) );
*/

        not.setDu("1212122"); //omitted field
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
        not.setVN(300227);
        not.setNumberCar("");
        not.setDriver("");
        not.setGoods(new ArrayList<>());

        NotificationItem i=new NotificationItem();
//        i.setLineNumber(1);
        i.setArticle("HLGAD 161010");
        i.setName("Tesla крепление");
        i.setExpirationDate(new Date());
        i.setProductionDate(new Date());
//        i.setLot(); TODO check me
        i.setSerialNum("777");
        i.setMarker("");
//        i.setMarker2(""); TODO check me
        i.setMarker3("");
        i.setCount(5);
        i.setComment("My comment");
        ArrayList<NotificationItem> items =new ArrayList<>();
        items.add(i);
        not.setGoods(items);

        XStream xs = new XStream();

/*
        String dateFormat = "dd.MM.yyyy HH:mm:ss";
//        String timeFormat = "HH:mm:ss";
        String[] acceptableFormats = {dateFormat};
        xs.registerConverter(new DateConverter(dateFormat,acceptableFormats));
*/

/*
        String[] formats ={"yyyy-MM-dd HH:mm"};
        xs.registerConverter(new DateConverter("yyyy-MM-dd HH:mm", formats));
*/


        xs.omitField(Notification.class,"du");
//        XStream.setupDefaultSecurity(xs);
/*
        XStream xstream = new XStream();
        xstream.alias("comments", Comments.class);
        xstream.alias("comment", Comment.class);
        xstream.addImplicitCollection(Comments.class, "comments");
        Comments comments = (Comments)xstream.fromXML(xml);
*/

        xs.alias(_4102, Notification.class); //IssueReceiptForGoods
        xs.alias("Goods",NotificationItem.class);
        xs.addImplicitCollection(Notification.class,"Goods");
//        xs.aliasField("Goods",NotificationItem.class,"Goods");
        System.out.println(xs.toXML(not));

        String xml = xs.toXML(not);
//        Notification notification = (Notification) xs.fromXML(xml);
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
