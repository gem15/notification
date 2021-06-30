package com.severtrans.notification;

import com.severtrans.notification.dto.Ftp;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.dto.ResponseFtp;
import com.severtrans.notification.service.NotificationException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Repository
//@Transactional(readOnly = true) must be placed in a service
public class SendNotifications {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");// HH:mm:ss
    private SimpleDateFormat ts = new SimpleDateFormat("yyyyMMddHHmmss");// HH:mm:ss
    private FTPClient ftpClient = new FTPClient();
    private InputStream is;

    public SendNotifications() {
    }

    @Transactional
    @Scheduled(fixedDelay = Long.MAX_VALUE) //initialDelay = 1000 * 30,
//    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void reply() {
        // int i=jdbcTemplate.queryForObject("select count(*) from kb_sost", Integer.class);
        // log.info(">>>>>>>>>>>>>>>"+i);
        //region List<Ftp> ftps = jdbcTemplate.query
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps",
                (rs, rowNum) -> new Ftp(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password"),
                        rs.getString("hostname"),
                        rs.getInt("port")
                )
        );
        //endregion
        for (Ftp ftp : ftps) {
/*
            MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftp.getId());
            //region List<ResponseFtp> responses = namedParameterJdbcTemplate.query
            List<ResponseFtp> responses = namedParameterJdbcTemplate.query("",
                    ftpParam,
                    (rs, rowNum) -> new ResponseFtp(
                            rs.getString("voc"),
                            rs.getInt("vn"),
                            rs.getString("path")
                    )
            );
            //endregion
*/
            //SimpleJdbcInsert insertActor = new SimpleJdbcInsert(jdbcTemplate);
            try {
                //region open FTP sessionS
                ftpClient.connect(ftp.getHostname(), ftp.getPort());
                ftpClient.enterLocalPassiveMode();
                if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    throw new NotificationException("Не удалось подключиться к FTP");
                }
                if (!ftpClient.login(ftp.getLogin(), ftp.getPassword())) {
                    throw new NotificationException("Не удалось авторизоваться на FTP");
                }
                //endregion

                MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftp.getId());
                //region List<ResponseFtp> responses = namedParameterJdbcTemplate.query
                List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                        "select Vn,path,Query_Text query,Alias_Text alias,e.direction,e.order_type from response_ftp r\n" +
                                "inner join response_extra e on r.Response_Extra_id = e.Id\n" +
                                "where r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(
                                rs.getInt("vn"),
                                rs.getString("path"),
                                rs.getString("query"),
                                rs.getString("alias"),
                                rs.getString("direction"),
                                rs.getString("order_type")
                        )
                );
                //endregion
                for (ResponseFtp resp : responses) {
/*
                    String sqlHeader = "", alias = "";

                    switch (responseFtp.getVoc()) {
                        case "KB_USL60174": //4102
                            sqlHeader = "select * from notif";
                            alias = "IssueReceiptForGoods";
                            break;
                        case "KB_USL60177"://4104:
                            sqlHeader = "select * from notif";
                            alias = "";
                            break;
                case E4111: 'KB_USL60189' --4111
                    sqlHeader = "select * from notif where id_klient='0'";
                    break;
                    }
*/
                    // Changes working directory
                    if (!ftpClient.changeWorkingDirectory(resp.getPath()))
                        throw new NotificationException("Не удалось сменить директорию");

                    //TODO add VN = :id to query text
                    MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id", resp.getVn());
                    List<Notification> list = namedParameterJdbcTemplate.query(resp.getQuery(), new NotificationRowMapper());
                    for (Notification not : list) {

                        not.setOrderType(resp.getOrderType());
                        not.setTypeOfDelivery(resp.getOrderType());

                        //TODO const query text
                        String sqlItems = "select row_number() over (),det.* from notifdet det where iddu =:id";

                        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id", not.getDu());
                        //region List<NotificationItem> items = namedParameterJdbcTemplate.query
                        List<NotificationItem> items = namedParameterJdbcTemplate.query(sqlItems,
                                mapSqlParameterSource,
                                (rs, rowNum) -> new NotificationItem(
                                        rs.getInt(1),
                                        rs.getString("SKU_ID"),
                                        rs.getString("NAME"),
                                        dateFormat.format(rs.getDate("EXPIRATION_DATE") == null ? new Date() : rs.getTimestamp("EXPIRATION_DATE")),
                                        dateFormat.format(rs.getDate("PRODUCTION_DATE") == null ? new Date() : rs.getTimestamp("PRODUCTION_DATE")),
                                        rs.getString("LOT"),
                                        rs.getString("SERIAL_NUM"),
                                        rs.getString("MARKER"),
                                        rs.getString("MARKER2"),
                                        rs.getString("MARKER3"),
                                        rs.getInt("QTY"),
                                        rs.getString("COMMENTS")
                                )
                        );
                        //endregion
                        not.setGoods(items);

                        //region xStream
                        StaxDriver driver = new StaxDriver();
                        driver.getOutputFactory().setProperty("escapeCharacters", false);
                        XStream xs = new XStream(driver);
                        xs.omitField(Notification.class, "du");
                        xs.omitField(Notification.class, "orderID");
                        xs.alias(resp.getAlias(), Notification.class);
                        xs.alias("Goods", NotificationItem.class);
                        xs.addImplicitCollection(Notification.class, "Goods");
/*
                        System.out.println(xs.toXML(not));

                        BufferedOutputStream stdout = new BufferedOutputStream(System.out);
                        xs.marshal(not, new PrettyPrintWriter(new OutputStreamWriter(stdout)));

                        System.out.println(xs.toXML(not));
*/
                        try (Writer writer = new StringWriter()) {
//                                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                            xs.toXML(not, writer);//        Notification notification = (Notification) xs.fromXML(xml);
//                                xs.marshal(not, new PrettyPrintWriter(writer));
//                                xs.toXML(not, new PrettyPrintWriter(writer));//        Notification notification = (Notification) xs.fromXML(xml);
                            System.out.println(writer.toString());
                            is = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        //endregion

                        // имя файла
//                        String fileName = resp.getDirection() + "_" + ts.format(new Date()) + ".xml";
                        String fileName = resp.getDirection() + "_" + new Date().getTime() + ".xml";
                        boolean ok = ftpClient.storeFile(fileName, is);
                        is.close();
                        if (ok) {
/*
                            Map<String, Object> params = new HashMap<>();
                            params.put("id_obsl", not.getOrderID());
                            params.put("id_sost", "KB_USL99771");
                            params.put("dt_sost", new Date());
                            params.put("dt_sost_end", new Date());
                            params.put("sost_prm", fileName);
*/
                            //добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
                            jdbcTemplate.update("INSERT INTO kb_sost (id,id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?, ?,?)",
                                    "3", not.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
                            log.info("Файл " + fileName + " успешно загружен");
                        } else {
                            throw new NotificationException("Не удалось загрузить " + fileName);
                        }
                    }
                }
                ftpClient.logout();
            } catch (IOException | NotificationException e) {
                int reply = ftpClient.getReplyCode();
                log.error(e.getMessage() + ". Код " + reply);
            } catch (DataAccessException e) {
                log.error(e.getMessage());
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException ioe) {
                        // do nothing
                    }
                }
            }
        }
    }
}
