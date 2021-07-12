package com.severtrans.notification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.severtrans.notification.dto.Ftp;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.dto.NotificationItemRowMapper;
import com.severtrans.notification.dto.ResponseFtp;
import com.severtrans.notification.service.NotificationException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
// @Transactional(readOnly = true) must be placed in a service
public class SendNotifications {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");// HH:mm:ss
    private FTPClient ftpClient = new FTPClient();
    private InputStream is;

    public SendNotifications() {
    }

    // @Transactional
    // @Scheduled(fixedDelay = Long.MAX_VALUE) // initialDelay = 1000 * 30,
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void reply() {
        System.out.println(dateFormat.format(0));
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps", (rs, rowNum) -> new Ftp(rs.getInt("id"),
                rs.getString("login"), rs.getString("password"), rs.getString("hostname"), rs.getInt("port"),rs.getString("description")));
        for (Ftp ftp : ftps) {
            log.info("START FTP " + ftp.getHostname()+" "+ftp.getDescription());
            try {
                // region open FTP sessionS
                ftpClient.connect(ftp.getHostname(), ftp.getPort());
                ftpClient.enterLocalPassiveMode();
                if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                    throw new NotificationException("Не удалось подключиться к FTP");
                }
                if (!ftpClient.login(ftp.getLogin(), ftp.getPassword())) {
                    throw new NotificationException("Не удалось авторизоваться на FTP");
                }
                // endregion

                MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftp.getId());
                // region List<ResponseFtp> responses = namedParameterJdbcTemplate.query
                List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                        "SELECT vn, path, e.master, e.details, alias_text alias, e.direction, e.order_type"
                                + " FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id"
                                + " INNER JOIN ftps f ON r.ftp_id = f.id WHERE r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(rs.getInt("vn"), rs.getString("path"), rs.getString("master"),
                                rs.getString("details"), rs.getString("alias"), rs.getString("direction"),
                                rs.getString("order_type")));
                // endregion
                for (ResponseFtp resp : responses) {
                    log.info("Processing " + resp.getAlias());
                    MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id", resp.getVn());
                    List<Notification> listMaster = namedParameterJdbcTemplate.query(resp.getQueryMaster(), queryParam,
                            new NotificationRowMapper());
                    for (Notification master : listMaster) {
                        master.setOrderType(resp.getOrderType());//Отгрузка/Поставка
                        master.setTypeOfDelivery(resp.getOrderType());
                        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id",
                                master.getDu());
                        List<NotificationItem> items = namedParameterJdbcTemplate.query(resp.getQueryDetails(),
                                mapSqlParameterSource, new NotificationItemRowMapper());
                        if (items.size() == 0)
                            continue;
                        master.setItems(items);
                        // to XML
                        XmlMapper xmlMapper = new XmlMapper();
                        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
                        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
                        String xml = xmlMapper.writer().withRootName(resp.getAlias()).writeValueAsString(master);

                        // имя файла
                        String fileName = resp.getDirection() + "_" + master.getNumber().replaceAll("\\D+","") + "_" + new Date().getTime()
                                + ".xml";
                        // Changes working directory
                        if (!ftpClient.changeWorkingDirectory(resp.getPath()))
                            throw new NotificationException("Не удалось сменить директорию");
                        // передача на FTP
                        is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                        boolean ok = ftpClient.storeFile(resp.getDirection() + "_" + master.getNumber().replaceAll("\\D+","") + "_" + new Date().getTime()
                                + ".xml",new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
                        is.close();
                        if (ok) {
                            // добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
 /*TODO                            jdbcTemplate.update(
                                    "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                                    master.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
 */                            log.info("Uploaded " + fileName);
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
            log.info("FINISH FTP "+ftp.getHostname()+" "+ftp.getDescription());
        }
    }
}
