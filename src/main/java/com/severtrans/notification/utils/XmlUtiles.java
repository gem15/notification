package com.severtrans.notification.utils;

import org.apache.commons.codec.binary.Hex;
import com.severtrans.notification.dto.Shell;
import lombok.extern.slf4j.Slf4j;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.GregorianCalendar;

@Slf4j
public class XmlUtiles {

    public <T> T unmarshaller(String content, Class<T> clasz, String xsdFile) throws JAXBException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(clasz);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        // Setup schema validator
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new StreamSource(getClass().getResourceAsStream(xsdFile)));
        jaxbUnmarshaller.setSchema(schema);
        return jaxbUnmarshaller.unmarshal(new StreamSource(new StringReader(content)), clasz).getValue();
    }

    /**
     * Без xsd валидации
     *
     * @param content - xml файл
     * @param clasz
     * @return T
     * @throws JAXBException
     */
    public static <T> T unmarshaller(String content, Class<T> clasz) throws JAXBException {
        //check for  UTF8_BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
/*
        String initialString = "text";
        InputStream targetStream = new ByteArrayInputStream(initialString.getBytes());
*/
        JAXBContext jaxbContext = JAXBContext.newInstance(Shell.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller.unmarshal(new StreamSource(new StringReader(content)), clasz).getValue();
    }

    @Deprecated
    public static <T> T unmarshaller(InputStream content, Class<T> clasz) throws JAXBException, IOException {
//        InputStream inStream = new FileInputStream( "employee.xml" );
//        Employee employee = (Employee) jaxbUnmarshaller.unmarshal( inStream );
        ByteBuffer bb = null;
        // BOM encoded as ef bb bf
        if (isContainBOM(content)) {
            byte[] bytes = content.readAllBytes();//Files.readAllBytes(path);
//            bytes = content.read();
            bb = ByteBuffer.wrap(bytes);
            System.out.println("Found BOM!");
            // get the first 3 bytes
            byte[] bom = new byte[3];
            bb.get(bom, 0, bom.length);

            // remaining
            byte[] contentAfterFirst3Bytes = new byte[bytes.length - 3];
            bb.get(contentAfterFirst3Bytes, 0, contentAfterFirst3Bytes.length);

            System.out.println("Remove the first 3 bytes, and overwrite the file!");
        }

        JAXBContext jaxbContext = JAXBContext.newInstance(clasz);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return (T) jaxbUnmarshaller.unmarshal(content);
    }

    private static boolean isContainBOM(InputStream content) throws IOException {
        boolean result = false;
        byte[] bom = new byte[3];
        // read 3 bytes of a file.
        content.read(bom);
        // BOM encoded as ef bb bf
        String content1 = new String(Hex.encodeHex(bom));
        if ("efbbbf".equalsIgnoreCase(content1)) {
            result = true;
        }
        return result;
    }

    /**
     * xslt преобразование
     *
     * @param input
     * @return
     * @throws TransformerException
     */
    public String transformer(String input) throws TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Source xslt = new StreamSource(getClass().getResourceAsStream("/msg.xsl"));
        Transformer transformer = factory.newTransformer(xslt);

        Source source = new StreamSource(new StringReader(input));
        // Source source = new StreamSource(new File("input.xml"));
        StringWriter output = new StringWriter();
        Result result = new StreamResult(output);
        transformer.transform(source, result);
        // transformer.transform(source, new StreamResult(new File("output.xml")));
        return output.toString();// TODO maybe return result?

    }

    public static void marshaller(Shell shell, OutputStream outputStream) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(shell.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // without prolog

        JAXBElement<Shell> jaxbElement = new JAXBElement<>(new QName("http://www.severtrans.com", "Shell"), Shell.class, shell);
/*
        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(jaxbElement, new PrintWriter(System.out));
//        jaxbMarshaller.marshal(jaxbElement, sw);
        System.out.println(sw.toString());
*/
//https://stackoverflow.com/a/23874232/2289282 PipedInputStream in = new PipedInputStream();
        jaxbMarshaller.marshal(jaxbElement, outputStream);
    }

    /**
     * The problem was not the encoding, but the wrong type in the StreamResource constructor.
     * If you call StreamResource constructor with a String argument it tries to parse this string as an URL.
     * See it in the documentation: docs.oracle.com/javase/7/docs/api/javax/xml/transform/stream/…
     * My suggestion calls the StreamResource constructor with an InputStream argument,
     * and to do that I wrapped the xmlContent string with an InputStream.
     *
     * @param xmlFile
     * @param schemaFile
     * @return
     */
    public boolean validate(String xmlFile, String schemaFile) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Source t = new StreamSource(getClass().getResourceAsStream("/xml/severtrans.xsd"));
            Source xslt = new StreamSource(getClass().getResourceAsStream("/msg.xsl"));

            Schema schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream(schemaFile)));
//            Schema schema = schemaFactory.newSchema(new File((schemaFile)));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xmlFile.getBytes(StandardCharsets.UTF_8))));
//            validator.validate(new StreamSource(xmlFile));
//            validator.validate(new StreamSource(new File(xmlFile)));
            return true;
        } catch
        (SAXException | IOException e) {
            log.error(e.getMessage());
//            e.printStackTrace();
            return false;
        }
    }


    @Deprecated
    // TODO set in bindings
    public XMLGregorianCalendar getNow() throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        DatatypeFactory datatypeFactory;

        datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        return now;
    }

}
