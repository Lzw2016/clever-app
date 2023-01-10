package org.clever.web.exception;

import javax.servlet.ServletException;

/**
 * 当找不到由其名称标识的“multipart/form-data”请求的部分时引发。
 * <p>这可能是因为请求不是 multipart/form-data 请求，因为该部分不存在于请求中，
 * 或者因为 Web 应用程序没有正确配置以处理 multipart 请求，例如没有{@link MultipartResolver}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/10 21:51 <br/>
 */
public class MissingServletRequestPartException extends ServletException {
    private final String requestPartName;

    /**
     * MissingServletRequestPartException 的构造函数。
     *
     * @param requestPartName 多部分请求中缺失部分的名称
     */
    public MissingServletRequestPartException(String requestPartName) {
        super("Required request part '" + requestPartName + "' is not present");
        this.requestPartName = requestPartName;
    }

    /**
     * 返回多部分请求的违规部分的名称。
     */
    public String getRequestPartName() {
        return this.requestPartName;
    }
}
