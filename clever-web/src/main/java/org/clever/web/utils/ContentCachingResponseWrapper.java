//package org.clever.web.utils;
//
//import org.clever.util.FastByteArrayOutputStream;
//import org.clever.web.http.HttpHeaders;
//
//import javax.servlet.ServletOutputStream;
//import javax.servlet.WriteListener;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpServletResponseWrapper;
//import java.io.*;
//
///**
// * {@link javax.servlet.http.HttpServletResponse} 包装器，
// * 用于缓存写入 {@linkplain #getOutputStream() 输出流} 和 {@linkplain #getWriter() writer} 的所有内容，
// * 并允许通过 {@link #getContentAsByteArray() byte array} 检索此内容。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/02/23 19:33 <br/>
// *
// * @see HttpRequestCachingWrapper
// */
//public class ContentCachingResponseWrapper extends HttpServletResponseWrapper {
//    private final FastByteArrayOutputStream content = new FastByteArrayOutputStream(1024);
//    private ServletOutputStream outputStream;
//    private PrintWriter writer;
//    private Integer contentLength;
//
//    /**
//     * 为给定的 servlet 响应创建新的 ContentCachingResponseWrapper
//     *
//     * @param response 原始 servlet 响应
//     */
//    public ContentCachingResponseWrapper(HttpServletResponse response) {
//        super(response);
//    }
//
//    @Override
//    public void sendError(int sc) throws IOException {
//        copyBodyToResponse(false);
//        try {
//            super.sendError(sc);
//        } catch (IllegalStateException ex) {
//            // 当调用太晚时可能在 Tomcat 上：回退到静默设置状态
//            super.setStatus(sc);
//        }
//    }
//
//    @SuppressWarnings("deprecation")
//    @Override
//    public void sendError(int sc, String msg) throws IOException {
//        copyBodyToResponse(false);
//        try {
//            super.sendError(sc, msg);
//        } catch (IllegalStateException ex) {
//            // 当调用太晚时可能在 Tomcat 上：回退到静默设置状态
//            super.setStatus(sc, msg);
//        }
//    }
//
//    @Override
//    public void sendRedirect(String location) throws IOException {
//        copyBodyToResponse(false);
//        super.sendRedirect(location);
//    }
//
//    @Override
//    public ServletOutputStream getOutputStream() throws IOException {
//        if (this.outputStream == null) {
//            this.outputStream = new ResponseServletOutputStream(getResponse().getOutputStream());
//        }
//        return this.outputStream;
//    }
//
//    @Override
//    public PrintWriter getWriter() throws IOException {
//        if (this.writer == null) {
//            String characterEncoding = getCharacterEncoding();
//            this.writer = characterEncoding != null ?
//                    new ResponsePrintWriter(characterEncoding) :
//                    new ResponsePrintWriter(WebUtils.DEFAULT_CHARACTER_ENCODING);
//        }
//        return this.writer;
//    }
//
//    @Override
//    public void flushBuffer() throws IOException {
//        // 不要刷新基础响应，因为内容尚未复制到其中
//    }
//
//    @Override
//    public void setContentLength(int len) {
//        if (len > this.content.size()) {
//            this.content.resize(len);
//        }
//        this.contentLength = len;
//    }
//
//    // 在运行时覆盖 Servlet 3.1 setContentLengthLong(long)
//    @Override
//    public void setContentLengthLong(long len) {
//        if (len > Integer.MAX_VALUE) {
//            throw new IllegalArgumentException(
//                    "Content-Length exceeds ContentCachingResponseWrapper's maximum (" + Integer.MAX_VALUE + "): " + len
//            );
//        }
//        int lenInt = (int) len;
//        if (lenInt > this.content.size()) {
//            this.content.resize(lenInt);
//        }
//        this.contentLength = lenInt;
//    }
//
//    @Override
//    public void setBufferSize(int size) {
//        if (size > this.content.size()) {
//            this.content.resize(size);
//        }
//    }
//
//    @Override
//    public void resetBuffer() {
//        this.content.reset();
//    }
//
//    @Override
//    public void reset() {
//        super.reset();
//        this.content.reset();
//    }
//
//    /**
//     * 将缓存的响应内容作为字节数组返回
//     */
//    public byte[] getContentAsByteArray() {
//        return this.content.toByteArray();
//    }
//
//    /**
//     * 将 {@link InputStream} 返回到缓存的内容
//     */
//    public InputStream getContentInputStream() {
//        return this.content.getInputStream();
//    }
//
//    /**
//     * 返回缓存内容的当前大小
//     */
//    public int getContentSize() {
//        return this.content.size();
//    }
//
//    /**
//     * 将完整的缓存正文内容复制到响应
//     */
//    public void copyBodyToResponse() throws IOException {
//        copyBodyToResponse(true);
//    }
//
//    /**
//     * 将缓存的正文内容复制到响应
//     *
//     * @param complete 是否为完整的缓存正文内容设置相应的内容长度
//     */
//    protected void copyBodyToResponse(boolean complete) throws IOException {
//        if (this.content.size() > 0) {
//            HttpServletResponse rawResponse = (HttpServletResponse) getResponse();
//            if ((complete || this.contentLength != null) && !rawResponse.isCommitted()) {
//                if (rawResponse.getHeader(HttpHeaders.TRANSFER_ENCODING) == null) {
//                    rawResponse.setContentLength(complete ? this.content.size() : this.contentLength);
//                }
//                this.contentLength = null;
//            }
//            this.content.writeTo(rawResponse.getOutputStream());
//            this.content.reset();
//            if (complete) {
//                super.flushBuffer();
//            }
//        }
//    }
//
//    private class ResponseServletOutputStream extends ServletOutputStream {
//        private final ServletOutputStream os;
//
//        public ResponseServletOutputStream(ServletOutputStream os) {
//            this.os = os;
//        }
//
//        @Override
//        public void write(int b) throws IOException {
//            content.write(b);
//        }
//
//        @Override
//        public void write(byte[] b, int off, int len) throws IOException {
//            content.write(b, off, len);
//        }
//
//        @Override
//        public boolean isReady() {
//            return this.os.isReady();
//        }
//
//        @Override
//        public void setWriteListener(WriteListener writeListener) {
//            this.os.setWriteListener(writeListener);
//        }
//    }
//
//    private class ResponsePrintWriter extends PrintWriter {
//        public ResponsePrintWriter(String characterEncoding) throws UnsupportedEncodingException {
//            super(new OutputStreamWriter(content, characterEncoding));
//        }
//
//        @Override
//        public void write(char[] buf, int off, int len) {
//            super.write(buf, off, len);
//            super.flush();
//        }
//
//        @Override
//        public void write(String s, int off, int len) {
//            super.write(s, off, len);
//            super.flush();
//        }
//
//        @Override
//        public void write(int c) {
//            super.write(c);
//            super.flush();
//        }
//    }
//}
