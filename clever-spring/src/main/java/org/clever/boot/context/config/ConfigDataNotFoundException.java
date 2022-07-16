package org.clever.boot.context.config;

import org.clever.boot.origin.OriginProvider;

/**
 * 找不到 {@link ConfigData} 时抛出 {@link ConfigDataNotFoundException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:19 <br/>
 */
public abstract class ConfigDataNotFoundException extends ConfigDataException implements OriginProvider {
    /**
     * 创建一个新的 {@link ConfigDataNotFoundException} 实例
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    ConfigDataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 返回无法找到的实际引用项的描述
     *
     * @return 引用项目的描述
     */
    public abstract String getReferenceDescription();
}
