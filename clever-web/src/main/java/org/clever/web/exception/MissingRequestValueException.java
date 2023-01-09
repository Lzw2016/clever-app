//package org.clever.web.exception;
//
//import org.clever.web.exception.ServletRequestBindingException;
//
///**
// * Base class for {@link ServletRequestBindingException} exceptions that could
// * not bind because the request value is required but is either missing or
// * otherwise resolves to {@code null} after conversion.
// *
// * 作者：lizw <br/>
// * 创建时间：2023/01/04 22:59 <br/>
// */
//public class MissingRequestValueException extends ServletRequestBindingException {
//
//    private final boolean missingAfterConversion;
//
//
//    public MissingRequestValueException(String msg) {
//        this(msg, false);
//    }
//
//    public MissingRequestValueException(String msg, boolean missingAfterConversion) {
//        super(msg);
//        this.missingAfterConversion = missingAfterConversion;
//    }
//
//
//    /**
//     * Whether the request value was present but converted to {@code null}, e.g. via
//     * {@code org.springframework.core.convert.support.IdToEntityConverter}.
//     */
//    public boolean isMissingAfterConversion() {
//        return this.missingAfterConversion;
//    }
//
//}
