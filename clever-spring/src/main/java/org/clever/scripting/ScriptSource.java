package org.clever.scripting;

import java.io.IOException;

/**
 * 定义脚本源的接口。跟踪基础脚本是否已修改。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:19 <br/>
 */
public interface ScriptSource {
    /**
     * 以字符串形式检索当前脚本源文本
     *
     * @return 脚本文本
     * @throws IOException 如果脚本检索失败
     */
    String getScriptAsString() throws IOException;

    /**
     * 指示自上次调用 {@link #getScriptAsString()} 以来是否修改了基础脚本数据。
     * 如果尚未读取脚本，则返回 {@code true}
     *
     * @return 脚本数据是否被修改
     */
    boolean isModified();

    /**
     * 确定底层脚本的类名
     *
     * @return 建议的类名，如果没有可用的 {@code null}
     */
    String suggestedClassName();
}
