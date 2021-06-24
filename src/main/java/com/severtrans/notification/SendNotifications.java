package com.severtrans.notification;

import com.severtrans.notification.dto.Ftp;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.dto.ResponseFtp;
import com.severtrans.notification.service.NotificationException;
import com.severtrans.notification.service.NotificationType;
import com.thoughtworks.xstream.XStream;
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
    @Scheduled(initialDelay = 1000 * 30, fixedDelay = Long.MAX_VALUE)
//    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public InputStream send(NotificationType type) throws IOException, NotificationException {

        //region List<Ftp> ftps = jdbcTemplate.query
        List<Ftp> ftps = jdbcTemplate.query("select * from ftp",
                (rs, rowNum) -> new Ftp(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("password"),
                        rs.getString("address"),
                        rs.getInt("port")
                )
        );
        //endregion
        for (Ftp ftp : ftps) {
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
// open FTP
            ftpClient.connect(ftp.getHostname(), ftp.getPort());
            ftpClient.enterLocalPassiveMode();
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftpClient.disconnect();
                throw new NotificationException("Не удалось подключиться к FTP. Ошибка " + reply);
            }
            if (!ftpClient.login(ftp.getLogin(), ftp.getPassword())) {
                reply = ftpClient.getReplyCode();
                ftpClient.logout();
                throw new NotificationException("Не удалось авторизоваться на FTP.  Ошибка " + reply);
            }

            for (ResponseFtp responseFtp : responses) {
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
/*
                    case E4111:
                        sqlHeader = "select * from notif where id_klient='0'";
                        break;
*/
                }
                List<Notification> list = namedParameterJdbcTemplate.query(sqlHeader, new NotificationRowMapper());
                for (Notification not : list) {
                    String sqlItems = "select * from notifdet where iddu =:id";
                    int lineNo = 0;

                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id", not.getDu());
                    //region List<NotificationItem> items = namedParameterJdbcTemplate.query
                    List<NotificationItem> items = namedParameterJdbcTemplate.query(sqlItems,
                            mapSqlParameterSource,
                            (rs, rowNum) -> new NotificationItem(
//TODO rownum                            rs.getInt(1),
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
                    // xStream
                    not.setGoods(items);
                    XStream xs = new XStream();
                    xs.omitField(Notification.class, "du");
                    xs.alias(alias, Notification.class);
                    xs.alias("Goods", NotificationItem.class);
                    xs.addImplicitCollection(Notification.class, "Goods");

                    try (Writer writer = new StringWriter()) {
                        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                        xs.toXML(not, writer);//        Notification notification = (Notification) xs.fromXML(xml);
//                System.out.println(writer.toString());
                        is = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
                    }
                    //TODO имя файла
                    ftpClient.storeFile("test.xml", is);// boolean done =
                    is.close();
                    if (ftpClient.completePendingCommand()) {
                        log.info("The second file is uploaded successfully.");
                        //TODO insert into sost
                    }
                }
            }
            ftpClient.logout();
            ftpClient.disconnect();
        }

        return is;
    }
}
