package com.severtrans.notification;

import com.severtrans.notification.dto.Ftp;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.dto.ResponseFtp;
import com.severtrans.notification.service.NotificationException;
import com.severtrans.notification.service.NotificationType;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.transform.OutputKeys;
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
    private FTPClient ftpClient = new FTPClient();
    InputStream is;

    public SendNotifications() {
    }

    @Transactional
    @Scheduled(fixedDelay = Long.MAX_VALUE) //initialDelay = 1000 * 30,
//    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void send() throws IOException, NotificationException {

        //region List<Ftp> ftps = jdbcTemplate.query
        List<Ftp> ftps = jdbcTemplate.query("select * from ftp",
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
// open FTP
            ftpClient.connect(ftp.getHostname(), ftp.getPort());
            ftpClient.enterLocalPassiveMode();
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                log.error("Не удалось подключиться к FTP. Ошибка " + reply);
            } else {
                if (!ftpClient.login(ftp.getLogin(), ftp.getPassword())) {
                    reply = ftpClient.getReplyCode();
                    log.error("Не удалось авторизоваться на FTP.  Ошибка " + reply);
                    if (ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } else {
                    MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftp.getId());
                    //region List<ResponseFtp> responses = namedParameterJdbcTemplate.query
                    List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                            "select Vn,path,Query_Text query,Alias_Text alias from response_ftp r\n" +
                                    "inner join response_extra e on r.Response_Extra_id = e.Id\n" +
                                    "where r.ftp_id = :id",
                            ftpParam,
                            (rs, rowNum) -> new ResponseFtp(
                                    rs.getInt("vn"),
                                    rs.getString("path"),
                                    rs.getString("query"),
                                    rs.getString("alias")
                            )
                    );
                    //endregion
//TODO use path !!! resp.getPath()
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
*/
/*
                    case E4111: 'KB_USL60189' --4111
                        sqlHeader = "select * from notif where id_klient='0'";
                        break;
*//*
                        }
*/
                        //TODO add :id to query text
                        MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id", resp.getVn());
                        List<Notification> list = namedParameterJdbcTemplate.query(resp.getQuery(), new NotificationRowMapper());
                        for (Notification not : list) {
                            String sqlItems = "select row_number() over (),det.* from notifdet det where iddu =:id";
                            int lineNo = 0;

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

                            // xStream
                            StaxDriver driver = new StaxDriver();
                            driver.getOutputFactory().setProperty("escapeCharacters", false);
                            XStream xs = new XStream(driver);
                            xs.omitField(Notification.class, "du");
                            xs.alias(resp.getAlias(), Notification.class);
                            xs.alias("Goods", NotificationItem.class);
                            xs.addImplicitCollection(Notification.class, "Goods");

//                            System.out.println(xs.toXML(not));

//                            BufferedOutputStream stdout = new BufferedOutputStream(System.out);
//                            xs.marshal(not, new PrettyPrintWriter(new OutputStreamWriter(stdout)));

//                            System.out.println(xs.toXML(not));

                            try (Writer writer = new StringWriter()) {
//                                writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                                xs.toXML(not, writer);//        Notification notification = (Notification) xs.fromXML(xml);
//                                xs.marshal(not, new PrettyPrintWriter(writer));
//                                xs.toXML(not, new PrettyPrintWriter(writer));//        Notification notification = (Notification) xs.fromXML(xml);
                System.out.println(writer.toString());
                                is = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
                            }
                            //TODO имя файла
                    /*
    SELECT SV_UTILITIES.FORM_KEY(SEQ_KB_XPEL_OUT.NextVal) INTO v_id FROM dual;
    SELECT p_direction || '_' || LPAD(TO_CHAR(v_id), 11, '0') || '_' || to_char(SYSDATE, 'DDMMYYYY') || '.xml'
      INTO v_file_name
      FROM DUAL;
                     */
                            boolean ok=ftpClient.storeFile("test.xml", is);// boolean done =
                            is.close();
                            if (ok) {
                            //TODO insert into sost
                                //TODO log.info("The second file is uploaded successfully.");
                                log.info("The second file is uploaded successfully.");
                             }
                        }
                    }
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            }
        }
    }
}
