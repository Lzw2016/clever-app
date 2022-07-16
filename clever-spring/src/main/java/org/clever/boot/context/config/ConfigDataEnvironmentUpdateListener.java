package org.clever.boot.context.config;

import org.clever.core.env.Environment;
import org.clever.core.env.PropertySource;

import java.util.EventListener;

/**
 * {@link EventListener} 监听由 {@code ConfigDataEnvironmentPostProcessor} 触发的 {@link Environment} 更新。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:40 <br/>
 */
public interface ConfigDataEnvironmentUpdateListener extends EventListener {
    /**
     * 一个什么都不做的 {@link ConfigDataEnvironmentUpdateListener}
     */
    ConfigDataEnvironmentUpdateListener NONE = new ConfigDataEnvironmentUpdateListener() {
    };

    /**
     * 在将新的 {@link PropertySource} 添加到 {@link Environment} 时调用。
     *
     * @param propertySource 添加的 {@link PropertySource}
     * @param location       原始 {@link ConfigDataLocation}
     * @param resource       {@link ConfigDataResource}
     */
    default void onPropertySourceAdded(PropertySource<?> propertySource, ConfigDataLocation location, ConfigDataResource resource) {
    }

    /**
     * Called when {@link Environment} profiles are set.
     *
     * @param profiles 正在设置的配置文件
     */
    default void onSetProfiles(Profiles profiles) {
    }
}
