//package org.clever.web.http.multipart.support;
//
//import org.clever.beans.MutablePropertyValues;
//import org.clever.util.LinkedMultiValueMap;
//import org.clever.util.MultiValueMap;
//import org.clever.web.exception.MultipartException;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.Part;
//import java.util.ArrayList;
//import java.util.List;
//
///**
// * 用于标准 Servlet {@link Part} 处理的实用方法。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/06/08 21:56 <br/>
// *
// * @see HttpServletRequest#getParts()
// */
//public abstract class StandardServletPartUtils {
//    /**
//     * 从给定的 servlet 请求中检索所有部分。
//     *
//     * @param request 服务请求
//     * @return MultiValueMap 中的部分
//     * @throws MultipartException 万一发生故障
//     */
//    public static MultiValueMap<String, Part> getParts(HttpServletRequest request) throws MultipartException {
//        try {
//            MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();
//            for (Part part : request.getParts()) {
//                parts.add(part.getName(), part);
//            }
//            return parts;
//        } catch (Exception ex) {
//            throw new MultipartException("Failed to get request parts", ex);
//        }
//    }
//
//    /**
//     * 从给定的 servlet 请求中检索具有给定名称的所有部分。
//     *
//     * @param request 服务请求
//     * @param name    要查找的名称
//     * @return MultiValueMap 中的部分
//     * @throws MultipartException 万一发生故障
//     */
//    public static List<Part> getParts(HttpServletRequest request, String name) throws MultipartException {
//        try {
//            List<Part> parts = new ArrayList<>(1);
//            for (Part part : request.getParts()) {
//                if (part.getName().equals(name)) {
//                    parts.add(part);
//                }
//            }
//            return parts;
//        } catch (Exception ex) {
//            throw new MultipartException("Failed to get request parts", ex);
//        }
//    }
//
//    /**
//     * 绑定来自给定 servlet 请求的所有部分。
//     *
//     * @param request   服务请求
//     * @param mpvs      要绑定的属性值
//     * @param bindEmpty 是否也绑定空的部分
//     * @throws MultipartException 万一发生故障
//     */
//    public static void bindParts(HttpServletRequest request, MutablePropertyValues mpvs, boolean bindEmpty) throws MultipartException {
//        getParts(request).forEach((key, values) -> {
//            if (values.size() == 1) {
//                Part part = values.get(0);
//                if (bindEmpty || part.getSize() > 0) {
//                    mpvs.add(key, part);
//                }
//            } else {
//                mpvs.add(key, values);
//            }
//        });
//    }
//}
