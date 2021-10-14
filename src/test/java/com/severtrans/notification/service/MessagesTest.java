package com.severtrans.notification.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.severtrans.notification.NotificationRowMapper;
import com.severtrans.notification.Utils;
import com.severtrans.notification.dto.DeliveryNotif;
import com.severtrans.notification.dto.DeliveryNotifLine;
import com.severtrans.notification.dto.NotificationLine;
import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.Order;
import com.severtrans.notification.dto.PickNotif;
import com.severtrans.notification.dto.PickNotifLine;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.dto.ShipmentNotif;
import com.severtrans.notification.dto.ShipmentNotifLine;
import com.severtrans.notification.dto.jackson.NotificationItem;
import com.severtrans.notification.dto.jackson.NotificationJack;
import com.severtrans.notification.dto.jackson.OrderJackIn;
import com.severtrans.notification.dto.jackson.OrderJackOut;
import com.severtrans.notification.model.Customer;
import com.severtrans.notification.model.CustomerRowMapper;
import com.severtrans.notification.model.MonitorLog;
import com.severtrans.notification.model.MonitorLogDto;
import com.severtrans.notification.model.NotificationItemRowMapper;
import com.severtrans.notification.model.Unit;
import com.severtrans.notification.utils.CalendarConverter;
import com.severtrans.notification.utils.XmlUtiles;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;

@AutoConfigureTestDatabase
@JdbcTest
// @Sql({"/schema.sql"})//, "/data.sql"
// https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/howto-database-initialization.html#howto-initialize-a-database-using-spring-jdbc
class MessagesTest {
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    XmlMapper xmlMapper;
    @Autowired
    ModelMapper modelMapper;
    
    // @Autowired(required=true)
    // MonitorLogDto logDto;

    @Test
    void monitorLogTest(){
        MonitorLog log = new MonitorLog();
        // "Insert into EXPORT_TABLE (ID,STATUS,MSG_TYPE,FILE_NAME,START_DATE,END_DATE,MSG)"
        // +" values ('89f81f05-9d1e-4319-9b9d-b6f4e34c7e77','R','0','IN_300185_01-10-2021-15-50-10.xml',to_date('14.10.21','DD.MM.RR'),null,"
        // +"<Shell xmlns=http://www.severtrans.com");
        log.setId("89f81f05-9d1e-4319-9b9d-b6f4e34c7e77");
        log.setStatus("R");
        log.setMsgType(0);
        log.setFileName("IN_300185_01-10-2021-15-50-10.xml");
        // MonitorLog log =new MonitorLog();
        jdbcTemplate.update("Insert into MONITOR_LOG (ID,STATUS,MSG_TYPE,FILE_NAME,MSG,VN) values (?,?,?,?,?,?)", log.getId(), log.getStatus(), log.getMsgType(), log.getFileName(),
                log.getMsg(),log.getVn());
        // logDto.save(log);
        System.out.println("stop");
    }

    @Test
    void JaxbExtendTest() throws IOException, JAXBException {
        String xml;
        try (InputStream is = new FileInputStream("src\\test\\resources\\files\\OUT_ZO_000307930_2021-08-18-10-10-58.xml")) {
            xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Shell shell = XmlUtiles.unmarshaller(xml, Shell.class);

        JAXBContext jaxbContext = JAXBContext.newInstance(shell.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // without prolog
     
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // jaxbMarshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, "");
        // JAXBElement<Shell> shellElement = new JAXBElement<>(new QName("ROOT"), Shell.class, shell);
        // jaxbMarshaller.marshal(shellElement, System.out);
         
         JAXBElement<Shell> jaxbElement =  new JAXBElement<>(new QName("http://www.severtrans.com", "Shell"), Shell.class, shell);
        // jaxbMarshaller.marshal(jaxbElement, System.out);
        StringWriter sw = new StringWriter();
   
        jaxbMarshaller.marshal(jaxbElement, sw);
        String result = sw.toString().replaceFirst(" xmlns=\"http://www.severtrans.com\"", "");
        //Marshal the employees list in file
        // jaxbMarshaller.marshal(employees, new File("c:/temp/employees.xml"));
        // String out = result.replaceFirst(" xmlns=\"http://www.severtrans.com\"","");
        System.out.println(result);
    }

    @Test
    void outOrderTest() throws IOException, JAXBException { // test выходных сообщений

        InputStream is = new FileInputStream("src\\test\\resources\\files\\OUT_ZO.xml");
        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        is.close();
        Shell shell = XmlUtiles.unmarshaller(xml, Shell.class);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XmlUtiles.marshaller(shell, outputStream);
        InputStream targetStream = new ByteArrayInputStream(outputStream.toByteArray());
        // System.out.println(targetStream.toString());

        String text = new String(targetStream.readAllBytes());
        System.out.println(text);
        

    }

    @Test
    void OrderTest() throws IOException, JAXBException {
        // InputStream is = new
        // FileInputStream("src\\test\\resources\\files\\IN_PO_MK00-010610_2021-04-18-08-00-59.xml");
        String xml;
        try (InputStream is = new FileInputStream(
                "src\\test\\resources\\files\\OUT_ZO_000307930_2021-08-18-10-10-58.xml")) {
            // InputStream is = new
            // FileInputStream("src\\test\\resources\\files\\OUT_ZO_000307930_2021-06-08-08-10-58.xml");
            xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Shell shell = XmlUtiles.unmarshaller(xml, Shell.class);

        /*
         * PipedInputStream in = new PipedInputStream(); final PipedOutputStream out =
         * new PipedOutputStream(in); new Thread(() -> { try { // write the original
         * OutputStream to the PipedOutputStream // note that in order for the below
         * method to work, you need // to ensure that the data has finished writing to
         * the // ByteArrayOutputStream outputStream.writeTo(out); } catch (IOException
         * e) { // logging and exception handling should go here } finally { if (out !=
         * null) { try { out.close(); } catch (IOException e) { e.printStackTrace(); } }
         * } }).start();
         */

        Order order = shell.getOrder();
        ModelMapper mp = new ModelMapper();
        mp.addConverter(new CalendarConverter());
        /*
         * PropertyMap<Order, OrderJackIn> orderMap = new PropertyMap<Order,
         * OrderJackIn>() { protected void configure() {
         * map().setBillingStreet(source.getBillingAddress().getStreet());
         * map(source.billingAddress.getCity(), destination.billingCity); } });
         * modelMapper.addMappings(orderMap);
         */

        String xml_out;
        if (!order.isOrderType()) {
            OrderJackIn jack = mp.map(order, OrderJackIn.class);
            jack.setOrderType("Поставка");
            jack.setDeliveryType("Поставка");
            xml_out = xmlMapper.writer().withRootName("ReceiptOrderForGoods").writeValueAsString(jack);
        } else {
            OrderJackOut jack = mp.map(order, OrderJackOut.class);
            jack.setClientID(shell.getCustomerID());
            jack.setOrderType("Отгрузка");
            jack.setDeliveryType("Отгрузка");
            xml_out = xmlMapper.writer().withRootName("ExpenditureOrderForGoods").writeValueAsString(jack);
        }

        System.out.println(xml_out);
    }

    @Test
    void notificationTest() throws JAXBException, IOException { // уведомления
        ModelMapper modelMapper = new ModelMapper();
        List<NotificationJack> listMaster = jdbcTemplate.query("SELECT * FROM master", new NotificationRowMapper());
        for (NotificationJack master : listMaster) {
            Shell shell = new Shell();
            shell.setCustomerID(300185);// TODO
            MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id", master.getDu());
            List<NotificationItem> items = namedParameterJdbcTemplate.query("SELECT * FROM detail WHERE iddu = :id",
                    mapSqlParameterSource, new NotificationItemRowMapper());

            int notificationType = 1;
            switch (notificationType) {
                /*
                 * case (1): {//NotificationLine List<NotificationLine> notificationLines =
                 * Utils.mapList(items, NotificationLine.class, mp); Notification notif =
                 * mp.map(master, Notification.class);
                 * notif.setGuid("cf843545-9eb5-11eb-80c0-00155d0c6c19");
                 * 
                 * shell.setDeliveryNotif((DeliveryNotif) notif);
                 * notif.getNotifLines().addAll(notificationLines); }
                 */
                case (1): {
                    List<NotificationLine> deliveryNotifLines = Utils.mapList(items, NotificationLine.class,
                            modelMapper);
                    DeliveryNotif deliveryNotif = modelMapper.map(master, DeliveryNotif.class);
                    deliveryNotif.getLine().addAll(deliveryNotifLines);
                    deliveryNotif.setGuid("cf843545-9eb5-11eb-80c0-00155d0c6c19");
                    shell.setDeliveryNotif(deliveryNotif);
                }
                    break;
                case (2): {
                    List<NotificationLine> shipmentNotifLines = Utils.mapList(items, NotificationLine.class,
                            modelMapper);
                    ShipmentNotif shipmentNotif = modelMapper.map(master, ShipmentNotif.class);
                    shipmentNotif.getLine().addAll(shipmentNotifLines);
                    shipmentNotif.setGuid("cf843545-9eb5-11eb-80c0-00155d0c6c19");
                    shell.setShipmentNotif(shipmentNotif);
                }
                    break;
                case (3): {
                    List<PickNotifLine> pickNotifLines = Utils.mapList(items, PickNotifLine.class, modelMapper);
                    PickNotif pickNotif = modelMapper.map(master, PickNotif.class);
                    pickNotif.getPickLine().addAll(pickNotifLines);
                    pickNotif.setGuid("cf843545-9eb5-11eb-80c0-00155d0c6c19");
                    shell.setPickNotif(pickNotif);
                }
                    break;
            }

            // region сохранение xml
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            XmlUtiles.marshaller(shell, outputStream);
            InputStream targetStream = new ByteArrayInputStream(outputStream.toByteArray());
            // endregion

            String xmlText = new String(targetStream.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println(xmlText);

            System.out.println("Zupinka");
        }
    }

    @Test
    void SkuTest() throws IOException, NullPointerException, JAXBException {

        String sql = "SELECT h.val_id id,h.val_short code ,h.val_full name FROM sv_hvoc h WHERE h.voc_id = 'KB_MEA'";
        List<Unit> units = jdbcTemplate.query(sql, new BeanPropertyRowMapper(Unit.class));

        String xml = "";
        Shell shell = new Shell();
        InputStream is = new FileInputStream("src/test/resources/files/SKU_2021-08-03-01-20-17.xml");
        is = new FileInputStream("src\\test\\resources\\files\\IN_PO_MK00-010610_2021-04-18-08-00-59.xml");
        xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        shell = XmlUtiles.unmarshaller(xml, Shell.class);

        // заполнить KB_T_ARTICLE
        ListSKU skus = shell.getSkuList();
        jdbcTemplate.update("DELETE FROM KB_T_ARTICLE");
        String sqlArt = "INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov)\n"
                + "    VALUES (?,?,?, ?, ?, ?, ?)";
        // https://javabydeveloper.com/spring-jdbctemplate-batch-update-with-maxperformance/
        jdbcTemplate.batchUpdate(sqlArt, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SKU sku = skus.getSku().get(i);
                ps.setString(1, sku.getArticle());
                ps.setString(2, sku.getName());
                // шт --> KB_.....
                Unit um = units.stream()
                        .filter(unit -> sku.getUofm().toUpperCase().equals(unit.getCode().toUpperCase())).findAny()
                        .orElse(null);
                ps.setString(3, um == null ? null : um.getId());
                ps.setString(4, sku.getUpc());
                ps.setString(5, String.valueOf(sku.getStorageLife()));
                ps.setString(6, sku.getStorageCondition());
                ps.setString(7, sku.getBillingClass());
            }

            @Override
            public int getBatchSize() {
                return skus.getSku().size();
            }
        });

        // Получить клиента по ВН try catch
        Customer customer = jdbcTemplate.queryForObject(
                "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT,PRF_WMS FROM kb_zak WHERE "
                        + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                new CustomerRowMapper(), shell.getCustomerID());

        // передача в солво
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_PACK")
                .withProcedureName("WMS3_UPDT_SKU");
        Map<String, Object> p_err = jdbcCall.execute(new MapSqlParameterSource().addValue("L_ID_ZAK", customer.getId())
                .addValue("V_PRF_WMS", customer.getPrefix()));
        // orderError = (String) p_err.get("P_ERR");

        // dailyOrder
        // region Поиск/создание суточного заказа
        String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'STOCK' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
        /*
         * SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'SKU' AND trunc(sp.dt_zakaz)
         * = trunc(SYSDATE) AND sp.id_zak = l_id_zak; SqlParameterSource namedParameters
         * = new MapSqlParameterSource("id", id); jdbcTemplate.queryForObject(sql,
         * namedParameters, String.class);
         */
        String dailyOrderId;
        try {
            dailyOrderId = jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
        } catch (EmptyResultDataAccessException e) {
            SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros")
                    .usingGeneratedKeyColumns("id");
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("dt_zakaz", new Date()).addValue("id_zak", customer.getId())
                    .addValue("id_pok", customer.getId()).addValue("n_gruz", "STOCK")
                    .addValue("usl", "Суточный заказ по пакетам PS");
            KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
            dailyOrderId = keyHolder.getKeyAs(String.class);
        }
        // endregion
        // event_4301

        System.out.println(shell.getSkuList());
    }
}