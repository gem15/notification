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
import com.severtrans.notification.service.FTPException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
// @Transactional(readOnly = true) must be placed in a service
public class SendNotifications {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    FTPClient ftp;
    @Autowired
    XmlMapper xmlMapper;//TODO   @Autowired

    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");// HH:mm:ss
    private InputStream is;

    public SendNotifications() {
    }

    // @Scheduled(fixedDelay = Long.MAX_VALUE) // initialDelay = 1000 * 30,
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void reply() {
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps", (rs, rowNum) -> new Ftp(rs.getInt("id"),
                rs.getString("login"), rs.getString("password"), rs.getString("hostname"), rs.getInt("port"), rs.getString("description")));
        for (Ftp ftp : ftps) {
            log.info("START FTP " + ftp.getHostname() + " " + ftp.getDescription());
            try {
                // region open FTP sessionS
                this.ftp.connect(ftp.getHostname(), ftp.getPort());
                this.ftp.enterLocalPassiveMode();
                if (!FTPReply.isPositiveCompletion(this.ftp.getReplyCode())) {
                    throw new FTPException("Не удалось подключиться к FTP");
                }
                if (!this.ftp.login(ftp.getLogin(), ftp.getPassword())) {
                    throw new FTPException("Не удалось авторизоваться на FTP");
                }
                // endregion

                // region List<ResponseFtp> responses = namedParameterJdbcTemplate.query
                MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftp.getId());
                List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                        "SELECT vn, path_in, path_out, e.master, e.details, alias_text alias, e.prefix, e.order_type, t.inout_id"
                                + " FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id"
                                + " INNER JOIN ftps f ON r.ftp_id = f.id"
                                + " INNER JOIN response_type T ON T.ID = e.response_type_id"
                                + " WHERE r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(rs.getInt("vn"), rs.getString("path_in"), rs.getString("path_out"), rs.getString("master"),
                                rs.getString("details"), rs.getString("alias"), rs.getString("prefix"),
                                rs.getString("order_type"),rs.getInt("inout_id")));
                // endregion
                //главный цикл
                for (ResponseFtp resp : responses) {
                    log.info("Processing " + resp.getAlias());//TODO заменить на тип сообщения

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
                        String fileName = resp.getPrefix() + "_" + master.getNumber().replaceAll("\\D+", "") + "_" + new Date().getTime()
                                + ".xml";
                        // Changes working directory
                        if (!this.ftp.changeWorkingDirectory(resp.getPathOut()))
                            throw new FTPException("Не удалось сменить директорию");
                        // передача на FTP
                        is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                        boolean ok = this.ftp.storeFile(fileName, is);
                        is.close();
                        if (ok) {
                            // добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
 /*TODO                            jdbcTemplate.update(
                                    "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                                    master.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
 */
                            log.info("Uploaded " + fileName);
                        } else {
                            throw new FTPException("Не удалось выгрузить " + fileName);
                        }
                    }
                }

                this.ftp.logout();
            } catch (IOException e) {
                //FTP and  XmlMapper
                e.printStackTrace();
            }catch (FTPException e) {
                //проблемы FTP сервера
                log.error(e.getMessage() + ". Код " + this.ftp.getReplyCode());
            } finally {
                if (this.ftp.isConnected()) {
                    try {
                        this.ftp.disconnect();
                    } catch (IOException ioe) {
                        // do nothing
                    }
                }
            }
            log.info("FINISH FTP " + ftp.getHostname() + " " + ftp.getDescription());
        }
    }

    @Transactional
    public void docsIn(InputStream remoteInput, String filePrefix) throws IOException, MonitorException, FTPException {
        Customer customer;
        switch (filePrefix) {
            case "P":  //PART_STOCK
                PartStock stockRq = xmlMapper.readValue(remoteInput, PartStock.class); //  десериализуем (из потока создаём объект)
                //regionПолучить клиента по ВН https://mkyong.com/spring/queryforobject-throws-emptyresultdataaccessexception-when-record-not-found/
//                Customer customer;
                try {
                    customer = jdbcTemplate.queryForObject("SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT FROM kb_zak WHERE " +
                            "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?", new CustomerRowMapper(), stockRq.getClientId());
                } catch (EmptyResultDataAccessException e) {
                    throw new MonitorException("ВН " + stockRq.getClientId() + " не найден");
                }
                //endregion
                // region Получить остатки
                SqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", customer.getHolderID());
                List<PartStockLine> partStockLines = namedParameterJdbcTemplate.query(//TODO пустой набор не ошибка?
                        "SELECT * FROM loads WHERE holder_id = :id", //FIXME поле master
                        ftpParam, (rs, i) -> new PartStockLine(
                                rs.getInt("LINENO"),
                                rs.getString("ARTICLE"),
                                rs.getString("UPC"),
                                rs.getString("NAME"),
                                rs.getInt("QTY")));
                stockRq.setTimeStamp(new Date()); //текущая дата
                stockRq.setStockLines(partStockLines);
                //endregion
                // TODO имя файла PS_VN_TIMESTAMP/ PS from field prefix
                String fileName = "PS_" + customer.getClientId() + "_" + new Date().getTime() + ".xml";
                String xml = xmlMapper.writer().writeValueAsString(stockRq); // сериализация

                // region выгрузка на FTP
                ftp.changeWorkingDirectory("/response"); //FIXME из таблицы
                is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                boolean ok = ftp.storeFile(fileName, is);
                is.close();
                ftp.changeWorkingDirectory("/in"); //FIXME из таблицы вернуть
                if (ok) {
                    //TODO обсудить логику/последовательность транзакции обмен
                    //region Поиск/создание суточного заказа //TODO поле detail ? обсудить HELLMAN_STOCK
                    String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'STOCK' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
                    String dailyOrderId;//String.valueOf(++j);//fixme remove me
                    try {
                        dailyOrderId = jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
                    } catch (EmptyResultDataAccessException e) {
                        //FIXME доделать .withTableName("tab")
                        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros").usingGeneratedKeyColumns("id");
                        MapSqlParameterSource params = new MapSqlParameterSource();
                        params.addValue("dt_zakaz", new Date())
                                .addValue("id_zak", customer.getId())
                                .addValue("id_pok", customer.getId())
                                .addValue("n_gruz", "STOCK")
                                .addValue("usl", "Суточный заказ по пакетам PS");
                        KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
                        dailyOrderId = keyHolder.getKeyAs(String.class);
                    }
                    //endregion

                    // добавляем событие 4301 в заказ Получено входящее сообщение
                    jdbcTemplate.update("INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp)VALUES (?, ?, ?, ?,?,?)",
                            dailyOrderId, new Date(), new Date(), "KB_USL99770", "Получен запрос PART_STOCK", "010277043");
                    // добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
                    jdbcTemplate.update(
                            "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                            dailyOrderId, "KB_USL99771", new Date(), new Date(), fileName);

                    //fixme ftp.deleteFile(file.getName());//TODO удаляем принятый файл
                    log.info("Выгружен " + fileName);
                } else {
                    throw new FTPException("Не удалось выгрузить " + fileName);
                }
                //endregion
                System.out.println(xml);
                System.out.println("stop");

                break;
            case "S?": //SKU
                SKU sku = xmlMapper.readValue(remoteInput, SKU.class);
                //region Получить клиента по ВН
                try {
                    customer = jdbcTemplate.queryForObject("SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT FROM kb_zak WHERE " +
                            "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?", new CustomerRowMapper(), sku.getClientId());
                } catch (EmptyResultDataAccessException e) {
                    throw new FTPException("ВН " + sku.getClientId() + " не найден");
                }
                //endregion
                // region е.и. из справочника
                String uofm = "";
                try {
                    uofm = jdbcTemplate.queryForObject("SELECT val_id  FROM sv_hvoc  " +
                            "WHERE voc_id = 'KB_MEA' AND UPPER(val_short) = UPPER(?)", String.class, sku.getMeasure());
                } catch (EmptyResultDataAccessException e) {
                    uofm = null;
                }
                //endregion
                //region поиск/создание суточного заказа
                String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'SKU' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
                String dailyOrderId;
                try {
                    dailyOrderId = jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
                } catch (EmptyResultDataAccessException e) {
                    //FIXME доделать .withTableName("tab")
                    SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros").usingGeneratedKeyColumns("id");
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("dt_zakaz", new Date())
                            .addValue("id_zak", customer.getId())
                            .addValue("id_pok", customer.getId())
                            .addValue("n_gruz", "SKU")
                            .addValue("usl", "Суточный заказ по пакетам SKU");
                    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
                    dailyOrderId = keyHolder.getKeyAs(String.class);
                }
                //endregion
/*
INSERT INTO KB_T_ARTICLE
(id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov) -- str_mu_code,categ, MARKER)
VALUES
(article, art_name, v_uof, upc, control_date, storage_pos, billing_class);
'/AddingGoods/PRODUCT_LIFE') control_date, -
'/AddingGoods/STORAGE_POS') storage_pos, --
'/AddingGoods/BILLING_CLASS') billing_class

*/

                //TODO txManager.commit(TransactionStatus new );
                jdbcTemplate.update("DELETE FROM KB_T_ARTICLE");
                jdbcTemplate.update("INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov) VALUES (?, ?, ?, ?,?,?,?)",
                        sku.getArticle(), sku.getName(), uofm, sku.getUpc(), sku.getProductLife(), sku.getStoragePos(), sku.getBillingClass());
//TODO UPDATE KB_T_ARTICLE SET COMMENTS = REPLACE(REPLACE(RTRIM(LT --- ???

                // TODO kb_pack.wms3_updt_sku(l_id_zak, v_prf_wms, p_err);
                String p_err=null;
                if (p_err != null && p_err != "Загружено записей:")
                    // добавляем событие 4301 в заказ Получено входящее сообщение
                    jdbcTemplate.update("INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp)VALUES (?, ?, ?, ?,?,?)",
                            dailyOrderId, new Date(), new Date(), "KB_USL99770", "Артикул" + sku.getArticle() + " отправлен в СОХ", "010277043");

                System.out.println(customer.getClientId());
                break;
            case ("I"): {
                System.out.println("I");
                break;
            }
            case ("O"): {
                System.out.println("O");
            }
            break;
            default:
                break;
        }
    }

}
