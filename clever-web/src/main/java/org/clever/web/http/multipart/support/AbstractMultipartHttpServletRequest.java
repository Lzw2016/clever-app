//package org.clever.web.http.multipart.support;
//
//import org.clever.util.LinkedMultiValueMap;
//import org.clever.util.MultiValueMap;
//import org.clever.web.http.HttpHeaders;
//import org.clever.web.http.HttpMethod;
//import org.clever.web.http.multipart.MultipartFile;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletRequestWrapper;
//import java.util.*;
//
//
///**
// * MultipartHttpServletRequest 接口的抽象基础实现。
// * 提供对预生成的 MultipartFile 实例的管理。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/11 15:31 <br/>
// */
//public abstract class AbstractMultipartHttpServletRequest extends HttpServletRequestWrapper implements MultipartHttpServletRequest {
//    private MultiValueMap<String, MultipartFile> multipartFiles;
//
//    /**
//     * 将给定的 HttpServletRequest 包装在 MultipartHttpServletRequest 中。
//     *
//     * @param request 被包装的请求
//     */
//    protected AbstractMultipartHttpServletRequest(HttpServletRequest request) {
//        super(request);
//    }
//
//    @Override
//    public HttpServletRequest getRequest() {
//        return (HttpServletRequest) super.getRequest();
//    }
//
//    @Override
//    public HttpMethod getRequestMethod() {
//        return HttpMethod.resolve(getRequest().getMethod());
//    }
//
//    @Override
//    public HttpHeaders getRequestHeaders() {
//        HttpHeaders headers = new HttpHeaders();
//        Enumeration<String> headerNames = getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            headers.put(headerName, Collections.list(getHeaders(headerName)));
//        }
//        return headers;
//    }
//
//    @Override
//    public Iterator<String> getFileNames() {
//        return getMultipartFiles().keySet().iterator();
//    }
//
//    @Override
//    public MultipartFile getFile(String name) {
//        return getMultipartFiles().getFirst(name);
//    }
//
//    @Override
//    public List<MultipartFile> getFiles(String name) {
//        List<MultipartFile> multipartFiles = getMultipartFiles().get(name);
//        if (multipartFiles != null) {
//            return multipartFiles;
//        } else {
//            return Collections.emptyList();
//        }
//    }
//
//    @Override
//    public Map<String, MultipartFile> getFileMap() {
//        return getMultipartFiles().toSingleValueMap();
//    }
//
//    @Override
//    public MultiValueMap<String, MultipartFile> getMultiFileMap() {
//        return getMultipartFiles();
//    }
//
//    /**
//     * 确定底层的多部分请求是否已得到解决。
//     *
//     * @return {@code true} 急切初始化或延迟触发时，{@code false} 延迟解析请求在访问任何参数或多部分文件之前中止的情况
//     * @see #getMultipartFiles()
//     */
//    public boolean isResolved() {
//        return (this.multipartFiles != null);
//    }
//
//    /**
//     * 设置一个 Map，参数名称作为键，MultipartFile 对象列表作为值。
//     * 在初始化时由子类调用。
//     */
//    protected final void setMultipartFiles(MultiValueMap<String, MultipartFile> multipartFiles) {
//        this.multipartFiles = new LinkedMultiValueMap<>(Collections.unmodifiableMap(multipartFiles));
//    }
//
//    /**
//     * 获取用于检索的 MultipartFile 映射，必要时对其进行惰性初始化。
//     *
//     * @see #initializeMultipart()
//     */
//    protected MultiValueMap<String, MultipartFile> getMultipartFiles() {
//        if (this.multipartFiles == null) {
//            initializeMultipart();
//        }
//        return this.multipartFiles;
//    }
//
//    /**
//     * 如果可能，延迟初始化多部分请求。
//     * 仅在尚未急切初始化时调用。
//     */
//    protected void initializeMultipart() {
//        throw new IllegalStateException("Multipart request not initialized");
//    }
//}
