package org.clever.web.utils;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * 缓存请求数据(不会缓存文件上传的请求)
 * 参考 org.springframework.web.util.ContentCachingRequestWrapper
 * 作者：lizw <br/>
 * 创建时间：2021/12/12 14:06 <br/>
 */
public class HttpRequestCachingWrapper extends HttpServletRequestWrapper {
    private static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String MULTIPART_FORM_DATA_TYPE = "multipart/form-data";

    public static HttpRequestCachingWrapper wrapper(HttpServletRequest request) {
        if (request instanceof HttpRequestCachingWrapper) {
            return (HttpRequestCachingWrapper) request;
        }
        return new HttpRequestCachingWrapper(request);
    }

    private final String bodyString;
    private final ServletInputCachingStream inputStream;
    private final StringReader reader;

    @SneakyThrows
    public HttpRequestCachingWrapper(HttpServletRequest request) {
        super(request);
        if (StringUtils.isBlank(request.getCharacterEncoding())) {
            request.setCharacterEncoding(DEFAULT_CHARACTER_ENCODING);
        }
        final String characterEncoding = StringUtils.trim(request.getCharacterEncoding());
        if (isUploadFile() || isMultipartFormPost() || isFormPost()) {
            byte[] data = getRequestParameters();
            inputStream = new ServletInputCachingStream(data);
            bodyString = new String(data, characterEncoding);
        } else {
            // bodyString = IOUtils.toString(request.getReader());
            bodyString = IOUtils.toString(request.getInputStream(), characterEncoding);
            inputStream = new ServletInputCachingStream(bodyString.getBytes(characterEncoding));
        }
        reader = new StringReader(bodyString);
        mark();
    }

    @SneakyThrows
    private boolean isUploadFile() {
        String contentType = getContentType();
        boolean uploadFile = (contentType != null && contentType.contains(MULTIPART_FORM_DATA_TYPE) && HttpMethod.POST.matches(getMethod()));
        if (uploadFile) {
            boolean hasFile = false;
            Collection<Part> parts = getParts();
            for (Part part : parts) {
                String headerValue = part.getHeader(HttpHeaders.CONTENT_DISPOSITION);
                ContentDisposition disposition = ContentDisposition.parse(headerValue);
                String filename = disposition.getFilename();
                if (filename != null) {
                    hasFile = true;
                    break;
                }
            }
            uploadFile = hasFile;
        }
        return uploadFile;
    }

    private boolean isMultipartFormPost() {
        String contentType = getContentType();
        return (contentType != null && contentType.contains(MULTIPART_FORM_DATA_TYPE) && HttpMethod.POST.matches(getMethod()));
    }

    private boolean isFormPost() {
        String contentType = getContentType();
        return (contentType != null && contentType.contains(FORM_CONTENT_TYPE) && HttpMethod.POST.matches(getMethod()));
    }

    private byte[] getRequestParameters() {
        int contentLength = getContentLength();
        contentLength = contentLength >= 0 ? contentLength : 1024;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream(contentLength);
        try {
            String requestEncoding = getCharacterEncoding();
            Map<String, String[]> form = super.getParameterMap();
            for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
                String name = nameIterator.next();
                List<String> values = Arrays.asList(form.get(name));
                for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
                    String value = valueIterator.next();
                    byteStream.write(URLEncoder.encode(name, requestEncoding).getBytes());
                    if (value != null) {
                        byteStream.write('=');
                        byteStream.write(URLEncoder.encode(value, requestEncoding).getBytes());
                        if (valueIterator.hasNext()) {
                            byteStream.write('&');
                        }
                    }
                }
                if (nameIterator.hasNext()) {
                    byteStream.write('&');
                }
            }
            return byteStream.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write request parameters to cached content", ex);
        } finally {
            IOUtils.closeQuietly(byteStream);
        }
    }

    public String getBody() {
        return bodyString;
    }

    @SneakyThrows
    public void mark() {
        inputStream.mark(0);
        reader.mark(0);
    }

    @SneakyThrows
    public void reset() {
        inputStream.reset();
        reader.reset();
    }

    @Override
    public ServletInputStream getInputStream() {
        return inputStream;
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(reader);
    }

    /**
     * 参考: org.springframework.web.servlet.function.DefaultServerRequestBuilder.BodyInputStream
     */
    private static class ServletInputCachingStream extends ServletInputStream {
        private final ByteArrayInputStream delegate;

        public ServletInputCachingStream(byte[] body) {
            delegate = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read() {
            return delegate.read();
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) {
            return delegate.read(b, off, len);
        }

        @Override
        public int read(byte @NotNull [] b) throws IOException {
            return delegate.read(b);
        }

        @Override
        public long skip(long n) {
            return delegate.skip(n);
        }

        @Override
        public int available() {
            return this.delegate.available();
        }

        @Override
        public void close() throws IOException {
            this.delegate.close();
        }

        @Override
        public synchronized void mark(int readLimit) {
            this.delegate.mark(readLimit);
        }

        @Override
        public synchronized void reset() {
            this.delegate.reset();
        }

        @Override
        public boolean markSupported() {
            return this.delegate.markSupported();
        }
    }
}
