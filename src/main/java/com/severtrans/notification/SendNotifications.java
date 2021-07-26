package com.severtrans.notification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.severtrans.notification.dto.Customer;
import com.severtrans.notification.dto.CustomerRowMapper;
import com.severtrans.notification.dto.Ftp;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.dto.NotificationItemRowMapper;
import com.severtrans.notification.dto.Order;
import com.severtrans.notification.dto.PartStock;
import com.severtrans.notification.dto.PartStockLine;
import com.severtrans.notification.dto.ResponseFtp;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.service.FTPException;
import com.severtrans.notification.service.MonitorException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

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
    XmlMapper xmlMapper;

    private InputStream is;

    public SendNotifications() {
    }

    // @Scheduled(fixedDelay = Long.MAX_VALUE) // initialDelay = 1000 * 30,
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}") //TODO какую задержку и какого типа?
    public void reply() {
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps",
                (rs, rowNum) -> new Ftp(rs.getInt("id"), rs.getString("login"), rs.getString("password"),
                        rs.getString("hostname"), rs.getInt("port"), rs.getString("description")));
        for (Ftp ftpLine : ftps) {// цикл по всем FTP

            if (!ftpLine.getHostname().equals("localhost"))
                continue; // FIXME заглушка для отладки

            log.info("Старт FTP " + ftpLine.getHostname() + " " + ftpLine.getDescription());
            try {
                // region open FTP session
                ftp.connect(ftpLine.getHostname(), ftpLine.getPort());
                ftp.enterLocalPassiveMode();
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    throw new FTPException("Не удалось подключиться к FTP");
                }
                if (!ftp.login(ftpLine.getLogin(), ftpLine.getPassword())) {
                    throw new FTPException("Не удалось авторизоваться на FTP");
                }
                // endregion
                // region List<ResponseFtp> responses = namedParameterJdbcTemplate.query
                MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftpLine.getId());
                List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                        "SELECT vn, path_in, path_out, e.master, e.details, alias_text alias, e.prefix, e.order_type, t.inout_id, f.hostname"
                                + " FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id"
                                + " INNER JOIN ftps f ON r.ftp_id = f.id"
                                + " INNER JOIN response_type T ON T.ID = e.response_type_id" + " WHERE r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(rs.getInt("vn"), rs.getString("path_in"),
                                rs.getString("path_out"), rs.getString("master"), rs.getString("details"),
                                rs.getString("alias"), rs.getString("prefix"), rs.getString("order_type"),
                                rs.getInt("inout_id")));
                // endregion
                // главный цикл
                for (ResponseFtp resp : responses) {
                    log.info("Processing FTP " + resp.getVn());// TODO заменить на тип сообщения ?
                    // отдельно обрабатываем входящие и исходящие сообщения
                    switch (resp.getInOut()) {
                        case (1): { // все входящие сообщения
                            ftp.changeWorkingDirectory(resp.getPathIn());
                            FTPFileFilter filter = ftpFile -> (ftpFile.isFile() && ftpFile.getName().endsWith(".xml"));
                            FTPFile[] listFile = ftp.listFiles(resp.getPathIn(), filter);
                            for (FTPFile file : listFile) {
                                log.info("Processing file "+file.getName());
                                String xmlText;// извлекаем файл в поток и преобразуем в строку
                                try (InputStream remoteInput = ftp.retrieveFileStream(file.getName())) {
                                    xmlText = new String(remoteInput.readAllBytes(), StandardCharsets.UTF_8);
                                }
                                if (!ftp.completePendingCommand()) {
                                    throw new FTPException("Completing Pending Commands Not Successful");
                                }
                                String filePrefix = file.getName().substring(0, 1).toUpperCase();
                                try {
                                    msgIn(xmlText, filePrefix);
                                    ftp.deleteFile(file.getName());// TODO удаляем принятый файл
                                } catch (MonitorException e) { // сообщения с разными ошибками
                                    log.error(e.getMessage());// TODO документ email
                                } catch (DataAccessException e) {// ошибки БД
                                    e.printStackTrace(); // TODO  ошибка доступа
                                }
                            }
                            break;
                        }
                        case (2): { // все исходящие сообщения
                            MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id", resp.getVn());
                            List<Notification> listMaster = namedParameterJdbcTemplate.query(resp.getQueryMaster(),
                                    queryParam, new NotificationRowMapper());
                            for (Notification master : listMaster) {
                                master.setOrderType(resp.getOrderType());// Отгрузка/Поставка
                                master.setTypeOfDelivery(resp.getOrderType());
                                MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id",
                                        master.getDu());
                                List<NotificationItem> items = namedParameterJdbcTemplate.query(resp.getQueryDetails(),
                                        mapSqlParameterSource, new NotificationItemRowMapper());
                                if (items.size() == 0)
                                    continue;
                                master.setItems(items);
                                // to XML
                                String xml = xmlMapper.writer().withRootName(resp.getAlias())
                                        .writeValueAsString(master);

                                // имя файла
                                if(master.getOrderNo() == null)
                                    throw new IOException("Отсутствует номер заказа");//FIXME обсудить
                                String fileName = resp.getPrefix() + "_" + master.getOrderNo().replaceAll("\\D+", "")
                                        + "_" + new Date().getTime() + ".xml";
                                // Changes working directory
                                if (!ftp.changeWorkingDirectory(resp.getPathOut()))
                                    throw new FTPException("Не удалось сменить директорию");
                                // передача на FTP
                                is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                                boolean ok = ftp.storeFile(fileName, is);
                                is.close();
                                if (ok) {
                                    // добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
                                    /*
                                     * TODO jdbcTemplate.update(
                                     * "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)"
                                     * , master.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
                                     */
                                    log.info("Uploaded " + fileName);
                                } else {
                                    throw new FTPException("Не удалось выгрузить " + fileName);
                                }
                            }
                            break;
                        }
                    }
                }

                ftp.logout();
            } catch (IOException e) {
                // FTP and XmlMapper
                e.printStackTrace();
            } catch (FTPException e) {
                // проблемы FTP сервера
                log.error(e.getMessage() + ". Код " + ftp.getReplyCode());
            } finally {
                if (ftp.isConnected()) {
                    try {
                        ftp.disconnect();
                    } catch (IOException ioe) {
                        // do nothing
                    }
                }
            }
            log.info("FINISH FTP " + ftpLine.getHostname() + " " + ftpLine.getDescription());
        }
    }

    @Transactional // для отката при исключениях при работе с ДБ
    public void msgIn(String xmlText, String filePrefix) throws IOException, MonitorException, FTPException {
        Customer customer;
        Map<String, Object> p_err; // возвращаемое из процедуры сообщение
        switch (filePrefix) {
            case "P": // PART_STOCK
                PartStock stockRq = xmlMapper.readValue("dfdf", PartStock.class); // десериализуем (из потока
                                                                                  // создаём объект)
                // regionПолучить клиента по ВН
                // https://mkyong.com/spring/queryforobject-throws-emptyresultdataaccessexception-when-record-not-found/
                try {
                    customer = jdbcTemplate.queryForObject(
                            "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT FROM kb_zak WHERE "
                                    + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                            new CustomerRowMapper(), stockRq.getClientId());
                } catch (EmptyResultDataAccessException e) {
                    throw new MonitorException("ВН " + stockRq.getClientId() + " не найден");
                }
                // endregion
                // region Получить остатки
                SqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", customer.getHolderID());
                List<PartStockLine> partStockLines = namedParameterJdbcTemplate.query(// TODO пустой набор не ошибка?
                        "SELECT * FROM loads WHERE holder_id = :id", // FIXME поле master
                        ftpParam, (rs, i) -> new PartStockLine(rs.getInt("LINENO"), rs.getString("ARTICLE"),
                                rs.getString("UPC"), rs.getString("NAME"), rs.getInt("QTY")));
                stockRq.setTimeStamp(new Date()); // текущая дата
                stockRq.setStockLines(partStockLines);
                // endregion
                // имя файла PS_VN_TIMESTAMP/ PS from field prefix
                String fileName = "PS_" + customer.getClientId() + "_" + new Date().getTime() + ".xml";
                String xml = xmlMapper.writer().writeValueAsString(stockRq); // сериализация

                // region выгрузка на FTP
                ftp.changeWorkingDirectory("/response"); // FIXME из таблицы
                is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                boolean ok = ftp.storeFile(fileName, is);
                is.close();
                ftp.changeWorkingDirectory("/in"); // FIXME из таблицы вернуть
                if (ok) {
                    // region Поиск/создание суточного заказа //TODO поле detail ? обсудить
                    // HELLMAN_STOCK
                    String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'STOCK' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
                    String dailyOrderId;// String.valueOf(++j);//fixme remove me
                    try {
                        dailyOrderId = jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
                    } catch (EmptyResultDataAccessException e) {
                        // FIXME доделать .withTableName("tab")
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

                    // добавляем событие 4301 в заказ Получено входящее сообщение
                    jdbcTemplate.update(
                            "INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp)VALUES (?, ?, ?, ?,?,?)",
                            dailyOrderId, new Date(), new Date(), "KB_USL99770", "Получен запрос PART_STOCK",
                            "010277043");
                    // добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
                    jdbcTemplate.update(
                            "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                            dailyOrderId, "KB_USL99771", new Date(), new Date(), fileName);
                    log.info("Выгружен " + fileName);

                } else {
                    throw new FTPException("Не удалось выгрузить " + fileName);
                }
                // endregion
                System.out.println(xml);
                System.out.println("stop");
                break;
            case "S": // SKU
                // SKU sku = xmlMapper.readValue(xmlText, SKU.class);
                // System.out.println(sku.getClientId());
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("ADD_SKU");
                // MapSqlParameterSource in = new MapSqlParameterSource().addValue("P_MSG",
                //         new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB);
                p_err = jdbcCall.execute(new MapSqlParameterSource().addValue("P_MSG",
                new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB));
                if (p_err.get("P_ERR") != null){
                    Utils.emailAlert((String) p_err.get("P_ERR"));// TODO доработать
                    System.out.println(p_err.get("P_ERR"));
                    throw new MonitorException((String) p_err.get("P_ERR"));// + fileName);
                }
                /*
                 * //region Получить клиента по ВН try { customer = jdbcTemplate.
                 * queryForObject("SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT FROM kb_zak WHERE "
                 * + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?", new
                 * CustomerRowMapper(), sku.getClientId()); } catch
                 * (EmptyResultDataAccessException e) { throw new FTPException("ВН " +
                 * sku.getClientId() + " не найден"); } //endregion // region е.и. из
                 * справочника String uofm = ""; try { uofm =
                 * jdbcTemplate.queryForObject("SELECT val_id  FROM sv_hvoc  " +
                 * "WHERE voc_id = 'KB_MEA' AND UPPER(val_short) = UPPER(?)", String.class,
                 * sku.getMeasure()); } catch (EmptyResultDataAccessException e) { uofm = null;
                 * } //endregion //region поиск/создание суточного заказа String dailyOrderSql =
                 * "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'SKU' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?"
                 * ; String dailyOrderId; try { dailyOrderId =
                 * jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId()); }
                 * catch (EmptyResultDataAccessException e) { //FIXME доделать
                 * .withTableName("tab") SimpleJdbcInsert simpleJdbcInsert = new
                 * SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros").
                 * usingGeneratedKeyColumns("id"); MapSqlParameterSource params = new
                 * MapSqlParameterSource(); params.addValue("dt_zakaz", new Date())
                 * .addValue("id_zak", customer.getId()) .addValue("id_pok", customer.getId())
                 * .addValue("n_gruz", "SKU") .addValue("usl", "Суточный заказ по пакетам SKU");
                 * KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
                 * dailyOrderId = keyHolder.getKeyAs(String.class); } //endregion
                 * 
                 * jdbcTemplate.update("DELETE FROM KB_T_ARTICLE"); jdbcTemplate.
                 * update("INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov) VALUES (?, ?, ?, ?,?,?,?)"
                 * , sku.getArticle(), sku.getName(), uofm, sku.getUpc(), sku.getProductLife(),
                 * sku.getStoragePos(), sku.getBillingClass()); //TODO UPDATE KB_T_ARTICLE SET
                 * COMMENTS = REPLACE(REPLACE(RTRIM(LT --- ???
                 * 
                 * // TODO kb_pack.wms3_updt_sku(l_id_zak, v_prf_wms, p_err); String p_err =
                 * null; if (p_err != null && p_err != "Загружено записей:") // добавляем
                 * событие 4301 в заказ Получено входящее сообщение jdbcTemplate.
                 * update("INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp)VALUES (?, ?, ?, ?,?,?)"
                 * , dailyOrderId, new Date(), new Date(), "KB_USL99770", "Артикул" +
                 * sku.getArticle() + " отправлен в СОХ", "010277043");
                 */
                break;
            case ("I"): {//IN
                // Order orderIn =xmlMapper.readValue(xmlText, Order.class);
                // System.out.println("I");
                SimpleJdbcCall jdbcCall_4101 = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("MSG_4101");
                // MapSqlParameterSource in = new MapSqlParameterSource().addValue("P_MSG",
                //         new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB);
                p_err = jdbcCall_4101.execute(new MapSqlParameterSource().addValue("P_MSG",
                new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB));
                if (p_err.get("P_ERR") != null){
                    Utils.emailAlert((String) p_err.get("P_ERR"));// TODO доработать
                    throw new MonitorException((String) p_err.get("P_ERR"));// + fileName);
                }
                break;
            }
            case ("O"): {
                SimpleJdbcCall jdbcCall_4103 = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("MSG_4103");
                // MapSqlParameterSource in = new MapSqlParameterSource().addValue("P_MSG",
                //         new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB);
                p_err = jdbcCall_4103.execute(new MapSqlParameterSource().addValue("P_MSG",
                new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB));
                if (p_err.get("P_ERR") != null){
                    Utils.emailAlert((String) p_err.get("P_ERR"));// TODO доработать
                    throw new MonitorException((String) p_err.get("P_ERR"));// + fileName);
                }
           }
                break;
            default:
                break;
        }
    }

}
