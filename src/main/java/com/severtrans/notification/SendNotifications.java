package com.severtrans.notification;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.severtrans.notification.dto.Order;
import com.severtrans.notification.dto.SKU;
import com.severtrans.notification.utils.CalendarConverter;
import com.severtrans.notification.utils.XmlUtiles;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.severtrans.notification.model.Customer;
import com.severtrans.notification.model.CustomerRowMapper;
import com.severtrans.notification.model.Ftp;
import com.severtrans.notification.dto.ListSKU;
import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.dto.jackson.Notification;
import com.severtrans.notification.dto.jackson.NotificationItem;
import com.severtrans.notification.dto.jackson.OrderJackIn;
import com.severtrans.notification.dto.jackson.OrderJackOut;
import com.severtrans.notification.model.NotificationItemRowMapper;
import com.severtrans.notification.dto.jackson.PartStock;
import com.severtrans.notification.dto.jackson.PartStockLine;
import com.severtrans.notification.model.ResponseFtp;
import com.severtrans.notification.model.Unit;
import com.severtrans.notification.service.FTPException;
import com.severtrans.notification.service.MonitorException;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
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
    @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}") // TODO какую задержку и какого типа?
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
                // region запрос для главного цикла
                MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id", ftpLine.getId());
                List<ResponseFtp> responses = namedParameterJdbcTemplate.query(
                        "SELECT vn, path_in, path_out, e.master, e.details, alias_text alias, e.prefix, e.order_type, t.inout_id, f.hostname, r.legacy"
                                + " FROM response_ftp r INNER JOIN response_extra e ON r.response_extra_id = e.id"
                                + " INNER JOIN ftps f ON r.ftp_id = f.id"
                                + " INNER JOIN response_type T ON T.ID = e.response_type_id" + " WHERE r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(rs.getInt("vn"), rs.getString("path_in"),
                                rs.getString("path_out"), rs.getString("master"), rs.getString("details"),
                                rs.getString("alias"), rs.getString("prefix"), rs.getString("order_type"),
                                rs.getInt("inout_id"), rs.getString("hostname"), rs.getBoolean("legacy")));
                // endregion
                // главный цикл
                for (ResponseFtp resp : responses) {
                    if (resp.isLegacy()) {//FIXME remove me
                        continue;
                    }
                    log.info("Клиент " + resp.getVn());// TODO заменить на тип сообщения ?
                    // отдельно обрабатываем входящие и исходящие сообщения
                    if (!resp.isLegacy()) { //формат xsd
                        log.info(">> New version with xsd");
                        switch (resp.getInOut()) {
                            case (1): { //TODO убрать повтор  потом
                                ftp.changeWorkingDirectory(resp.getPathIn());
                                FTPFileFilter filter = ftpFile -> (ftpFile.isFile()
                                        && ftpFile.getName().endsWith(".xml"));
                                FTPFile[] listFile = ftp.listFiles(resp.getPathIn(), filter);
                                for (FTPFile file : listFile) {
                                    log.info("Processing file " + file.getName());
                                    String xmlText;// извлекаем файл в поток и преобразуем в строку
                                    try (InputStream remoteInput = ftp.retrieveFileStream(file.getName())) {
                                        xmlText = new String(remoteInput.readAllBytes(), StandardCharsets.UTF_8);
                                    }

                                    //TODO доделать - копировать в loaded
                                    ftp.deleteFile(file.getName());// TODO удаляем принятый файл/переименовываем?

                                    if (!ftp.completePendingCommand()) {
                                        throw new FTPException("Completing Pending Commands Not Successful");
                                    }
                                    try {
                                        msgInNew(file.getName().split("_")[0],
                                                XmlUtiles.unmarshaller(xmlText, Shell.class));
                                    } catch (MonitorException e) { // сообщения с разными ошибками
                                        log.error(e.getMessage());// TODO документ email
                                    } catch (DataAccessException | JAXBException e) {
                                        log.error(e.getMessage());
                                        //Utils.emailAlert(error);// TODO доработать ошибку и файл приатачить
                                    }
                                }
                                break;
                            }
                        }
                    } else {//старый формат
                        switch (resp.getInOut()) {
                            case (1): { // все входящие сообщения

                                ftp.changeWorkingDirectory(resp.getPathIn());
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
                                List<Notification> listMaster = namedParameterJdbcTemplate.query(resp.getQueryMaster(),
                                        queryParam, new NotificationRowMapper());
                                for (Notification master : listMaster) {
                                    master.setOrderType(resp.getOrderType());// Отгрузка/Поставка
                                    master.setTypeOfDelivery(resp.getOrderType());
                                    MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource()
                                            .addValue("id", master.getDu());
                                    List<NotificationItem> items = namedParameterJdbcTemplate.query(
                                            resp.getQueryDetails(), mapSqlParameterSource,
                                            new NotificationItemRowMapper());
                                    if (items.size() == 0)
                                        continue;
                                    master.setItems(items);
                                    // to XML
                                    String xml = xmlMapper.writer().withRootName(resp.getAlias())
                                            .writeValueAsString(master);

                                    // имя файла
                                    if (master.getOrderNo() == null)
                                        throw new IOException("Отсутствует номер заказа");// FIXME обсудить
                                    String fileName = resp.getPrefix() + "_"
                                            + master.getOrderNo().replaceAll("\\D+", "") + "_" + new Date().getTime()
                                            + ".xml";
                                    // Changes working directory
                                    if (!ftp.changeWorkingDirectory(resp.getPathOut()))
                                        throw new FTPException("Не удалось сменить директорию");
                                    // передача на FTP
                                    is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                                    boolean ok = ftp.storeFile(fileName, is);
                                    is.close();
                                    if (ok) {
                                        // FIXME добавляем 4302 подтверждение что по данному заказу мы отправили
                                        // уведомление
                                        /*
                                         * jdbcTemplate.update(
                                         * "INSERT INTO kb_sost (id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm) VALUES (?, ?, ?, ?,?)"
                                         * , master.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
                                         */
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
            log.info(" Закончена обработка FTP " + ftpLine.getHostname() + " " + ftpLine.getDescription());
        }
    }

    /**
     * Новый формат обработки входящих сообщений
     * @param filePrefix
     * @param shell
     * @throws IOException
     * @throws MonitorException
     * @throws FTPException
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void msgInNew(String filePrefix, Shell shell) throws IOException, MonitorException, FTPException {
        Customer customer = new Customer();
        // String orderError = null; // FIXME заменить на throw MonExcep
        Map<String, Object> p_err; // возвращаемое из процедуры сообщение
        switch (filePrefix) {
            case "SKU": {
                // Справочник е.и.
                String sql = "SELECT h.val_id id,h.val_short code ,h.val_full name FROM sv_hvoc h WHERE h.voc_id = 'KB_MEA'";
                List<Unit> units = jdbcTemplate.query(sql, new BeanPropertyRowMapper(Unit.class));

                // region заполнить KB_T_ARTICLE
                ListSKU skus = shell.getSkuList();
                jdbcTemplate.update("DELETE FROM KB_T_ARTICLE");
                String sqlArt = "INSERT INTO KB_T_ARTICLE (id_sost, comments, measure, marker, str_sr_godn, storage_pos, tip_tov)\n"
                        + "    VALUES (?,?,?, ?,?,?,?)";
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
                        ps.setString(5, String.valueOf(sku.getStorageLife()));
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
                try {
                    customer = jdbcTemplate.queryForObject(
                            "SELECT ID,ID_SVH,ID_WMS,ID_USR,N_ZAK,ID_KLIENT,PRF_WMS FROM kb_zak WHERE "
                                    + "id_usr IN ('KB_USR92734', 'KB_USR99992') AND id_klient = ?",
                            new CustomerRowMapper(), shell.getCustomerID());
                } catch (EmptyResultDataAccessException e) {
                    throw new MonitorException("ВН " + shell.getCustomerID() + " не найден");
                }
                // endregion

                //  region передача в солво
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_PACK")
                        .withProcedureName("WMS3_UPDT_SKU");
                p_err = jdbcCall.execute(new MapSqlParameterSource().addValue("P_ID", customer.getId())
                        .addValue("P_PREF", customer.getPrefix()));
                String orderError = (String) p_err.get("P_ERR");
                if (!orderError.split(" ")[0].equals("Загружено"))//TODO так себе проверка
                    throw new MonitorException(orderError);
                // endregion

                // region Поиск/создание суточного заказа
                String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'SKU' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
                /*
                                SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'SKU'   AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = l_id_zak;
                                SqlParameterSource namedParameters = new MapSqlParameterSource("id", id);
                                jdbcTemplate.queryForObject(sql, namedParameters, String.class);
                */
                String dailyOrderId;
                try {
                    dailyOrderId = jdbcTemplate.queryForObject(dailyOrderSql, String.class, customer.getId());
                } catch (EmptyResultDataAccessException e) {
                    SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate).withTableName("kb_spros")
                            .usingGeneratedKeyColumns("id");
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("dt_zakaz", new Date())
                            .addValue("id_zak", customer.getId())
                            .addValue("id_pok", customer.getId())
                            .addValue("n_gruz", "SKU")
                            .addValue("usl", "Суточный заказ по пакетам SKU");
                    KeyHolder keyHolder = simpleJdbcInsert.executeAndReturnKeyHolder(params);
                    dailyOrderId = keyHolder.getKeyAs(String.class);
                }
                // endregion

                // событие 4301 в суточный заказ Получено входящее сообщение
                jdbcTemplate.update(
                        "INSERT INTO kb_sost (id_obsl, dt_sost, dt_sost_end, id_sost,  sost_prm, id_isp) VALUES (?, ?, ?, ?,?,?)",
                        dailyOrderId, new Date(), new Date(), "KB_USL99770", "Уточнить текст","010277043");

                break;
            } // SKU
            case ("IN"):
            case ("OUT"): {// поставка/отгрузка
                Order order = shell.getOrder();
                ModelMapper mp = new ModelMapper();
                mp.addConverter(new CalendarConverter());
       
                String xml_out;
                if (!order.isOrderType()) {
                    OrderJackIn jack = mp.map(order, OrderJackIn.class);
                    jack.setOrderType("Поставка");
                    jack.setDeliveryType("Поставка");
                    xml_out = xmlMapper.writer().withRootName("ReceiptOrderForGoods")
                            .writeValueAsString(jack);
                } else {
                    OrderJackOut jack = mp.map(order, OrderJackOut.class);
                    jack.setClientID(shell.getCustomerID());
                    jack.setOrderType("Отгрузка");
                    jack.setDeliveryType("Отгрузка");
                    xml_out = xmlMapper.writer().withRootName("ExpenditureOrderForGoods")
                            .writeValueAsString(jack);
                }
                String procedureName = filePrefix.equals("IN") ? "MSG_4101" : "MSG_4103";
                SimpleJdbcCall jdbcCall_4101 = new SimpleJdbcCall(jdbcTemplate).withCatalogName("KB_MONITOR")
                        .withProcedureName(procedureName);
                p_err = jdbcCall_4101.execute(new MapSqlParameterSource().addValue("P_MSG",
                        new SqlLobValue(xml_out, new DefaultLobHandler()), Types.CLOB));
                if (p_err != null)
                    throw new MonitorException((String)p_err.get("P_ERR"));
   
               break;
            }
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
    @Transactional // для отката при исключениях при работе с ДБ
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
                ftp.changeWorkingDirectory(pathOut); // FIXME из таблицы
                is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                boolean ok = ftp.storeFile(fileName, is);
                is.close();
                if (ok) {
                    // region Поиск/создание суточного заказа
                    String dailyOrderSql = "SELECT sp.id FROM kb_spros sp WHERE sp.n_gruz = 'STOCK' AND trunc(sp.dt_zakaz) = trunc(SYSDATE) AND sp.id_zak = ?";
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
                // SKU sku = xmlMapper.readValue(xmlText, SKU.class);
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

}
