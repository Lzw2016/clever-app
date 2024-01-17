package org.clever.js.graaljs;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/21 09:09 <br/>
 */
public interface GraalConstant {
    /**
     * 错误的引擎名字(没有使用GraalVM compiler功能)
     */
    String Error_Engine_Name = "Interpreted";

    /**
     * ECMAScript Version: 11 (ES2020)
     */
    String ECMAScript_Version = "11";

    /**
     * JS 语言ID
     */
    String Js_Language_Id = "js";
}
