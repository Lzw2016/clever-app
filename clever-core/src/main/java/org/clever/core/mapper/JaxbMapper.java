package org.clever.core.mapper;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.reflection.ReflectionsUtils;
import org.clever.util.Assert;

import javax.xml.bind.*;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 使用Jaxb2.0实现XML<->Java Object的Mapper.<br/>
 * <b>(使用JDK自带的功能不依赖其他库)</b>
 * 在创建时需要设定所有需要序列化的Root对象的Class.<br/>
 * 特别支持Root对象是Collection的情形.<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-4-29 23:51 <br/>
 */
public class JaxbMapper {
    private static final ConcurrentMap<Class<?>, JAXBContext> jaxbContexts = new ConcurrentHashMap<>();

    /**
     * Java Object->Xml without encoding.
     */
    public static String toXml(Object root) {
        Class<?> clazz = ReflectionsUtils.getUserClass(root);
        return toXml(root, clazz, null);
    }

    /**
     * Java Object->Xml with encoding.
     */
    public static String toXml(Object root, String encoding) {
        Class<?> clazz = ReflectionsUtils.getUserClass(root);
        return toXml(root, clazz, encoding);
    }

    /**
     * Java Object->Xml with encoding.
     */
    @SneakyThrows
    public static String toXml(Object root, Class<?> clazz, String encoding) {
        StringWriter writer = new StringWriter();
        createMarshaller(clazz, encoding).marshal(root, writer);
        return writer.toString();
    }

    /**
     * Java Collection->Xml without encoding, 特别支持Root Element是Collection的情形.
     */
    public static String toXml(Collection<?> root, String rootName, Class<?> clazz) {
        return toXml(root, rootName, clazz, null);
    }

    /**
     * Java Collection->Xml with encoding, 特别支持Root Element是Collection的情形.
     */
    @SneakyThrows
    public static String toXml(Collection<?> root, String rootName, Class<?> clazz, String encoding) {
        CollectionWrapper wrapper = new CollectionWrapper();
        wrapper.collection = root;
        JAXBElement<CollectionWrapper> wrapperElement = new JAXBElement<>(new QName(rootName), CollectionWrapper.class, wrapper);
        StringWriter writer = new StringWriter();
        createMarshaller(clazz, encoding).marshal(wrapperElement, writer);
        return writer.toString();
    }

    /**
     * Xml->Java Object.
     */
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public static <T> T fromXml(String xml, Class<T> clazz) {
        StringReader reader = new StringReader(xml);
        return (T) createUnmarshaller(clazz).unmarshal(reader);
    }

    /**
     * 创建Marshaller并设定encoding(可为null).
     * 线程不安全，需要每次创建或pooling。
     */
    @SneakyThrows
    public static Marshaller createMarshaller(Class<?> clazz, String encoding) {
        JAXBContext jaxbContext = getJaxbContext(clazz);
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        if (StringUtils.isNotBlank(encoding)) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, encoding);
        }
        return marshaller;
    }

    /**
     * 创建UnMarshaller.
     * 线程不安全，需要每次创建或pooling。
     */
    @SneakyThrows
    public static Unmarshaller createUnmarshaller(Class<?> clazz) {
        JAXBContext jaxbContext = getJaxbContext(clazz);
        return jaxbContext.createUnmarshaller();
    }

    protected static JAXBContext getJaxbContext(Class<?> clazz) {
        Assert.notNull(clazz, "'clazz' must not be null");
        JAXBContext jaxbContext = jaxbContexts.get(clazz);
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(clazz, CollectionWrapper.class);
                jaxbContexts.putIfAbsent(clazz, jaxbContext);
            } catch (JAXBException ex) {
                throw new RuntimeException("Could not instantiate JAXBContext for class [" + clazz + "]: " + ex.getMessage(), ex);
            }
        }
        return jaxbContext;
    }

    /**
     * 封装Root Element 是 Collection的情况.
     */
    public static class CollectionWrapper {
        @XmlAnyElement
        protected Collection<?> collection;
    }
}
