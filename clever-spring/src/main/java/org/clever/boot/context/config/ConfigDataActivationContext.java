package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.env.Environment;
import org.clever.core.style.ToStringCreator;

/**
 * 确定何时激活 {@link ConfigDataEnvironmentContributor contributed} {@link ConfigData} 时使用的上下文信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:43 <br/>
 */
class ConfigDataActivationContext {
    private final Profiles profiles;

    /**
     * 创建新的 {@link ConfigDataActivationContext} 在激活任何配置文件之前。
     *
     * @param environment 源环境
     * @param binder      提供访问相关配置数据 contributor 的活页夹
     */
    ConfigDataActivationContext(Environment environment, Binder binder) {
        this.profiles = null;
    }

    /**
     * 创建新的 {@link ConfigDataActivationContext}
     *
     * @param profiles 配置文件
     */
    ConfigDataActivationContext(Profiles profiles) {
        this.profiles = profiles;
    }

    /**
     * 返回具有特定配置文件的新 {@link ConfigDataActivationContext}
     *
     * @param profiles 配置文件
     * @return 一个新的 {@link ConfigDataActivationContext} 具有特定配置文件
     */
    ConfigDataActivationContext withProfiles(Profiles profiles) {
        return new ConfigDataActivationContext(profiles);
    }

    /**
     * 返回配置文件信息（如果可用）。
     *
     * @return 配置文件信息或空
     */
    Profiles getProfiles() {
        return this.profiles;
    }

    @Override
    public String toString() {
        ToStringCreator creator = new ToStringCreator(this);
        creator.append("profiles", this.profiles);
        return creator.toString();
    }
}
