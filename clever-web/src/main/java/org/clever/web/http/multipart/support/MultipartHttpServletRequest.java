//package org.clever.web.http.multipart.support;
//
//import org.clever.web.http.HttpHeaders;
//import org.clever.web.http.HttpMethod;
//import org.clever.web.http.multipart.MultipartRequest;
//
//import javax.servlet.http.HttpServletRequest;
//
///**
// * 提供额外的方法来处理 servlet 请求中的多部分内容，从而允许访问上传的文件。
// * 实现还需要重写用于参数访问的标准 {@link javax.servlet.ServletRequest} 方法，使多部分参数可用。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2023/01/11 15:33 <br/>
// */
//public interface MultipartHttpServletRequest extends HttpServletRequest, MultipartRequest {
//    /**
//     * 将此请求的方法作为方便的 HttpMethod 实例返回。
//     */
//    HttpMethod getRequestMethod();
//
//    /**
//     * 将此请求的标头作为方便的 HttpHeaders 实例返回。
//     */
//    HttpHeaders getRequestHeaders();
//
//    /**
//     * 返回多部分请求的指定部分的标头。
//     * <p>如果底层实现支持访问部分标头，则返回所有标头。否则，例如对于文件上传，返回的标头可能会公开“Content-Type”（如果可用）。
//     */
//    HttpHeaders getMultipartHeaders(String paramOrFileName);
//}
