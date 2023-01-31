package org.clever.scripting.support;

import org.clever.scripting.ScriptSource;
import org.clever.util.Assert;

/**
 * {@link ScriptSource} 接口的静态实现，封装包含脚本源文本的给定字符串。
 * 支持脚本字符串的编程更新
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:23 <br/>
 */
public class StaticScriptSource implements ScriptSource {
    private String script = "";
    private boolean modified;
    private String className;

    /**
     * 为给定的脚本创建一个新的 StaticScriptSource
     *
     * @param script 脚本字符串
     */
    public StaticScriptSource(String script) {
        setScript(script);
    }

    /**
     * 为给定的脚本创建一个新的 StaticScriptSource
     *
     * @param script    脚本字符串
     * @param className 脚本的建议类名（可能是 {@code null}）
     */
    public StaticScriptSource(String script, String className) {
        setScript(script);
        this.className = className;
    }

    /**
     * 设置一个新的脚本字符串，覆盖以前的脚本
     *
     * @param script 脚本字符串
     */
    public synchronized void setScript(String script) {
        Assert.hasText(script, "Script must not be empty");
        this.modified = !script.equals(this.script);
        this.script = script;
    }

    @Override
    public synchronized String getScriptAsString() {
        this.modified = false;
        return this.script;
    }

    @Override
    public synchronized boolean isModified() {
        return this.modified;
    }

    @Override
    public String suggestedClassName() {
        return this.className;
    }

    @Override
    public String toString() {
        return "static script" + (this.className != null ? " [" + this.className + "]" : "");
    }
}
