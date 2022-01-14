package com.severtrans.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;
import com.severtrans.notification.model.Customer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import com.severtrans.notification.dto.Confirmation;
import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.model.MonitorLog;
import com.severtrans.notification.repository.CustomerDao;
import com.severtrans.notification.repository.MonitorLogDao;
import com.severtrans.notification.utils.XmlUtiles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import lombok.extern.log4j.Log4j2;

@Log4j2
@DataJdbcTest
public class MonitorLogTest {
    @Autowired
    NamedParameterJdbcTemplate npJdbcTemplate;

    @Autowired
    MonitorLogDao logDao;
    @Autowired
    CustomerDao customerDao;

    static String xml_1, xml_5, xml_5_err;
    static Shell shell;

    @BeforeAll
    public static void init() throws FileNotFoundException, IOException, JAXBException {
        try (InputStream is = new FileInputStream("src\\test\\resources\\files\\TEST_APP.xml")) {
            xml_1 = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (InputStream is = new FileInputStream("src\\test\\resources\\files\\TEST_SKU.xml")) {
            xml_5 = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (InputStream is = new FileInputStream("src\\test\\resources\\files\\TEST_SKU_ERR.xml")) {
            xml_5_err = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        shell = XmlUtiles.unmarshaller(xml_5, Shell.class);
    }

    @Test
    void saveTest() throws Exception {
        MonitorLog mlog = new MonitorLog();
        // mlog = new MonitorLog(0L, "89f81f05-9d1e-4319-9b9d-b6f4e34c7e77", "P", 0,
        //         "IN_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185, "my info");

        mlog.setMsg(xml_1);
        logDao.save(mlog);
        // assertTrue(mlog.isNotNull());
        // assertThat(mlog.getId()).isNotNull();
        
        // List<MonitorLog> co = logDao.findIncompleted();
        // assertTrue(co.size() > 0, "record have not saved");

        // region работа с заказчиком
        Customer customer = customerDao.findById("0102304213").orElse(null);
        customer = customerDao.findByClientId(300185).orElse(null);
        boolean isPresent = customerDao.findById("'00-07087898'").isPresent();

        // endregion // ArrayList<FTPFile> list = new

        // ArrayList(Arrays.asList(listFiles));
        // FTPFile james = list.stream()
        // .filter(jame -> "TEST".equals(jame.getName().split("_")[0].toUpperCase()))
        // .findAny().orElse(null);

    }

    @Test
    public void confirm() {// TODO JaxB exception!!!
        MonitorLog mlog = new MonitorLog();
        // region experiment DataAccessException
        // try {
        // mlog = new MonitorLog(0L, "89f81f05-9d1e-4319-9b9d-b6f4e34c7e77", "test", 1,
        // "IN_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185,
        // "my info");

        // logDao.save(mlog);
        // throw new SQLException();
        // } catch (Exception e) {// DataAccessException
        // log.error("Fuck", e);
        // }
        // endregion

        // region init
        mlog = new MonitorLog();
        // mlog = new MonitorLog(0L, "89f81f05-9d1e-4319-9b9d-b6f4e34c7e77", "E", 1,
        //         "IN_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185, "my info");
        // // xml_1="11" + xml_1; JAXBException
        // mlog.setMsg(xml_1);
        // logDao.save(mlog);

        // mlog = new MonitorLog(0L, "89f81f05", "P", 5,
        //         "SKU_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185, "my info");

        // mlog.setMsg(xml_5);
        // logDao.save(mlog);

        // mlog = new MonitorLog(0L, "89f81f05", "X", 5,
        //         "SKU_300185_01-10-2021-15-50-10.xml", new Date(), null, "FAKE", 300185, "my info");
        // mlog.setMsg(xml_5);
        // logDao.save(mlog);

        // endregion

        // List<MonitorLog> logs = logDao.findIncompleted();
        Confirmation confirmation;

        // region SKU
        boolean artNotFound = false;
        SqlParameterSource params;
        // отфильтровываем SKU
        // List<MonitorLog> mls = logs.stream().filter(s -> s.getMsgType() == 5).collect(Collectors.toList());
        String sql = "SELECT COUNT(*) FROM sku WHERE sku_id=:art";
        // for (MonitorLog skuLog : mls) {
        //     try {
        //         shell = XmlUtiles.unmarshallShell(skuLog.getMsg());
        //     } catch (JAXBException e) {
        //         log.error("Не может быть", e);
        //         break;
        //     } // TODO + validation
        //     log.info("skuLog "+shell.getMsgID());
        //     for (SKU skuItem : shell.getSkuList().getSku()) {
        //         log.info("skuItem "+shell.getMsgID());
        //         Customer customer = customerDao.findByClientId(300185).orElse(null);
        //         params = new MapSqlParameterSource().addValue("art", customer.getPrefix() + skuItem.getArticle());
        //         if (npJdbcTemplate.queryForObject(sql, params, Integer.class) == 0) {
        //             artNotFound = true;
        //             break;
        //         }
        //     }
        //     if (!artNotFound) {
        //         confirmation = new Confirmation();
        //         confirmation.setInfo(skuLog.getInfo());
        //         logDao.completeOrder(skuLog.getId(), new Date());
        //         // sendConfirm(skuLog, confirmation);
        //         log.info(shell.getCustomerID() + "\r\nОбработан файл " + skuLog.getFileName());
        //     }
        // }
        if (artNotFound)
            return;
        // endregion

        // region заказы только
        // mls = logs.stream().filter(s -> s.getMsgType() != 5).collect(Collectors.toList());
        // for (MonitorLog ml : mls) {
        //     confirmation = new Confirmation();
        //     try {
        //         shell = XmlUtiles.unmarshallShell(ml.getMsg());
        //     } catch (JAXBException e) {
        //         log.error("Не может быть", e);
        //         break;
        //     }

        //     // if (shell.getMsgType() == 1 || shell.getMsgType() == 2)
        //     if (ml.getStatus() == "E") {
        //         confirmation.setStatus("ERROR");
        //     } else if (ml.getStatus() == "S") {
        //         confirmation.setStatus("SUCCESS");
        //     }
        //     confirmation.setInfo(ml.getInfo());
        //     confirmation.setOrderNo(shell.getOrder().getOrderNo());
        //     confirmation.setGuid(shell.getOrder().getGuid());
        //     // sendConfirm(ml, confirmation);
        //     logDao.completeOrder(ml.getId(), new Date());
        //     log.info(shell.getCustomerID() + "\r\nОбработан файл " + ml.getFileName());
        // }
        // // endregion

        List<MonitorLog> list = (List<MonitorLog>) logDao.findAll();
        log.info("Pause");

    }
}
