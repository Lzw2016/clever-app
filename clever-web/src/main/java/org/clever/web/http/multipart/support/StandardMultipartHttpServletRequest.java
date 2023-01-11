package org.clever.web.http.multipart.support;

import org.clever.util.FileCopyUtils;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;
import org.clever.web.exception.MaxUploadSizeExceededException;
import org.clever.web.exception.MultipartException;
import org.clever.web.http.ContentDisposition;
import org.clever.web.http.HttpHeaders;
import org.clever.web.http.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * MultipartHttpServletRequest 适配器，
 * 包装了 Servlet 3.0 HttpServletRequest 及其 Part 对象。
 * 参数通过本机请求的 getParameter 方法公开 - 我们这边没有任何自定义处理。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/11 15:28 <br/>
 */
public class StandardMultipartHttpServletRequest extends AbstractMultipartHttpServletRequest {
    private Set<String> multipartParameterNames;

    /**
     * 为给定的请求创建一个新的 StandardMultipartHttpServletRequest 包装器，立即解析多部分内容。
     *
     * @param request 被包装的请求
     * @throws MultipartException 如果解析失败
     */
    public StandardMultipartHttpServletRequest(HttpServletRequest request) throws MultipartException {
        this(request, false);
    }

    /**
     * 为给定的请求创建一个新的 StandardMultipartHttpServletRequest 包装器。
     *
     * @param request     被包装的请求
     * @param lazyParsing 是否应在首次访问多部分文件或参数时延迟触发多部分解析
     * @throws MultipartException 如果立即解析尝试失败
     */
    public StandardMultipartHttpServletRequest(HttpServletRequest request, boolean lazyParsing) throws MultipartException {
        super(request);
        if (!lazyParsing) {
            parseRequest(request);
        }
    }

    private void parseRequest(HttpServletRequest request) {
        try {
            Collection<Part> parts = request.getParts();
            this.multipartParameterNames = new LinkedHashSet<>(parts.size());
            MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(parts.size());
            for (Part part : parts) {
                String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
                ContentDisposition disposition = ContentDisposition.parse(headerValue);
                String filename = disposition.getFilename();
                if (filename != null) {
                    // if (filename.startsWith("=?") && filename.endsWith("?=")) {
                    //     filename = MimeDelegate.decode(filename);
                    // }
                    files.add(part.getName(), new StandardMultipartFile(part, filename));
                } else {
                    this.multipartParameterNames.add(part.getName());
                }
            }
            setMultipartFiles(files);
        } catch (Throwable ex) {
            handleParseFailure(ex);
        }
    }

    protected void handleParseFailure(Throwable ex) {
        String msg = ex.getMessage();
        if (msg != null) {
            msg = msg.toLowerCase();
            if (msg.contains("size") && msg.contains("exceed")) {
                throw new MaxUploadSizeExceededException(-1, ex);
            }
        }
        throw new MultipartException("Failed to parse multipart servlet request", ex);
    }

    @Override
    protected void initializeMultipart() {
        parseRequest(getRequest());
    }

    @Override
    public Enumeration<String> getParameterNames() {
        if (this.multipartParameterNames == null) {
            initializeMultipart();
        }
        if (this.multipartParameterNames.isEmpty()) {
            return super.getParameterNames();
        }
        // Servlet 3.0 getParameterNames() 不保证包含多部分表单项
        // （例如在 WebLogic 12 上）-> 需要将它们合并到这里以确保安全
        Set<String> paramNames = new LinkedHashSet<>();
        Enumeration<String> paramEnum = super.getParameterNames();
        while (paramEnum.hasMoreElements()) {
            paramNames.add(paramEnum.nextElement());
        }
        paramNames.addAll(this.multipartParameterNames);
        return Collections.enumeration(paramNames);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (this.multipartParameterNames == null) {
            initializeMultipart();
        }
        if (this.multipartParameterNames.isEmpty()) {
            return super.getParameterMap();
        }
        // Servlet 3.0 getParameterMap() 不保证包含多部分表单项
        // （例如在 WebLogic 12 上）-> 需要将它们合并到这里以确保安全
        Map<String, String[]> paramMap = new LinkedHashMap<>(super.getParameterMap());
        for (String paramName : this.multipartParameterNames) {
            if (!paramMap.containsKey(paramName)) {
                paramMap.put(paramName, getParameterValues(paramName));
            }
        }
        return paramMap;
    }

    @Override
    public String getMultipartContentType(String paramOrFileName) {
        try {
            Part part = getPart(paramOrFileName);
            return (part != null ? part.getContentType() : null);
        } catch (Throwable ex) {
            throw new MultipartException("Could not access multipart servlet request", ex);
        }
    }

    @Override
    public HttpHeaders getMultipartHeaders(String paramOrFileName) {
        try {
            Part part = getPart(paramOrFileName);
            if (part != null) {
                HttpHeaders headers = new HttpHeaders();
                for (String headerName : part.getHeaderNames()) {
                    headers.put(headerName, new ArrayList<>(part.getHeaders(headerName)));
                }
                return headers;
            } else {
                return null;
            }
        } catch (Throwable ex) {
            throw new MultipartException("Could not access multipart servlet request", ex);
        }
    }

    /**
     * MultipartFile 适配器，包装一个 Servlet 3.0 Part 对象。
     */
    private static class StandardMultipartFile implements MultipartFile, Serializable {
        private final Part part;
        private final String filename;

        public StandardMultipartFile(Part part, String filename) {
            this.part = part;
            this.filename = filename;
        }

        @Override
        public String getName() {
            return this.part.getName();
        }

        @Override
        public String getOriginalFilename() {
            return this.filename;
        }

        @Override
        public String getContentType() {
            return this.part.getContentType();
        }

        @Override
        public boolean isEmpty() {
            return (this.part.getSize() == 0);
        }

        @Override
        public long getSize() {
            return this.part.getSize();
        }

        @Override
        public byte[] getBytes() throws IOException {
            return FileCopyUtils.copyToByteArray(this.part.getInputStream());
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return this.part.getInputStream();
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            this.part.write(dest.getPath());
            if (dest.isAbsolute() && !dest.exists()) {
                // Servlet 3.0 Part.write 不保证支持绝对文件路径：
                // 可以将给定路径转换为临时目录中的相对位置（例如，在 Jetty 上，而 Tomcat 和 Undertow 检测绝对路径）
                // 至少我们从内存中卸载了文件；无论如何，它最终都会从临时目录中删除。
                // 为了我们的用户的目的，我们可以手动将其复制到请求的位置作为后备。
                FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest.toPath()));
            }
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            FileCopyUtils.copy(this.part.getInputStream(), Files.newOutputStream(dest));
        }
    }
}
