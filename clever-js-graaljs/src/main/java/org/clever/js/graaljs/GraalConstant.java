package org.clever.js.graaljs;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/21 09:09 <br/>
 */
public interface GraalConstant {
    /**
     * 错误的引擎名字(没有使用GraalVM compiler功能)
     */
    String ERROR_ENGINE_NAME = "Interpreted";

    /**
     * ECMAScript Version: 11 (ES2020)
     */
    String ECMASCRIPT_VERSION = "11";

    /**
     * JS 语言ID
     */
    String JS_LANGUAGE_ID = "js";
}
