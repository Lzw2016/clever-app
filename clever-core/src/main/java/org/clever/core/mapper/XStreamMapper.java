//package org.clever.core.mapper;
//
//import com.thoughtworks.xstream.XStream;
//import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
//import com.thoughtworks.xstream.io.xml.StaxDriver;
//import org.apache.commons.lang3.StringUtils;
//
///**
// * 实现XML与Java对象的相互转换<br/>
// * 1.通过XStream实现<br/>
// * <p/>
// * 作者：LiZW <br/>
// * 创建时间：2016-4-29 20:00 <br/>
// */
//public class XStreamMapper {
//    /**
//     * 使用SAX解析XML的Mapper，性能较高<br/>
//     */
//    private static final XStreamMapper SAX_XStream;
//
//    /**
//     * 使用XPP3解析XML的Mapper，依赖XPP3库<br/>
//     */
//    private static final XStreamMapper XPP3_XStream;
//
//    static {
//        SAX_XStream = new XStreamMapper(new StaxDriver());
//        XPP3_XStream = new XStreamMapper(null);
//    }
//
//    /**
//     * XStream核心Mapper类
//     */
//    private final XStream xstream;
//
//    /**
//     * 构造函数
//     *
//     * @param driver xml解析驱动，可为空默认为：XPP3
//     */
//    private XStreamMapper(HierarchicalStreamDriver driver) {
//        if (null == driver) {
//            // 需要XPP3库
//            xstream = new XStream();
//        } else {
//            xstream = new XStream(driver);
//        }
//    }
//
//    /**
//     * 返回一个使用SAX解析XML的Mapper，性能较高
//     *
//     * @return 返回一个使用SAX解析XML的Mapper，性能较高
//     */
//    public static XStreamMapper getSaxXStream() {
//        return SAX_XStream;
//    }
//
//    /**
//     * 返回一个使用XPP3解析XML的Mapper，依赖XPP3库
//     *
//     * @return 返回一个使用XPP3解析XML的Mapper，依赖XPP3库
//     */
//    public static XStreamMapper getXpp3XStream() {
//        return XPP3_XStream;
//    }
//
//    /**
//     * 对象序列化成XML字符串
//     *
//     * @param object 需要序列化xml的对象
//     * @return 返回xml
//     */
//    public String toXml(Object object) {
//        if (null == object) {
//            return null;
//        }
//        return xstream.toXML(object);
//    }
//
//    /**
//     * XML字符串反序列化成对象
//     *
//     * @param xmlString XML字符串
//     * @param <T>       返回的对象类型
//     * @return 返回的对象
//     */
//    @SuppressWarnings("unchecked")
//    public <T> T fromXml(String xmlString) {
//        if (StringUtils.isBlank(xmlString)) {
//            return null;
//        }
//        return (T) xstream.fromXML(xmlString);
//    }
//
//    /**
//     * 当XML里只含有Bean的部分属性时，更新一个已存在Bean，只覆盖该部分的属性
//     *
//     * @param xmlString XML字符串
//     * @param object    需要更新的对象
//     * @return 操作成功返回true
//     */
//    public boolean update(String xmlString, Object object) {
//        try {
//            xstream.fromXML(xmlString, object);
//            return true;
//        } catch (Exception ignored) {
//            return false;
//        }
//    }
//}
