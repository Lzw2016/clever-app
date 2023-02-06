package org.clever.beans.factory;

/**
 * 需要由 bean 实现的接口：例如执行自定义初始化，或仅检查是否已设置所有必需属性。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/06 15:50 <br/>
 */
public interface InitializingBean {
    /**
     * 在设置所有 bean 属性。
     * <p>此方法允许 bean 实例在设置所有 bean 属性后执行其整体配置和最终初始化的验证。
     *
     * @throws Exception 如果配置错误（例如未能设置基本属性）或由于任何其他原因初始化失败
     */
    void afterPropertiesSet() throws Exception;
}
