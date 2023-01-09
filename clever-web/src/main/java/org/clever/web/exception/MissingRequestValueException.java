package org.clever.web.exception;

/**
 * 无法绑定的 {@link ServletRequestBindingException} 异常的基类，
 * 因为请求值是必需的但丢失或以其他方式在转换后解析为 {@code null}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:59 <br/>
 */
public class MissingRequestValueException extends ServletRequestBindingException {
    private final boolean missingAfterConversion;

    public MissingRequestValueException(String msg) {
        this(msg, false);
    }

    public MissingRequestValueException(String msg, boolean missingAfterConversion) {
        super(msg);
        this.missingAfterConversion = missingAfterConversion;
    }

    /**
     * 请求值是否存在但已转换为 {@code null}
     */
    public boolean isMissingAfterConversion() {
        return this.missingAfterConversion;
    }
}
