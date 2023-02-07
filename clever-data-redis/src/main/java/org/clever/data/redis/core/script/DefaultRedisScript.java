package org.clever.data.redis.core.script;

import org.clever.core.io.Resource;
import org.clever.scripting.ScriptSource;
import org.clever.scripting.support.ResourceScriptSource;
import org.clever.scripting.support.StaticScriptSource;
import org.clever.util.Assert;

import java.io.IOException;

/**
 * {@link RedisScript} 的默认实现。
 * 委托底层 {@link ScriptSource} 检索脚本文本并检测脚本是否已被修改（因此应该重新计算 SHA1）。此类最好用作单例，以避免在每次脚本执行时重新计算 SHA1。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/31 10:18 <br/>
 *
 * @param <T> 脚本结果类型。应该是 Long、Boolean、List 或反序列化值类型之一。如果脚本返回一次性状态（即“OK”），则可以为 null
 */
public class DefaultRedisScript<T> implements RedisScript<T> {
    private final Object shaModifiedMonitor = new Object();
    private ScriptSource scriptSource;
    private String sha1;
    private Class<T> resultType;

    /**
     * 创建一个新的 {@link DefaultRedisScript}
     */
    public DefaultRedisScript() {
    }

    /**
     * 创建一个新的 {@link DefaultRedisScript}
     *
     * @param script 不得为 {@literal null}
     */
    public DefaultRedisScript(String script) {
        this(script, null);
    }

    /**
     * 创建一个新的 {@link DefaultRedisScript}
     *
     * @param script     不得为 {@literal null}
     * @param resultType 可以是 {@literal null}。
     */
    public DefaultRedisScript(String script, Class<T> resultType) {
        this.setScriptText(script);
        this.resultType = resultType;
    }

    public void afterPropertiesSet() {
        Assert.state(this.scriptSource != null, "Either script, script location," + " or script source is required");
    }

    public String getSha1() {
        synchronized (shaModifiedMonitor) {
            if (sha1 == null || scriptSource.isModified()) {
                this.sha1 = DigestUtils.sha1DigestAsHex(getScriptAsString());
            }
            return sha1;
        }
    }

    public Class<T> getResultType() {
        return this.resultType;
    }

    public String getScriptAsString() {
        try {
            return scriptSource.getScriptAsString();
        } catch (IOException e) {
            throw new ScriptingException("Error reading script text", e);
        }
    }

    /**
     * @param resultType 脚本结果类型。应该是 Long、Boolean、List 或反序列化值类型之一。如果脚本返回一次性状态（即“OK”），则可以是 {@literal null}
     */
    public void setResultType(Class<T> resultType) {
        this.resultType = resultType;
    }

    /**
     * @param scriptText 脚本文本
     */
    public void setScriptText(String scriptText) {
        this.scriptSource = new StaticScriptSource(scriptText);
    }

    /**
     * @param scriptLocation 脚本的位置
     */
    public void setLocation(Resource scriptLocation) {
        this.scriptSource = new ResourceScriptSource(scriptLocation);
    }

    /**
     * @param scriptSource 指向脚本的 {@link ScriptSource}
     */
    public void setScriptSource(ScriptSource scriptSource) {
        this.scriptSource = scriptSource;
    }
}
