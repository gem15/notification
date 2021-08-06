package com.severtrans.notification.service;

import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.model.Customer;
import com.severtrans.notification.model.CustomerRowMapper;
import com.severtrans.notification.model.Unit;
import com.severtrans.notification.utils.XmlUtiles;
import org.junit.jupiter.api.Test;
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
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.test.context.jdbc.Sql;

import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureTestDatabase
@JdbcTest
//@Sql({"/schema.sql"})//, "/data.sql" https://docs.spring.io/spring-boot/docs/2.1.18.RELEASE/reference/html/howto-database-initialization.html#howto-initialize-a-database-using-spring-jdbc
class UnitTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void UnitTest() {
        String ts = "SKU_2021-08-03-01-20-17.xml".split("_")[0];
        ts = "Загружено записей:";
        System.out.println(ts.split(" ")[0].equals("Загружено"));
        System.out.println(ts);

        String sql = "SELECT h.val_id id,h.val_short code ,h.val_full name FROM sv_hvoc h WHERE h.voc_id = 'KB_MEA'";
        List<Unit> units = jdbcTemplate.query(sql, new BeanPropertyRowMapper(Unit.class));
//        Unit un = units.stream().filter(unit -> "шт".toUpperCase().equals(unit.getCode().toUpperCase())).findAny().orElse(null);
//FIXME src\test\resources\data.sql
// Path fileName = Path.of("./files/SKU_2021-08-03-01-20-17.xml");

        String xml = "";
        InputStream is = null;
        Shell shell = new Shell();
        try {
//                Path fileName = new File(getClass().getResource("/files/SKU_2021-08-03-01-20-17.xml").getFile()).toPath();
//                 Path path = Paths.get("src/test/resources/files/SKU_2021-08-03-01-20-17.xml");//.toURI().toString());//Path.of("./files/SKU_2021-08-03-01-20-17.xml");
            // Files.writeString(fileName, content);
            is = new FileInputStream("src/test/resources/files/SKU_2021-08-03-01-20-17.xml");
            xml = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//                xml = Files.readAllLines(path);
            shell = XmlUtiles.unmarshaller(xml, Shell.class);
        } catch (IOException | NullPointerException | JAXBException e1) { //URISyntaxException |
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

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
/*
SELECT z.id, z.prf_wms, z.id_usr
        INTO l_id_zak, v_prf_wms, v_id_usr
        FROM kb_zak z
       WHERE z.id_klient = vn
             AND z.id_usr IN ('KB_USR92734', 'KB_USR99992');
* */
        Customer customer = jdbcTemplate.queryForObject(
                "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT,PRF_WMS FROM kb_zak WHERE "
                        + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                new CustomerRowMapper(), shell.getCustomerID());

        // передача в солво
        SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_PACK")
                .withProcedureName("WMS3_UPDT_SKU");
/*
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("l_id_zak", customer.getId())
                .addValue("v_prf_wms",customer.getPrefix());
        //kb_pack.wms3_updt_sku(l_id_zak, v_prf_wms, p_err);
*/
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