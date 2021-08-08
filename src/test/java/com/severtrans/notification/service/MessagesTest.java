package com.severtrans.notification.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.Order;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.dto.jackson.OrderJackIn;
import com.severtrans.notification.dto.jackson.OrderJackOut;
import com.severtrans.notification.model.Customer;
import com.severtrans.notification.model.CustomerRowMapper;
import com.severtrans.notification.model.Unit;
import com.severtrans.notification.utils.CalendarConverter;
import com.severtrans.notification.utils.XmlUtiles;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;

import javax.xml.bind.JAXBException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@AutoConfigureTestDatabase
@JdbcTest
//@Sql({"/schema.sql"})//, "/data.sql" https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/howto-database-initialization.html#howto-initialize-a-database-using-spring-jdbc
class MessagesTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    XmlMapper xmlMapper;

    @Test
    void OrderTest() throws IOException, JAXBException {
//        InputStream is = new FileInputStream("src\\test\\resources\\files\\IN_PO_MK00-010610_2021-04-18-08-00-59.xml");
        InputStream is = new FileInputStream("src\\test\\resources\\files\\OUT_ZO_000307930_2021-06-08-08-10-58.xml");
        String xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Shell shell = XmlUtiles.unmarshaller(xml, Shell.class);
        Order order = shell.getOrder();
        ModelMapper mp = new ModelMapper();
        mp.addConverter(new CalendarConverter());
/*
        PropertyMap<Order, OrderJackIn> orderMap = new PropertyMap<Order, OrderJackIn>() {
            protected void configure() {
                map().setBillingStreet(source.getBillingAddress().getStreet());
                map(source.billingAddress.getCity(), destination.billingCity);
            }
        });
        modelMapper.addMappings(orderMap);
*/

        String xml_out;
        if (!order.isOrderType()) {
            OrderJackIn jack = mp.map(order, OrderJackIn.class);
            jack.setOrderType("Поставка");
            jack.setDeliveryType("Поставка");
            xml_out = xmlMapper.writer().withRootName("ReceiptOrderForGoods")
                    .writeValueAsString(jack);
        } else {
            OrderJackOut jack = mp.map(order, OrderJackOut.class);
            jack.setClientID(shell.getCustomerID());
            jack.setOrderType("Отгрузка");
            jack.setDeliveryType("Отгрузка");
            xml_out = xmlMapper.writer().withRootName("ExpenditureOrderForGoods")
                    .writeValueAsString(jack);
        }

        System.out.println(xml_out);
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

        //заполнить KB_T_ARTICLE
        ListSKU skus = shell.getSkuList();
        jdbcTemplate.update("DELETE FROM KB_T_ARTICLE");
        String sqlArt = "INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov)\n" +
                "    VALUES (?,?,?, ?, ?, ?, ?)";
        //https://javabydeveloper.com/spring-jdbctemplate-batch-update-with-maxperformance/
        jdbcTemplate.batchUpdate(sqlArt, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SKU sku = skus.getSku().get(i);
                ps.setString(1, sku.getArticle());
                ps.setString(2, sku.getName());
                //шт --> KB_.....
                Unit um = units.stream().filter(unit -> sku.getUofm().toUpperCase().equals(unit.getCode().toUpperCase())).findAny().orElse(null);
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
        //orderError = (String) p_err.get("P_ERR");

        // dailyOrder
        // region Поиск/создание суточного заказа
        String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'STOCK' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
        /*
                                SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'SKU'   AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = l_id_zak;
                                SqlParameterSource namedParameters = new MapSqlParameterSource("id", id);
                                jdbcTemplate.queryForObject(sql, namedParameters, String.class);
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
        //event_4301

        System.out.println(shell.getSkuList());
    }
}