package org.clever.groovy.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/27 11:33 <br/>
 */
public class FilePathUtils {
    public static final String DOT = ".";
    public static final String GROOVY_SUFFIX = ".groovy";
    public static final String JAVA_SUFFIX = ".java";

    public static String getClassPath(String classFullName) {
        String classPath;
        if (StringUtils.endsWithIgnoreCase(classFullName, GROOVY_SUFFIX)) {
            // .groovy 脚本
            int index = classFullName.length() - GROOVY_SUFFIX.length();
            String path = StringUtils.substring(classFullName, 0, index);
            String suffix = StringUtils.substring(classFullName, index);
            classPath = StringUtils.replace(path, DOT, File.separator) + suffix;
        } else if (StringUtils.endsWithIgnoreCase(classFullName, JAVA_SUFFIX)) {
            // .java 脚本
            int index = classFullName.length() - JAVA_SUFFIX.length();
            String path = StringUtils.substring(classFullName, 0, index);
            String suffix = StringUtils.substring(classFullName, index);
            classPath = StringUtils.replace(path, DOT, File.separator) + suffix;
        } else {
            // 默认 .groovy 脚本
            classPath = StringUtils.replace(classFullName, DOT, File.separator) + GROOVY_SUFFIX;
        }
        return classPath;
    }

    public static String getClassName(String classFullName) {
        String className = classFullName;
        if (StringUtils.endsWithIgnoreCase(classFullName, GROOVY_SUFFIX)) {
            // .groovy 脚本
            int index = classFullName.length() - GROOVY_SUFFIX.length();
            className = StringUtils.substring(classFullName, 0, index);
        } else if (StringUtils.endsWithIgnoreCase(classFullName, JAVA_SUFFIX)) {
            // .java 脚本
            int index = classFullName.length() - JAVA_SUFFIX.length();
            className = StringUtils.substring(classFullName, 0, index);
        }
        return className;
    }
}
