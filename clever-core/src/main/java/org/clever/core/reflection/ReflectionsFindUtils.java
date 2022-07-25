//package org.clever.core.reflection;
//
//import lombok.extern.slf4j.Slf4j;
//import org.reflections.Reflections;
//import org.reflections.scanners.Scanners;
//import org.reflections.util.ClasspathHelper;
//import org.reflections.util.ConfigurationBuilder;
//
///**
// * 反射工具类，使用org.reflections实现<br/>
// * 用于在运行时搜索类、方法、属性、注解等元数据<br/>
// * <p/>
// * 作者：LiZW <br/>
// * 创建时间：2016-5-22 21:52 <br/>
// */
//public class ReflectionsFindUtils {
//    /**
//     * 反射操作类
//     */
//    private static final Reflections REFLECTIONS;
//
//    static {
//        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
//        configurationBuilder.setUrls(ClasspathHelper.forPackage("com.yvan"));
//        configurationBuilder.setScanners(Scanners.SubTypes, Scanners.TypesAnnotated);
//        REFLECTIONS = new Reflections(configurationBuilder);
//    }
//}
