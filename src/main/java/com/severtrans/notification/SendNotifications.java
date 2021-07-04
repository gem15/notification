package com.severtrans.notification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.severtrans.notification.dto.Ftp;
import com.severtrans.notification.dto.Notif;
import com.severtrans.notification.dto.Notification;
import com.severtrans.notification.dto.NotificationItem;
import com.severtrans.notification.dto.NotificationItemRowMapper;
import com.severtrans.notification.dto.ResponseFtp;
import com.severtrans.notification.service.Author;
import com.severtrans.notification.service.Blog;
import com.severtrans.notification.service.Entry;
import com.severtrans.notification.service.NotificationException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

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
    private SimpleDateFormat ts = new SimpleDateFormat("yyyyMMddHHmmss");// HH:mm:ss
    private FTPClient ftpClient = new FTPClient();
    private InputStream is;

    public SendNotifications() {
    }

    // @Transactional
    @Scheduled(fixedDelay = Long.MAX_VALUE) // initialDelay = 1000 * 30,
    // @Scheduled(fixedDelayString = "${fixedDelay.in.milliseconds}")
    public void reply() {

// demo();
// jackson(null);

 /*        try {
            printXMLTest();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
 */        
        // int i=jdbcTemplate.queryForObject("select count(*) from kb_sost",
        // Integer.class);
        // region List<Ftp> ftps = jdbcTemplate.query
        List<Ftp> ftps = jdbcTemplate.query("select * from ftps", (rs, rowNum) -> new Ftp(rs.getInt("id"),
                rs.getString("login"), rs.getString("password"), rs.getString("hostname"), rs.getInt("port")));
        // endregion
        for (Ftp ftp : ftps) {

            log.info(">>> " + new Date().getTime());
            /*
             * MapSqlParameterSource ftpParam = new MapSqlParameterSource().addValue("id",
             * ftp.getId()); //region List<ResponseFtp> responses =
             * namedParameterJdbcTemplate.query List<ResponseFtp> responses =
             * namedParameterJdbcTemplate.query("", ftpParam, (rs, rowNum) -> new
             * ResponseFtp( rs.getString("voc"), rs.getInt("vn"), rs.getString("path") ) );
             * //endregion
             */
            // SimpleJdbcInsert insertActor = new SimpleJdbcInsert(jdbcTemplate);
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
                        "select Vn,path,e.master,e.details,Alias_Text alias,e.direction,e.order_type from response_ftp r\n"
                                + "inner join response_extra e on r.Response_Extra_id = e.Id\n"
                                + "where r.ftp_id = :id",
                        ftpParam,
                        (rs, rowNum) -> new ResponseFtp(rs.getInt("vn"), rs.getString("path"), rs.getString("master"),rs.getString("details"),
                                rs.getString("alias"), rs.getString("direction"), rs.getString("order_type")));
                // endregion
                for (ResponseFtp resp : responses) {
                    /*
                     * String sqlHeader = "", alias = "";
                     * 
                     * switch (responseFtp.getVoc()) { case "KB_USL60174": //4102 sqlHeader =
                     * "select * from notif"; alias = "IssueReceiptForGoods"; break; case
                     * "KB_USL60177"://4104: sqlHeader = "select * from notif"; alias = ""; break;
                     * case E4111: 'KB_USL60189' --4111 sqlHeader =
                     * "select * from notif where id_klient='0'"; break; }
                     */
                    // Changes working directory
                    if (!ftpClient.changeWorkingDirectory(resp.getPath()))
                        throw new NotificationException("Не удалось сменить директорию");

                     MapSqlParameterSource queryParam = new MapSqlParameterSource().addValue("id", resp.getVn());
                    List<Notification> listMaster = namedParameterJdbcTemplate.query(resp.getQueryMaster(), queryParam,
                            new NotificationRowMapper());
                    for (Notification master : listMaster) {

                        master.setOrderType(resp.getOrderType());
                        master.setTypeOfDelivery(resp.getOrderType());

                        // TODO const query text
                        // String sqlItems = resp.gette"select rownum,det.* from notifdet det where iddu =:id";
                        // String sqlItems = "select row_number() over (),det.* from notifdet det where iddu =:id";

                        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource().addValue("id", master.getDu());
                        // region List<NotificationItem> items = namedParameterJdbcTemplate.query
                        List items = namedParameterJdbcTemplate.query(resp.getQueryDetails(), mapSqlParameterSource,
                        new NotificationItemRowMapper()
 /*                                (rs, rowNum) -> new NotificationItem(rs.getInt("ROWNUM"), rs.getString("SKU_ID"),
                                        rs.getString("NAME"),
                                        dateFormat.format(rs.getDate("EXPIRATION_DATE") == null ? new Date()
                                                : rs.getTimestamp("EXPIRATION_DATE")),
                                        dateFormat.format(rs.getDate("PRODUCTION_DATE") == null ? new Date()
                                                : rs.getTimestamp("PRODUCTION_DATE")),
                                        rs.getString("LOT"), rs.getString("SERIAL_NUM"), rs.getString("MARKER"),
                                        rs.getString("MARKER2"), rs.getString("MARKER3"), rs.getInt("QTY"),
                                        rs.getString("COMMENTS")) */
                                        );
                        // endregion
                        master.setItems(items);
                        // region xStream

                        Notification notif=new Notification();
        notif.setDu("du");
        notif.setOrderType("OrderType");
        notif.setTypeOfDelivery("TypeOfDelivery");
        NotificationItem ni =new NotificationItem();
        ni.setArticle("Article");
        List list = new ArrayList();
        list.add(ni);
        notif.getItems().add(list);
                         XStream xs = new XStream();
                        xs.omitField(Notification.class, "du");
                        // xs.omitField(Notification.class, "orderID");
                        // xs.alias(resp.getAlias(), Notification.class);
                        xs.alias("Goods", NotificationItem.class);
                        xs.addImplicitCollection(Notification.class, "items");
 
                        System.out.println(xs.toXML(notif));
 
                        try (Writer writer = new StringWriter()) {
                            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
                            xs.toXML(master, writer);// TODO
                             System.out.println(writer.toString());
                            
                            is = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        // endregion

                        // имя файла
                        // String fileName = resp.getDirection() + "_" + ts.format(new Date()) + ".xml";
                        String fileName = resp.getDirection() + "_" + new Date().getTime() + ".xml";
                        boolean ok = true;//ftpClient.storeFile(fileName, is);
                        is.close();
                        if (ok) {
                            // добавляем 4302 подтверждение что по данному заказу мы отправили уведомление
                            // jdbcTemplate.update(
                            // "INSERT INTO kb_sost (id,id_obsl, id_sost, dt_sost, dt_sost_end, sost_prm)
                            // VALUES (?, ?, ?, ?, ?,?)",
                            // "3", not.getOrderID(), "KB_USL99771", new Date(), new Date(), fileName);
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
            log.info("<<< " + new Date().getTime());
        }
    }

 
    void printXMLTest() throws IOException {
 
/*         Notification not = new Notification();//"Отгрузка", "Отгрузка"
// region
        not.setDu("1212122"); //omitted field
        not.setDate("");
        not.setVehicleFactlArrivalTime("");
        not.setFactDeliveryDate("");
        not.setNumber("");
        not.setCustomer("MyCustomer");
        not.setOrderType("");
        not.setTypeOfDelivery("");
        not.setIDSupplier("");
        not.setNameSupplier("");
        not.setAdressSupplier("");
        not.setVN(300227);
        not.setNumberCar("");
        not.setDriver("");
        // not.setItems(new ArrayList<>());
// endregion
        NotificationItem i=new NotificationItem();
//        i.setLineNumber(1);
        i.setArticle("HLGAD 161010");
        i.setName("Tesla крепление");
//        i.setExpirationDate(new Date());
//        i.setProductionDate(new Date());
//        i.setLot(); TODO check me
        i.setSerialNum("777");
        i.setMarker("");
//        i.setMarker2(""); TODO check me
        i.setMarker3("");
        i.setCount(5);
        i.setComment("My comment");
        // List<NotificationItem> items =new ArrayList<>();
        // items.add(i);
        not.getItems().add(i);
 */        
/*         // Create JAXB Context
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(Notification.class);
              
        //Create Marshaller
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        //Required formatting??
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        //Print XML String to Console
        StringWriter sw = new StringWriter();
         
        //Write XML to StringWriter
        jaxbMarshaller.marshal(not, sw);
         
        //Verify XML Content
        String xmlContent = sw.toString();
        System.out.println( xmlContent );
        
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
 */
/* StaxDriver driver = new StaxDriver();
driver.getOutputFactory().setProperty("escapeCharacters", false);
XStream xs = new XStream(driver);
 */
/*         var xs1 = new XStream();
        List<Notification> list = new ArrayList<>();
        list.add(not);
        System.out.println(xs1.toXML(not));//list
 */
        XStream xs = new XStream();
        xs.omitField(Notification.class,"du");

        xs.alias("IssueReceiptForGoods", Notification.class); //IssueReceiptForGoods
        xs.alias("Goods",NotificationItem.class);
        
        xs.addImplicitCollection(Notification.class,"items");
        
        // System.out.println(xs.toXML(not));

        Writer writer = new StringWriter();
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
        // xs.toXML(not, writer);//        Notification notification = (Notification) xs.fromXML(xml);
        System.out.println(writer.toString());
        System.out.println(">>> stop printXMLTest <<< ");

    }

    private void demo() {
        Blog teamBlog = new Blog(new Author("Guilherme Silveira"));
        teamBlog.add(new Entry("first","My first blog entry."));
        teamBlog.add(new Entry("tutorial",
                "Today we have developed a nice alias tutorial. Tell your friends! NOW!"));

        XStream xs = new XStream();
        xs.alias("blog", Blog.class);
        xs.alias("entry", Entry.class);
        xs.aliasField("author", Blog.class, "writer");
        xs.addImplicitCollection(Blog.class, "entries");
        System.out.println(xs.toXML(teamBlog));
        System.out.println(">>>>> Stop demo <<<<");
    }

    private void jackson(Notification not){
        Notification notif=new Notification();
        notif.setDu("du");
        notif.setOrderType("OrderType");
        notif.setTypeOfDelivery("TypeOfDelivery");
        XStream xs = new XStream();
        System.out.println(xs.toXML(notif));

        ObjectMapper objectMapper = new XmlMapper();
        try {
            String xml = objectMapper.writeValueAsString(not);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    // public String toXML(T object) throws JAXBException {
    //     StringWriter stringWriter = new StringWriter();
      
    //     JAXBContext jaxbContext = JAXBContext.newInstance(T.class);
    //     Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
      
    //     // format the XML output
    //     jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      
    //     QName qName = new QName("com.yourModel.t", "object");
    //     JAXBElement<T> root = new JAXBElement<>(qName, T.class, object);
      
    //     jaxbMarshaller.marshal(root, stringWriter);
      
    //     String result = stringWriter.toString();
    //     // LOGGER.info(result);
    //     return result;
    //   }

}
