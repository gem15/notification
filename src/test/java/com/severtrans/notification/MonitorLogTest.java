package com.severtrans.notification;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import com.severtrans.notification.dto.Confirmation;
import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.model.MonitorLog;
import com.severtrans.notification.repository.MonitorLogDao;
import com.severtrans.notification.utils.XmlUtiles;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import lombok.extern.log4j.Log4j2;

@Log4j2
@DataJdbcTest
public class MonitorLogTest {
    @Autowired
    NamedParameterJdbcTemplate namedParam;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    MonitorLogDao logDao;

    static String xml_1, xml_5;
    static Shell shell;

    @BeforeAll
    public static void init() throws FileNotFoundException, IOException, JAXBException {
        try (InputStream is = new FileInputStream("src\\test\\resources\\files\\TEST_APP.xml")) {
            xml_1 = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (InputStream is = new FileInputStream("src\\test\\resources\\files\\TEST_SKU.xml")) {
            xml_5 = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        shell = XmlUtiles.unmarshaller(xml_5, Shell.class);
    }

    @Test
    void saveTest() throws Exception {
        MonitorLog mlog = new MonitorLog();
        mlog = new MonitorLog(0L, "89f81f05-9d1e-4319-9b9d-b6f4e34c7e77", "P", 0,
                "IN_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185, "my info");

        mlog.setMsg(xml_1);
        logDao.save(mlog);
        // assertTrue(mlog.isNotNull());
        // assertThat(mlog.getId()).isNotNull();
        List<MonitorLog> co = logDao.findIncompleted();
        assertTrue(co.size() > 0, "record have not saved");
        // ArrayList<FTPFile> list = new ArrayList(Arrays.asList(listFiles));
        // FTPFile james = list.stream()
        // .filter(jame -> "TEST".equals(jame.getName().split("_")[0].toUpperCase()))
        // .findAny().orElse(null);

    }

    @Test
    void sendConfirm() throws Exception {// TODO JaxB exception!!!
        MonitorLog mlog = new MonitorLog();
        mlog = new MonitorLog(0L, "89f81f05-9d1e-4319-9b9d-b6f4e34c7e77", "P", 0,
                "IN_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185, "my info");
        mlog.setMsg(xml_1);
        logDao.save(mlog);
        mlog = new MonitorLog(0L, "89f81f05", "P", 5,
                "SKU_300185_01-10-2021-15-50-10.xml", new Date(), null, "msg here", 300185, "my info");
        mlog.setMsg(xml_5);
        logDao.save(mlog);

        try {
            List<MonitorLog> logs = logDao.findIncompleted();

            // region обработка SKU
            String prefix = "TMK";// TODO get from customer
            boolean flag = false;

            String sql = "SELECT COUNT(*) FROM sku WHERE sku_id=:art"; //
            List<MonitorLog> sku = logs.stream().filter(s -> s.getMsgType() == 5).collect(Collectors.toList());
            for (MonitorLog skuLog : sku) {

                shell = XmlUtiles.unmarshallShell(skuLog.getMsg());// TODO + validation
                List<SKU> skuList = shell.getSkuList().getSku();
                for (SKU skuItem : skuList) {
                    SqlParameterSource params = new MapSqlParameterSource().addValue("art",
                            prefix + skuItem.getArticle());
                    // int i = namedParam.queryForObject(sql, params, Integer.class);// , params
                    if (namedParam.queryForObject(sql, params, Integer.class) == 0) {
                        flag = true;
                        break;
                    }
                }
            }
            if (flag)
                return;

            // endregion

            Confirmation confirmation = new Confirmation();
            for (MonitorLog ml : logs) {
                // shell = XmlUtiles.unmarshallShell(ml.getMsg());// TODO + validation
                switch (shell.getMsgType()) {
                    case 5:
                        System.out.println("check all arts");
                        break;
                    case 1:
                    case 2:
                        System.out.println("orders");
                        if (ml.getStatus() == "E") {
                            confirmation.setStatus("ERROR");
                        } else if (ml.getStatus() == "S") {
                            confirmation.setStatus("SUCCESS");
                        }
                        logDao.completeOrder(ml.getId(), new Date());
                        break;
                }

                // sendConfirm( ml)
            }
        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
        }
        log.info("Pause");

    }
}
