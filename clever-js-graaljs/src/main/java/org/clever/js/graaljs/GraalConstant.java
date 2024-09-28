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
     * ECMAScript Version: 13 (ES2022)，参考：<a href="https://www.graalvm.org/latest/reference-manual/js/Options/">GraalJS 引擎选项</a>
     */
    String ECMASCRIPT_VERSION = "13";

    /**
     * JS 语言ID
     */
    String JS_LANGUAGE_ID = "js";
}
