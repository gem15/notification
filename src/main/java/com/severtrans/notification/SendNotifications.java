package com.severtrans.notification;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.ArrayList;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.severtrans.notification.dto.Confirmation;
import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationLine;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.dto.jackson.NotificationItem;
import com.severtrans.notification.dto.jackson.NotificationJack;
import com.severtrans.notification.dto.jackson.PartStock;
import com.severtrans.notification.dto.jackson.PartStockLine;
import com.severtrans.notification.model.Customer;
import com.severtrans.notification.model.CustomerRowMapper;
import com.severtrans.notification.model.Ftp;
import com.severtrans.notification.model.MonitorLog;
import com.severtrans.notification.model.NotificationItemRowMapper;
import com.severtrans.notification.model.ResponseFtp;
import com.severtrans.notification.model.Unit;
import com.severtrans.notification.service.FTPException;
import com.severtrans.notification.service.MonitorException;
import com.severtrans.notification.utils.XmlUtiles;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPReply;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
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
public class SendNotifications {

    /**
     * Поиск суточного заказа LIKE search||'%';
     */
    private static final String DAILY_ORDER_STOCK = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz like '%STOCK' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    FTPClient ftp;
    @Autowired
    XmlMapper xmlMapper;

    @Autowired
    ModelMapper modelMapper;
    /**
     *  FTP root directory
     */
    private String rootDir;
    private Shell shell;
    private boolean ok;

    // @Scheduled(fixedDelay = Long.MAX_VALUE) // initialDelay = 1000 * 30,
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void reply() {
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps",
                (rs, rowNum) -> new Ftp(rs.getInt("id"), rs.getString("login"), rs.getString("password"),
                        rs.getString("hostname"), rs.getInt("port"), rs.getString("description")));
        for (Ftp ftpLine : ftps) {// цикл по всем FTP

            if (ftpLine.getId() != 4)continue; // FIXME заглушка для отладки

            log.info(">>> Старт FTP " + ftpLine.getHostname() + " " + ftpLine.getDescription());
            try {
                // region open FTP session
                ftp.setControlEncoding("UTF-8");
                ftp.setAutodetectUTF8(true);
                ftp.connect(ftpLine.getHostname(), ftpLine.getPort());
                ftp.enterLocalPassiveMode();
                ftp.sendCommand("OPTS UTF8 ON");
                if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                    throw new FTPException("Не удалось подключиться к FTP");
                }
                if (!ftp.login(ftpLine.getLogin(), ftpLine.getPassword())) {
                    throw new FTPException("Не удалось авторизоваться на FTP");
                }
                rootDir = ftp.printWorkingDirectory();
                // endregion
                // region запрос для главного цикла
                MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftpLine.getId());
                List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                        "SELECT vn, path_in, path_out, e.master, e.details, alias_text alias, e.prefix,"
                                + " e.order_type, t.inout_id, f.hostname, e.legacy,t.name as type_name, t.id as type_id"
                                + " FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id"
                                + " INNER JOIN ftps f ON r.ftp_id = f.id"
                                + " INNER JOIN response_type T ON T.ID = e.response_type_id" + " WHERE r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(rs.getInt("vn"), rs.getString("path_in"),
                                rs.getString("path_out"), rs.getString("master"), rs.getString("details"),
                                rs.getString("alias"), rs.getString("prefix"), rs.getString("order_type"),
                                rs.getInt("inout_id"), rs.getString("hostname"), rs.getBoolean("legacy"),
                                rs.getInt("type_id"), rs.getString("type_name")));
                // endregion
                // главный цикл
                for (ResponseFtp resp : responses) {
                    // отдельно обрабатываем входящие и исходящие сообщения
                    if (!resp.isLegacy()) {
                        switch (resp.getInOut()) {
                            case (1): { //входящие
                                ftp.changeWorkingDirectory(rootDir + "IN");
                                FTPFileFilter filter = ftpFile -> (ftpFile.isFile()
                                        && ftpFile.getName().endsWith(".xml"));
                                FTPFile[] listFiles = ftp.listFiles(ftp.printWorkingDirectory(), filter);
                                // ArrayList<FTPFile> list = new ArrayList(Arrays.asList(listFiles));
                                // FTPFile james = list.stream()
                                //         .filter(jame -> "TEST".equals(jame.getName().split("_")[0].toUpperCase()))
                                //         .findAny().orElse(null);

                                for (FTPFile file : listFiles) {
                                    // region  извлекаем файл в поток и преобразуем в строку
                                    String xmlText;
                                    ftp.changeWorkingDirectory(rootDir + "IN");
                                    try (InputStream remoteInput = ftp.retrieveFileStream(file.getName())) {
                                        xmlText = new String(remoteInput.readAllBytes(), StandardCharsets.UTF_8);
                                    }
                                    if (!ftp.completePendingCommand()) {// завершение FTP транзакции
                                        throw new FTPException("Completing Pending Commands Not Successful");
                                    }
                                    // endregion
                                    // region  сохраняем принятый в  папке LOADED
                                    ok = ftp.rename(rootDir + "IN/" + file.getName(),
                                            rootDir + "LOADED/" + file.getName());
                                    if (!ok)
                                        throw new FTPException("Ошибка перемещения файла " + file.getName());
                                    // endregion
                                    try {
                                        shell = XmlUtiles.unmarshaller(xmlText, Shell.class);
                                        prefix2MsgType(file.getName().split("_")[0].toUpperCase());//костыль
                                        msgInNew();
                                        confirm(file.getName());
                                    } catch (MonitorException e) { 
                                        // сообщения с пользовательскими ошибками
                                        confirm(file.getName(),e.getMessage());
                                    } catch (DataAccessException e) {
                                        log.error("Ошибка БД. " + e.getMessage());
                                        //Utils.emailAlert(error);
                                    } catch (JAXBException e) {
                                        log.error("Некорректный формат. " + e.getMessage());
                                        confirm(file.getName(),"Некорректный формат");
                                        e.printStackTrace();
                                    }
                                }
                            }
                                break;
                            case (2): {// NEW все исходящие сообщения (отбивки)
                                MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id",
                                        resp.getVn());
// log.info("\n----- "+resp.getTypeID());
                                List<NotificationJack> listMaster;
                                listMaster = namedParameterJdbcTemplate.query(resp.getQueryMaster(), queryParam,
                                        new NotificationRowMapper());

                                for (NotificationJack master : listMaster) {
                                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                                            .addValue("id", master.getDu());
                                    List<NotificationItem> items = namedParameterJdbcTemplate.query(
                                            resp.getQueryDetails(), mapSqlParameterSource,
                                            new NotificationItemRowMapper());
                                    if (items.size() == 0)
                                        continue;

                                    shell = new Shell();
                                    shell.setCustomerID(resp.getVn());
                                    shell.setMsgID(UUID.randomUUID().toString());
                                    shell.setMsgType(resp.getTypeID());
                                    List<NotificationLine> notificationLine = Utils.mapList(items,
                                    NotificationLine.class, modelMapper);
                                    Notification notification = modelMapper.map(master, Notification.class);
                                    notification.getLine().addAll(notificationLine);
                                    shell.setNotification(notification);
                                    try {// передача на FTP
                                         // region имя файла
                                         // DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd-hh-mm-ss");
                                         // String fileName = resp.getPrefix() + "_" + master.getOrderNo() + "_"
                                         //         + dateFormat.format(new Date()) + ".xml"; //+ master.getOrderNo() + "_" 
                                        String fileName = resp.getPrefix() + "_" + master.getGuid() + ".xml";//TODO ТАЙПИТ
                                        // endregion

                                        /* ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                        XmlUtiles.marshaller(shell, outputStream);
                                        InputStream targetStream = new ByteArrayInputStream(outputStream.toByteArray());
                                        String text = new String(targetStream.readAllBytes());
                                        log.info(text); */

                                        if (!ftp.changeWorkingDirectory(rootDir + resp.getPathOut()))
                                            throw new FTPException("Не удалось сменить директорию");
                                        if (ftp.storeFile(fileName, XmlUtiles.marshaller(shell))) {
                                            // 4302 подтверждение что по данному заказу мы отправили уведомление
                                            jdbcTemplate.update(
                                                    "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                                                    master.getOrderID(), "KB_USL99771", new Date(), new Date(),
                                                    fileName);
                                            log.info(resp.getVn() + " " + resp.getTypeName() + " Выгружен " + fileName);
                                        } else {
                                            throw new FTPException(resp.getVn() + " " + resp.getTypeName()
                                                    + " Не удалось выгрузить " + fileName);
                                        }
                                    } catch (JAXBException e) {
                                        log.error(e.getMessage());
                                        //TODOошибка обработки XML  валидацию ?
                                        //  throw new MonitorException(e.getMessage());
                                        //  e.printStackTrace();
                                    }
                                }
                            }
                                break;
                        }
                    } else {//старый формат
                        switch (resp.getInOut()) {
                            case (1): { // все входящие сообщения
                                ftp.changeWorkingDirectory(rootDir + resp.getPathIn());
                                FTPFileFilter filter = ftpFile -> (ftpFile.isFile()
                                        && ftpFile.getName().endsWith(".xml"));
                                FTPFile[] listFile = ftp.listFiles(resp.getPathIn(), filter);
                                for (FTPFile file : listFile) {
                                    log.info("Processing file " + file.getName());
                                    String xmlText;// извлекаем файл в поток и преобразуем в строку
                                    try (InputStream remoteInput = ftp.retrieveFileStream(file.getName())) {
                                        xmlText = new String(remoteInput.readAllBytes(), StandardCharsets.UTF_8);
                                    }
                                    if (!ftp.completePendingCommand()) {
                                        throw new FTPException("Completing Pending Commands Not Successful");
                                    }
                                    String filePrefix = file.getName().substring(0, 1).toUpperCase();
                                    try {
                                        String error = msgIn(xmlText, filePrefix, resp.getPathOut());
                                        if (error == null || error.isEmpty()) {
                                            Utils.emailAlert(error);// TODO доработать ошибку и файл приатачить
                                            // throw new MonitorException((String) p_err.get("P_ERR"));// + fileName);
                                        }
                                        ftp.deleteFile(file.getName());// TODO удаляем принятый файл с ошибкой
                                        // переименовываем?
                                    } catch (MonitorException e) { // сообщения с разными ошибками
                                        log.error(e.getMessage());// TODO документ email
                                    } catch (DataAccessException e) {
                                        log.error(e.getMessage()); // TODO ошибка доступа // ошибки БД
                                    }
                                }
                                break;
                            }
                            case (2): { // все исходящие сообщения (отбивки)
                                MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id",
                                        resp.getVn());
                                List<NotificationJack> listMaster = namedParameterJdbcTemplate
                                        .query(resp.getQueryMaster(), queryParam, new NotificationRowMapper());
                                for (NotificationJack master : listMaster) {
                                    master.setOrderType(resp.getOrderType());// Отгрузка/Поставка
                                    master.setTypeOfDelivery(resp.getOrderType());
                                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                                            .addValue("id", master.getDu());
                                    List<NotificationItem> items = namedParameterJdbcTemplate.query(
                                            resp.getQueryDetails(), mapSqlParameterSource,
                                            new NotificationItemRowMapper());
                                    if (items.size() == 0)
                                        continue;
                                    master.setOrderLine(items);

                                    // region имя файла
                                    DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd-hh-mm-ss");
                                    String fileName = resp.getPrefix() + "_" + master.getOrderNo() + "_"
                                            + dateFormat.format(new Date()) + ".xml";
                                    // endregion

                                    // Changes working directory
                                    if (!ftp.changeWorkingDirectory(rootDir + resp.getPathOut()))
                                        throw new FTPException("Не удалось сменить директорию");
                                    // to XML
                                    String xml = xmlMapper.writer().withRootName(resp.getAlias())
                                            .writeValueAsString(master);
                                    // передача на FTP
                                    InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                                    boolean ok = ftp.storeFile(fileName, is);
                                    is.close();
                                    if (ok) {
                                        // 4302 подтверждение что по данному заказу мы отправили уведомление
                                        jdbcTemplate.update(
                                                "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                                                master.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
                                        log.info("Выгружен " + fileName);
                                    } else {
                                        throw new FTPException("Не удалось выгрузить " + fileName);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
                ftp.logout();
                log.info("<<< Закончена обработка FTP " + ftpLine.getHostname() + " " + ftpLine.getDescription());
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
        }
    }

    /**
     * Новый формат обработки входящих сообщений
     *
     * @throws IOException
     * @throws MonitorException
     * @throws JAXBException
     * @throws FTPException
     */
    @Transactional //(propagation = Propagation.REQUIRES_NEW)
    public void msgInNew() throws IOException, MonitorException, JAXBException {
        Map<String, Object> p_err; // возвращаемое из процедуры сообщение
        switch (shell.getMsgType()){
            case 5: { //SKU
                // Справочник е.и.
                String sql = "SELECT h.val_id id,h.val_short code ,h.val_full name FROM sv_hvoc h WHERE h.voc_id = 'KB_MEA'";
                List<Unit> units = jdbcTemplate.query(sql, new BeanPropertyRowMapper<Unit>(Unit.class));

                // region заполнить KB_T_ARTICLE
                ListSKU skus = shell.getSkuList();
                jdbcTemplate.update("DELETE FROM KB_T_ARTICLE");
                String sqlArt = "INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov)\n"
                        + "    VALUES (?,?,?, ?,?,?,?)"; //(article, art_name, v_uof, upc, control_date, storage_pos, billing_class);
                //https://javabydeveloper.com/spring-jdbctemplate-batch-update-with-maxperformance/
                jdbcTemplate.batchUpdate(sqlArt, new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        SKU sku = skus.getSku().get(i);
                        ps.setString(1, sku.getArticle());
                        ps.setString(2, sku.getName());
                        //шт --> KB_.....
                        Unit um = units.stream()
                                .filter(unit -> sku.getUofm().toUpperCase().equals(unit.getCode().toUpperCase()))
                                .findAny().orElse(null);
                        ps.setString(3, um == null ? null : um.getId());
                        ps.setString(4, sku.getUpc());
                        ps.setInt(5, sku.getStorageLife() == null ? 0 : sku.getStorageLife());
                        ps.setString(6, sku.getStorageCondition());
                        ps.setString(7, sku.getBillingClass());
                    }

                    @Override
                    public int getBatchSize() {
                        return skus.getSku().size();
                    }
                });
                // List<Map<String, Object>> test = jdbcTemplate.queryForList("select id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov from KB_T_ARTICLE");
                // endregion

                // region Получить клиента по ВН
                Customer customer;
                try {
                     customer = jdbcTemplate.queryForObject(
                            "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT,PRF_WMS FROM kb_zak WHERE "
                                    + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                            new CustomerRowMapper(), shell.getCustomerID());
                } catch (EmptyResultDataAccessException e) {
                    throw new MonitorException("ВН " + shell.getCustomerID() + " не найден", shell.getCustomerID(), 2,
                            null);
                }
                // endregion

                //  region передача в солво
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("WMS3_UPDT_SKU");
                p_err = jdbcCall.execute(new MapSqlParameterSource().addValue("P_ID", customer.getId())
                        .addValue("P_PREF", customer.getPrefix()));
                String scuError = (String) p_err.get("P_ERR");
                if (scuError != null)
                    throw new MonitorException(scuError, shell.getCustomerID(), 2, null);
                // endregion

                // region Поиск/создание суточного заказа
                String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz like '%SKU' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
                String dailyOrderId;
                try {
                    dailyOrderId = jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
                    log.info("Найден суточный заказ");
                } catch (EmptyResultDataAccessException e) {
                    SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros")
                            .usingGeneratedKeyColumns("id");
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("dt_zakaz", new Date()).addValue("id_zak", customer.getId())
                            .addValue("id_pok", customer.getId())
                            .addValue("n_gruz", customer.getCustomerName() +" SKU")// FIXME
                            .addValue("usl", "Суточный заказ по пакетам SKU").addValue("ORA_USER_EDIT_ROW_LOCK", 0);
                    //WTF ORA_USER_EDIT_ROW_LOCK !!!!!!!!!!!!!!!!
                    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
                    dailyOrderId = keyHolder.getKeyAs(String.class);
                    log.info("Создан суточный заказ");
                }
                // событие 4301 в суточный заказ Получено входящее сообщение
                jdbcTemplate.update(
                        "INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp) VALUES (?, ?, ?, ?,?,?)",
                        dailyOrderId, new Date(), new Date(), "KB_USL99770", "Суточный заказ", "010277043");
                // endregion

                break;
            } // end SKU
            case 1:
            case 2: {// поставка/отгрузка
                //region//костыль
                // Order order = shell.getOrder();
                // if (shell.getMsgID() == null || shell.getMsgID().isEmpty()) {
                //     shell.setMsgID(order.getGuid());
                // }
                //endregion
                //region проверка на дубликат
                String sql = "SELECT count(*) FROM kb_sost st " + " INNER JOIN kb_spros sp ON st.id_obsl = sp.ID"
                        + " INNER JOIN kb_zak z ON z.ID = sp.id_zak" + " WHERE z.id_klient = :custID"
                        + " AND z.id_usr IS NOT NULL" + " AND  st.id_sost = 'KB_USL99770'"
                        + " AND UPPER(st.id_du)= UPPER(:msgID)";
                MapSqlParameterSource params = new MapSqlParameterSource().addValue("custID", shell.getCustomerID())
                        .addValue("msgID", shell.getMsgID());

                if (namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class) > 0)
                    throw new MonitorException("Заказ уже существует");
                //endregion
       
                // ищем в логе
                // MonitorLog monitorLog = namedParameterJdbcTemplate.queryForObject(
                //         "SELECT * FROM MONITOR_LOG WHERE ID = :ID",
                //         new MapSqlParameterSource().addValue("ID", shell.getMsgID()),
                //         (rs, rowNum) -> new MonitorLog(rs.getString("id"), rs.getString("name"), rs.getInt("age"),
                //                 rs.getString("e"), rs.getDate("e"), rs.getDate("e")));
                //add to log
                // jdbcTemplate.update(
                //         "insert into monitor_log (insert into monitor_log ( id , status, msg_type , file_name , start_date ,end_date)"
                //                 + " VALUES(?,?,?,?,?,?)",
                // shell.getMsgID());

                InputStream is = XmlUtiles.marshaller(shell);
                String xmlOrder = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String procedureName = shell.getOrder().isOrderType() ? "MSG_4103_" : "MSG_4101_";
                SimpleJdbcCall jdbcCallOrder = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName(procedureName);
                p_err = jdbcCallOrder.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xmlOrder, new DefaultLobHandler()), Types.CLOB));
                if (p_err.get("P_ERR") != null)
                    throw new MonitorException((String) p_err.get("P_ERR"));
                  
             break;

            }
            case 9://тестовая ветка для разных экспериментов
                log.info("TEST");

                shell.setMsgType(0);//FIXME

                //region проверка на дубликат
                String sql = "SELECT count(*) FROM kb_sost st " + " INNER JOIN kb_spros sp ON st.id_obsl = sp.ID"
                        + " INNER JOIN kb_zak z ON z.ID = sp.id_zak" + " WHERE z.id_klient = :custID"
                        + " AND z.id_usr IS NOT NULL" + " AND  st.id_sost = 'KB_USL99770'"
                        + " AND UPPER(st.id_du)= UPPER(:msgID)";
                MapSqlParameterSource params = new MapSqlParameterSource().addValue("custID", shell.getCustomerID())
                .addValue("msgID", shell.getMsgID());
                
                if (namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class) > 0)
                    throw new MonitorException("Заказ уже существует");
                //endregion
                /*
                select * from kb_zak where id='0102304213';
                select s.* from kb_spros s where id_zak='0102304213';

                SELECT sp.* FROM kb_spros sp WHERE sp.n_gruz like '%SKU' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak ='0102304213';
                --st.id_du,st.*
SELECT 1 FROM kb_sost st
INNER JOIN kb_spros sp ON st.id_obsl = sp.ID
INNER JOIN kb_zak z ON z.ID = sp.id_zak
WHERE z.id_klient = 300185
AND z.id_usr IS NOT NULL
--AND sp.id_zak='0102304213'
AND  st.id_sost = 'KB_USL99770' Получено входящее сообщение	4301
AND st.id_du= '965e4682-9ec3-11eb-80c0-00155d0c6c19'
;                ;
                */
                /* Object myParam;
                                boolean hasRecord =
                                jdbcTemplate
                                .query("select 1 from MyTable where Param = ?",
                                    new Object[] { myParam },
                                    (ResultSet rs) -> {
                                    if (rs.next()) {
                                        return true;
                                    }
                                    return false;
                                    }
                                );
                */
                
                //region//костыль
                // Order order = shell.getOrder();
                // if (shell.getMsgID() == null || shell.getMsgID().isEmpty()) {
                //     shell.setMsgID(order.getGuid());
                // }
                //endregion
                
                InputStream is = XmlUtiles.marshaller(shell);
                String xmlOrder = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                String procedureName = shell.getOrder().isOrderType() ? "MSG_4103_" : "MSG_4101_";
                SimpleJdbcCall jdbcCallOrder = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName(procedureName);
                p_err = jdbcCallOrder.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xmlOrder, new DefaultLobHandler()), Types.CLOB));
                if (p_err.get("P_ERR") != null)
                    throw new MonitorException((String) p_err.get("P_ERR"));
                // log.info("TEST\n"+xmlOrder);
                break;
            default:
                throw new MonitorException("Неизвестный тип файла - " + shell.getMsgType());
        }
    }

    /**
     * Обработчик входных сообщений старого формата
     *
     * @param xmlText
     * @param filePrefix
     * @param pathOut
     * @return Текст ошибки для возрата клиенту
     * @throws IOException
     * @throws MonitorException
     * @throws FTPException
     */
    @Transactional //(propagation = Propagation.REQUIRES_NEW) // для отката при исключениях при работе с ДБ
    public String msgIn(String xmlText, String filePrefix, String pathOut)
            throws IOException, MonitorException, FTPException {
        Customer customer = new Customer();
        String orderError = null; // для обработки заказов
        Map<String, Object> p_err; // возвращаемое из процедуры сообщение
        switch (filePrefix) {
            case "P": {// PART_STOCK
                PartStock stockRq = xmlMapper.readValue(xmlText, PartStock.class); // десериализуем (из потока
                // создаём объект)
                // regionПолучить клиента по ВН
                // https://mkyong.com/spring/queryforobject-throws-emptyresultdataaccessexception-when-record-not-found/
                try {
                    customer = jdbcTemplate.queryForObject(
                            "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT,PRF_WMS FROM kb_zak WHERE "
                                    + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                            new CustomerRowMapper(), stockRq.getClientId());
                } catch (EmptyResultDataAccessException e) {
                    orderError = "PART STOCK -->ВН " + stockRq.getClientId() + " не найден";
                    break;
                    // throw new MonitorException("PART STOCK -->ВН " + stockRq.getClientId() + " не
                    // найден");
                }
                // endregion
                // region Получить остатки
                SqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", customer.getHolderID());
                List<PartStockLine> partStockLines = namedParameterJdbcTemplate.query(// пустой набор не ошибка?
                        "SELECT * FROM loads WHERE holder_id = :id", // в поле response_extra.master ?
                        ftpParam, (rs, i) -> new PartStockLine(rs.getInt("LINENO"), rs.getString("ARTICLE"),
                                rs.getString("UPC"), rs.getString("NAME"), rs.getInt("QTY")));
                stockRq.setTimeStamp(new Date()); // текущая дата
                stockRq.setStockLines(partStockLines);
                // endregion
                // имя файла PS_VN_TIMESTAMP/ PS from field prefix
                String fileName = "PS_" + customer.getClientId() + "_" + new Date().getTime() + ".xml";
                String xml = xmlMapper.writer().writeValueAsString(stockRq); // сериализация

                // region выгрузка на FTP
                ftp.changeWorkingDirectory(rootDir + pathOut);
                InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                ok = ftp.storeFile(fileName, is);
                is.close();
                if (ok) {
                    // region Поиск/создание суточного заказа
                    String dailyOrderSql = DAILY_ORDER_STOCK;
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

                    // добавляем событие 4301 в суточный заказ Получено входящее сообщение
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
                    throw new FTPException("Не удалось выгрузить " + fileName);// если текущий FTP кирдык выходим из
                    // цикла или нет?
                }
                // endregion
                break;
            }
            case "S": {// SKU
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("ADD_SKU");
                p_err = jdbcCall.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB));
                orderError = (String) p_err.get("P_ERR");
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
                 * catch (EmptyResultDataAccessException e) { // доделать
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
            }
            case ("I"): {// поставка
                // Order orderIn =xmlMapper.readValue(xmlText, Order.class);
                SimpleJdbcCall jdbcCall_4101 = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("MSG_4101");
                p_err = jdbcCall_4101.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB));
                orderError = (String) p_err.get("P_ERR");
                break;
            }
            case ("O"): { // отгрузка
                SimpleJdbcCall jdbcCall_4103 = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("MSG_4103");
                p_err = jdbcCall_4103.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xmlText, new DefaultLobHandler()), Types.CLOB));
                orderError = (String) p_err.get("P_ERR");
                if (p_err.get("P_ERR") != null) {
                    Utils.emailAlert((String) p_err.get("P_ERR"));// TODO доработать
                    throw new MonitorException((String) p_err.get("P_ERR"));// + fileName);
                }
            }
                break;
        }
        return orderError;
    }

    /**
     * Квитирование  SUCCESS
     * @param fileNameIn
     * @param prefix
     * @param shell
     * @throws IOException
     * @throws FTPException
     * @throws JAXBException
     */
    private void confirm(String fileName) throws IOException, FTPException, JAXBException {
        if (!ftp.changeWorkingDirectory(rootDir + "OUT")) {
            throw new FTPException("Не удалось сменить директорию");
        }
        Confirmation confirmation = new Confirmation();
        confirmation.setStatus("SUCCESS");
        // создаём xml и передаём на FTP
        Shell _shell = new Shell();
        _shell.setCustomerID(shell.getCustomerID());
        _shell.setMsgID(shell.getMsgID());
        _shell.setMsgType(shell.getMsgType());
        _shell.setConfirmation(confirmation);
        if (!ftp.storeFile("_" + fileName, XmlUtiles.marshaller(_shell))) {
            throw new FTPException("Ошибка квитирования. Не удалось выгрузить " + fileName);
        }
        log.info(shell.getCustomerID() + "Обработан файл " + fileName);
    }

    /**
     * Квитирование ERROR
     * @param fileNameIn
     * @param msgType
     * @param errorText
     * @param docNo
     * @throws IOException
     * @throws FTPException
     * @throws JAXBException
     */
    private void confirm(String fileName, String errorText)
            throws IOException, FTPException {
        log.error(errorText);
        if (!ftp.changeWorkingDirectory(rootDir + "OUT")) {
            throw new FTPException("Не удалось сменить директорию");
        }
        Confirmation confirmation = new Confirmation();
        confirmation.setStatus("ERROR");
        confirmation.setInfo(errorText);
        // создаём xml и передаём на FTP
        Shell _shell = new Shell();
        _shell.setCustomerID(shell.getCustomerID());
        _shell.setMsgID(shell.getMsgID());
        _shell.setMsgType(shell.getMsgType());
        _shell.setConfirmation(confirmation);
        try {
            if (!ftp.storeFile("_" + fileName, XmlUtiles.marshaller(_shell))) {
                throw new FTPException("Ошибка квитирования. Не удалось выгрузить " + fileName);
            }
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Костыль. Получить тип сообщения по префиксу фала
     * @param prefix
     * @return message type
     */
    private int prefix2MsgType(String prefix) {
        int msg;
        switch (prefix) {
            case "TEST":
                msg = 9;//поставка
                break;
            case "SKU":
                msg = 5;
                break;
            case "IN":
                msg = 1;
                break;
            case "OUT":
                msg = 2;
                break;
            default:
                msg = 9;
        }
        shell.setMsgType(msg);
        // if (shell.getMsgID() == null || shell.getMsgID().isEmpty()) {
        //     if (msg == 0 || msg == 1) {
        //         shell.setMsgID(shell.getOrder().getGuid()); //костыль
        //     }
        // }
        return msg;
    }

}
