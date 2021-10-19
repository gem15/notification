package com.severtrans.notification.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.severtrans.notification.dto.Shell;
import com.severtrans.notification.service.MonitorException;

import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

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
        // check for UTF8_BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        JAXBContext jaxbContext = JAXBContext.newInstance(Shell.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        return jaxbUnmarshaller.unmarshal(new StreamSource(new StringReader(content)), clasz).getValue();
    }

    public static Shell unmarshallShell(String content) throws MonitorException {
        // check for UTF8_BOM
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        JAXBContext jaxbContext;
        Unmarshaller jaxbUnmarshaller;
        try {
            jaxbContext = JAXBContext.newInstance(Shell.class);
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return jaxbUnmarshaller.unmarshal(new StreamSource(new StringReader(content)), Shell.class).getValue();
        } catch (JAXBException e) {
            throw new MonitorException("Неверный формат сообщения\n");
        }

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
        StringWriter output = new StringWriter();
        Result result = new StreamResult(output);
        transformer.transform(source, result);
        // transformer.transform(source, new StreamResult(new File("output.xml")));
        return output.toString();// TODO maybe return result?

    }

    /**
     * Shell into InputStream to store on FTP
     * 
     * @param shell
     * @return InputStream
     */
    public static InputStream marshaller(Shell shell) {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(shell.getClass());
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true); // without prolog
            JAXBElement<Shell> jaxbElement = new JAXBElement<>(new QName("http://www.severtrans.com", "Shell"),
                    Shell.class, shell); // для генерации root element
            StringWriter sw = new StringWriter();
            jaxbMarshaller.marshal(jaxbElement, sw);
            return new ByteArrayInputStream(sw.toString().getBytes(StandardCharsets.UTF_8));
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String marshaller(Shell shell, boolean withProlog) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(shell.getClass());
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, withProlog); // without prolog

        JAXBElement<Shell> jaxbElement = new JAXBElement<>(new QName("http://www.severtrans.com", "Shell"), Shell.class,
                shell);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        jaxbMarshaller.marshal(jaxbElement, outputStream);
        InputStream targetStream = new ByteArrayInputStream(outputStream.toByteArray());
        try {
            return new String(targetStream.readAllBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * The problem was not the encoding, but the wrong type in the StreamResource
     * constructor. If you call StreamResource constructor with a String argument it
     * tries to parse this string as an URL. See it in the documentation:
     * docs.oracle.com/javase/7/docs/api/javax/xml/transform/stream/… My suggestion
     * calls the StreamResource constructor with an InputStream argument, and to do
     * that I wrapped the xmlContent string with an InputStream.
     *
     * @param xmlFile
     * @param schemaFile
     * @return
     */
    public boolean validate(String xmlFile, String schemaFile) {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(new StreamSource(getClass().getResourceAsStream(schemaFile)));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xmlFile.getBytes(StandardCharsets.UTF_8))));
            return true;
        } catch (SAXException | IOException e) {
            log.error(e.getMessage());
            return false;
        }
    }

}
