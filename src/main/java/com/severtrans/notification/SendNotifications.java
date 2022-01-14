package com.severtrans.notification;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.severtrans.notification.model.Customer;
import com.severtrans.notification.model.CustomerRowMapper;
import com.severtrans.notification.model.Ftp;
import com.severtrans.notification.model.MonitorLog;
import com.severtrans.notification.model.NotificationItemRowMapper;
import com.severtrans.notification.model.ResponseFtp;
import com.severtrans.notification.model.Unit;
import com.severtrans.notification.repository.CustomerDao;
import com.severtrans.notification.repository.EventLogDao;
import com.severtrans.notification.repository.MonitorLogDao;
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
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
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
    NamedParameterJdbcTemplate npJdbcTemplate;
    @Autowired
    CustomerDao customerDao;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    FTPClient ftp;
    @Autowired
    XmlMapper xmlMapper;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    MonitorLogDao logDao;
    @Autowired
    EventLogDao eventLog;

    /**
     * FTP root directory
     */
    private String rootDir;
    private Shell shell;
    private String xml;
    private boolean ok;

    String folderIN = "IN";
    String folderOUT = "OUT";
    String folderLOADED = "LOADED"; // *TEST* LOADED_TEST
    // *TEST*
    // String folderIN = "IN_TEST";
    // String folderOUT = "OUT_TEST";
    // String folderLOADED = "LOADED_TEST";

    // @Scheduled(fixedDelay = Long.MAX_VALUE) // initialDelay = 1000 * 30,
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void reply() {
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps",
                (rs, rowNum) -> new Ftp(rs.getInt("id"), rs.getString("login"), rs.getString("password"),
                        rs.getString("hostname"), rs.getInt("port"), rs.getString("description")));
        for (Ftp ftpLine : ftps) {// цикл по всем FTP

            // if (ftpLine.getId() == 4)
            //     continue; // FIXME *PROD* пропуск тестового
            if (ftpLine.getId() != 4) // FIXME *TEST* заглушка для отладки
                continue;
            else
                folderLOADED = "LOADED_TEST";

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
                List<ResponseFtp> responses = npJdbcTemplate.query(
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
                    if (resp.isLegacy())
                        continue;
                    // region folders костыль
                    folderIN = resp.getPathIn();
                    folderOUT = resp.getPathOut();
                    // endregion
                    switch (resp.getInOut()) {
                        case (1): { // входящие
                            ftp.changeWorkingDirectory(rootDir + folderIN);
                            FTPFileFilter filter = ftpFile -> (ftpFile.isFile() && ftpFile.getName().endsWith(".xml"));
                            FTPFile[] listFiles = ftp.listFiles(ftp.printWorkingDirectory(), filter);

                            for (FTPFile file : listFiles) {
                                // region извлекаем файл в поток и преобразуем в строку
                                ftp.changeWorkingDirectory(rootDir + folderIN);
                                try (InputStream remoteInput = ftp.retrieveFileStream(file.getName())) {
                                    xml = new String(remoteInput.readAllBytes(), StandardCharsets.UTF_8);
                                }
                                if (!ftp.completePendingCommand()) {// завершение FTP транзакции
                                    throw new FTPException("Completing Pending Commands Not Successful");
                                }
                                // endregion
                                // region сохраняем принятый в папке LOADED
                                // https://stackoverflow.com/a/6790857/2289282
                                if (ftpLine.getId() != 4) { // FIXME *TEST* заглушка для отладки
                                    String remotePath = rootDir + folderLOADED + "/" + file.getName();
                                    // if exist delete
                                    FTPFile[] remoteFiles = ftp.listFiles(remotePath);
                                    if (remoteFiles.length > 0)
                                        ftp.deleteFile(remotePath);
                                    ok = ftp.rename(rootDir + folderIN + "/" + file.getName(), remotePath);
                                    if (!ok)
                                        throw new FTPException("Ошибка перемещения файла " + file.getName());
                                }
                                // endregion

                                try {
                                    msgInNew(file);
                                } catch (DataAccessException | DbActionExecutionException e) {
                                    log.error("\nОшибка при работе с Базой Данных. " + e.getMessage(),e);
                                }
                            }
                        }
                            break;
                        case (3): {// FIXME 2 !!! NEW все исходящие сообщения (отбивки)
                            MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id", resp.getVn());
                            List<NotificationJack> listMaster;
                            listMaster = npJdbcTemplate.query(resp.getQueryMaster(), queryParam,
                                    new NotificationRowMapper());

                            for (NotificationJack master : listMaster) {
                                MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id",
                                        master.getDu());
                                // region
                                String sql = "WITH cte AS (SELECT s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3"
                                        + " , sum(l.units) AS qty, l.comments" + " FROM wms.order_details@wms3 o"
                                        + " INNER JOIN wms.loads@wms3   l ON o.order_id = l.order_id"
                                        + " INNER JOIN wms.sku@wms3     s ON l.sku_id = s.id"
                                        + " WHERE o.order_id = :id AND o.sku_id = l.sku_id"
                                        + " GROUP BY s.sku_id, s.name, l.expiration_date, l.production_date, l.lot, l.marker, l.marker2, l.marker3, l.comments)"
                                        + " SELECT rownum, cte.* FROM cte";
                                // region
                                List<NotificationItem> items = npJdbcTemplate.query(resp.getQueryDetails(), // sql,
                                        mapSqlParameterSource, new NotificationItemRowMapper());
                                if (items.isEmpty())
                                    continue;

                                shell = new Shell();
                                shell.setCustomerID(resp.getVn());
                                shell.setMsgID(UUID.randomUUID().toString());
                                shell.setMsgType(resp.getTypeID());
                                List<NotificationLine> notificationLine = Utils.mapList(items, NotificationLine.class,
                                        modelMapper);
                                Notification notification = modelMapper.map(master, Notification.class);
                                notification.getLine().addAll(notificationLine);
                                shell.setNotification(notification);
                                // region имя файла
                                String fileName = resp.getPrefix() + "_" + master.getGuid() + ".xml";// TODO ТАЙПИТ
                                // region

                                if (!ftp.changeWorkingDirectory(rootDir + resp.getPathOut()))
                                    throw new FTPException("Не удалось сменить директорию");
                                if (ftp.storeFile(fileName, XmlUtiles.marshaller(shell))) {
                                    // 4302 подтверждение что по данному заказу мы отправили уведомление
                                    jdbcTemplate.update(
                                            "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)",
                                            master.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
                                    log.info(resp.getVn() + " " + resp.getTypeName() + " Выгружен " + fileName);
                                } else {
                                    throw new FTPException(
                                            resp.getVn() + " " + resp.getTypeName() + " Не удалось выгрузить "
                                                    + fileName);
                                }
                            }
                        }
                            break;
                    }
                }
                ftp.logout();
            } catch (IOException e) {
                // FTP and XmlMapper
                log.error("Системная ошибка", e);
                e.printStackTrace();
            } catch (FTPException e) { // TODO IOException
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
     * Формирование подтверждений
     * 
     * @throws IOException
     */
    @Transactional
    // @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}", initialDelayString = "${initialDelay.in.milliseconds}")
    public void confirm() {
        // FUCK List<MonitorLog> logs = logDao.findAllIncompleted();
        //ORDER_UID,STATUS,MSG_TYPE,FILE_NAME,START_DATE,END_DATE,MSG,VN,INFO,ID
        List<MonitorLog> logs = jdbcTemplate.query(
                "SELECT id,order_uid as orderUid, FILE_NAME as fileName, msg, info, vn FROM monitor_log WHERE end_date IS NULL ORDER BY vn",
                new BeanPropertyRowMapper<MonitorLog>(MonitorLog.class));

        try {
            Confirmation confirmation;

            // region SKU
            boolean artNotFound = false;
            SqlParameterSource params;
            // отфильтровываем SKU
            List<MonitorLog> mls = logs.stream().filter(s -> s.getMsgType() == 5).collect(Collectors.toList());
            String sql = "SELECT COUNT(*) FROM sku WHERE sku_id=:art";
            for (MonitorLog skuLog : mls) {
                try {
                    shell = XmlUtiles.unmarshallShell(skuLog.getMsg());
                } catch (JAXBException e) {
                    log.error("Не может быть", e);
                    break;
                } // TODO + validation
                log.info("skuLog " + shell.getMsgID());
                for (SKU skuItem : shell.getSkuList().getSku()) {
                    Customer customer = customerDao.findByClientId(skuLog.getVn()).orElse(null);
                    params = new MapSqlParameterSource().addValue("art", customer.getPrefix() + skuItem.getArticle());
                    if (npJdbcTemplate.queryForObject(sql, params, Integer.class) == 0) {
                        artNotFound = true;
                        break;
                    }
                }
                if (!artNotFound) {
                    confirmation = new Confirmation();
                    confirmation.setInfo(skuLog.getInfo());
                    logDao.completeOrder(skuLog.getId(), new Date());
                    sendConfirm(skuLog, confirmation);
                    log.info(shell.getCustomerID() + "\r\nОбработан файл " + skuLog.getFileName());
                }
            }
            if (artNotFound)
                return;
            // endregion

            // region заказы
            mls = logs.stream().filter(s -> s.getMsgType() == 1 || s.getMsgType() == 2).collect(Collectors.toList());
            for (MonitorLog ml : mls) {
                confirmation = new Confirmation();
                try {
                    shell = XmlUtiles.unmarshallShell(ml.getMsg());
                } catch (JAXBException e) {
                    log.error("Не может быть", e);
                    break;
                }

                // if (shell.getMsgType() == 1 || shell.getMsgType() == 2)
                if (ml.getStatus() == "E") {
                    confirmation.setStatus("ERROR");
                } else if (ml.getStatus() == "S") {
                    confirmation.setStatus("SUCCESS");
                }
                confirmation.setInfo(ml.getInfo());
                confirmation.setOrderNo(shell.getOrder().getOrderNo());
                confirmation.setGuid(shell.getOrder().getGuid());
                sendConfirm(ml, confirmation);
                logDao.completeOrder(ml.getId(), new Date());
                log.info(shell.getCustomerID() + "\r\nОбработан файл " + ml.getFileName());
            }
            // endregion

        } catch (IOException e) {
            // проблемы FTP сервера
            log.error(e.getMessage() + ". Код " + ftp.getReplyCode());
        }

        // region try catch
        // try {
        // List<MonitorLog> logs = logDao.findIncompleted();

        // for (MonitorLog ml : logs) {
        // Confirmation confirmation = new Confirmation();
        // try {
        // shell = XmlUtiles.unmarshallShell(ml.getMsg());
        // } catch (JAXBException e) {
        // log.error(e.getMessage());
        // break;
        // }

        // if (shell.getMsgType() == 5) {
        // log.info("пока пропускаем check all arts");
        // continue;
        // } else if (shell.getMsgType() == 1 || shell.getMsgType() == 2) {
        // if ("E".equals(ml.getStatus())) {
        // confirmation.setStatus("ERROR");
        // } else if ("S".equals(ml.getStatus())) {
        // confirmation.setStatus("SUCCESS");
        // }
        // confirmation.setInfo(ml.getInfo());
        // }
        // sendConfirm(ml, confirmation);
        // logDao.completeOrder(ml.getId(), new Date());
        // log.info(shell.getCustomerID() + "\r\nОбработан файл " + ml.getFileName());
        // }
        // } catch (DataAccessException | IOException e) {
        // log.error("\nСистемная ошибка\n", e);
        // }
        // log.info("Pause");
        // endregion
    }

    /**
     * Передаём Confirm на FTP
     * 
     * @param ml           monitor_log
     * @param confirmation confirmation
     * @throws IOException ошибка FTP
     */
    private void sendConfirm(MonitorLog ml, Confirmation confirmation) throws IOException {
        // создаём xml и передаём на FTP
        Shell cshell = new Shell();
        cshell.setCustomerID(shell.getCustomerID());
        cshell.setMsgID(shell.getMsgID());
        cshell.setMsgType(shell.getMsgType());
        cshell.setConfirmation(confirmation);
        
        ftp.changeWorkingDirectory(rootDir + folderOUT);
        ftp.storeFile("_" + ml.getFileName(), XmlUtiles.marshaller(cshell));
    }

    /**
     * Новый формат обработки входящих сообщений
     *
     * @throws IOException
     * @throws MonitorException
     * @throws JAXBException
     * @throws FTPException
     */
    @Transactional
    public void msgInNew(FTPFile file) throws IOException {
        Map<String, Object> err; // возвращаемое из процедуры сообщение
        MonitorLog mlog = new MonitorLog();
        Confirmation confirmation = new Confirmation();

        // region десериализация Shell
        try {//FIXME confirmation
            shell = XmlUtiles.unmarshallShell(xml);
            // region MonitorLog init
            mlog.setVn(shell.getCustomerID());
            mlog.setMsgType(shell.getMsgType());
            mlog.setMsg(xml);
            mlog.setStartDate(new Date());
            mlog.setFileName(file.getName());
            // endregion
        } catch (JAXBException e) {
            mlog.setStatus("E");
            mlog.setInfo("Ошибка при разборе файла");
            logDao.save(mlog);
            confirmation.setStatus("ERROR");
            confirmation.setInfo(mlog.getInfo());
            sendConfirm(mlog, confirmation);
            return;
        } // TODO use schema validator!!!

        // endregion

        prefix2MsgType(file.getName().split("_")[0].toUpperCase());// костыль

        switch (shell.getMsgType()) {
            case 5: { // SKU
                // Справочник е.и.
                String sql = "SELECT h.val_id id,h.val_short code ,h.val_full name FROM sv_hvoc h WHERE h.voc_id = 'KB_MEA'";
                List<Unit> units = jdbcTemplate.query(sql, new BeanPropertyRowMapper<Unit>(Unit.class));

                // region заполнить KB_T_ARTICLE
                ListSKU skus = shell.getSkuList();
                jdbcTemplate.update("DELETE FROM KB_T_ARTICLE");
                String sqlArt = "INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov)\n"
                        + "    VALUES (?,?,?, ?,?,?,?)";
                jdbcTemplate.batchUpdate(sqlArt, new BatchPreparedStatementSetter() {

                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        SKU sku = skus.getSku().get(i);
                        ps.setString(1, sku.getArticle());
                        ps.setString(2, sku.getName());
                        // шт --> KB_.....
                        Unit um = units.stream()
                                // .filter(unit -> sku.getUofm().toUpperCase().equals(unit.getCode().toUpperCase()))
                                .filter(unit -> sku.getUofm().equalsIgnoreCase(unit.getCode()))
                                .findAny()
                                .orElse(null);
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
                // endregion

                // region Получить клиента по ВН
                Customer customer;
                try {
                    customer = jdbcTemplate.queryForObject(
                            "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT,PRF_WMS FROM kb_zak WHERE "
                                    + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                            new CustomerRowMapper(), shell.getCustomerID());
                } catch (EmptyResultDataAccessException e) {
                    mlog.setStatus("E");
                    mlog.setInfo(("ВН " + shell.getCustomerID() + " не найден"));
                    return;
                }
                // endregion

                // region передача в солво
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName("WMS3_UPDT_SKU");
                err = jdbcCall
                        .execute(new MapSqlParameterSource().addValue("P_ID", customer.getId()).addValue("P_PREF",
                                customer.getPrefix()));

                if (err.get("P_ERR") != null) {
                    mlog.setStatus("E");
                    mlog.setInfo((String) err.get("P_ERR"));
                    confirmation.setStatus("ERROR");
                    return;
                } else {
                    mlog.setStatus("S");
                    confirmation.setStatus("SUCCESS");
                }
                // endregion

                // region Поиск/создание суточного заказа 4301 Получено входящее сообщение
                String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz like '%SKU' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
                try {
                    jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
                    log.info("Найден суточный заказ");
                } catch (EmptyResultDataAccessException e) {
                    // создание суточного заказа
                    SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros")
                            .usingGeneratedKeyColumns("id");
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("dt_zakaz", new Date()).addValue("id_zak", customer.getId())
                            .addValue("id_pok", customer.getId())
                            .addValue("n_gruz", customer.getCustomerName() + " SKU")
                            .addValue("usl", "Суточный заказ по пакетам SKU").addValue("ORA_USER_EDIT_ROW_LOCK", 0);
                    // WTF ORA_USER_EDIT_ROW_LOCK !!!!!!!!!!!!!!!!
                    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
                    String dailyOrderId = keyHolder.getKeyAs(String.class);
                    jdbcTemplate.update(
                            "INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp) VALUES (?, ?, ?, ?,?,?)",
                            dailyOrderId, new Date(), new Date(), "KB_USL99770", "Суточный заказ", "010277043");
                    log.info("Создан суточный заказ");
                }
                // endregion

                break;
            } // end SKU
            case 1:
            case 2: {// поставка/отгрузка
                // use shell.getMsgType() ?
                // String procedureName = shell.getOrder().isOrderType() ? "MSG_4103_" : "MSG_4101_";
                //procedure MSG_4101_test(p_msg CLOB :='',p_err out varchar2, p_info out varchar2)

                // region TEST
                String procedureName = "MSG_4101_test";
                // xml = null;
                SimpleJdbcCall jdbcCallOrder = new SimpleJdbcCall(jdbcTemplate).withProcedureName(procedureName);
                // endregion

                // SimpleJdbcCall jdbcCallOrder = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                //         .withProcedureName(procedureName);
                err = jdbcCallOrder.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xml, new DefaultLobHandler()), Types.CLOB));
                if (err.get("P_ERR") != null) {
                    mlog.setStatus("E");
                    confirmation.setStatus("ERROR");
                    mlog.setInfo((String) err.get("P_ERR"));
                } else {
                    mlog.setStatus("S");
                    confirmation.setStatus("SUCCESS");
                    if (err.get("P_INFO") != null)// TODO check it
                        mlog.setInfo((String) err.get("P_INFO"));
                }
                mlog.setOrderUID(shell.getOrder().getGuid());
                logDao.save(mlog);
                confirmation.setGuid(shell.getOrder().getGuid());
                confirmation.setOrderNo(shell.getOrder().getOrderNo());
                break;

            }
            case 9:// тестовая ветка для разных экспериментов
                log.info("TEST");
        }
        confirmation.setInfo(mlog.getInfo());
        sendConfirm(mlog, confirmation);
    }

    /**
     * Костыль Тайпит. Получить тип сообщения по префиксу фала
     * 
     * @param prefix
     * @return message type
     */
    private int prefix2MsgType(String prefix) {
        int msg;
        switch (prefix) {
            case "TEST":
                msg = 9;// поставка
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
        if (shell.getMsgID() == null || shell.getMsgID().isEmpty()) {
            shell.setMsgID(UUID.randomUUID().toString());// костыль
        }
        return msg;
    }

}
