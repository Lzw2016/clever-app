//package org.clever.data.jdbc.support;
//
//import org.clever.core.Conv;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2022/03/03 17:35 <br/>
// */
//public class OracleLog {
//    private static final ThreadLocal<Boolean> ENABLE = new ThreadLocal<>();
//    private static final ThreadLocal<StringBuilder> buffer = new ThreadLocal<>();
//
//    /**
//     * 启用Oracle服务端日志(dbms_output)
//     */
//    public static void enable() {
//        ENABLE.set(true);
//    }
//
//    /**
//     * 禁用Oracle服务端日志(dbms_output)
//     */
//    public static void disable() {
//        ENABLE.set(false);
//    }
//
//    /**
//     * 是否启用Oracle服务端日志(dbms_output)
//     */
//    public static boolean isEnable() {
//        Boolean enable = ENABLE.get();
//        return enable != null && enable;
//    }
//
//    public static void clear() {
//        ENABLE.remove();
//    }
//
//    public static void append(Object content) {
//        StringBuilder sb = buffer.get();
//        if (sb == null) {
//            sb = new StringBuilder();
//            buffer.set(sb);
//        }
//        sb.append(Conv.asString(content)).append("\n");
//    }
//
//    public static String getAndClear() {
//        StringBuilder sb = buffer.get();
//        if (sb == null) {
//            return "";
//        }
//        buffer.set(new StringBuilder());
//        return sb.toString();
//    }
//
//    public static String get() {
//        StringBuilder sb = buffer.get();
//        if (sb == null) {
//            return "";
//        }
//        return sb.toString();
//    }
//}
