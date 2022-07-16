package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.util.Assert;

/**
 * {@link BindHandler} 实现的抽象基类。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/09 23:24 <br/>
 */
public abstract class AbstractBindHandler implements BindHandler {
    private final BindHandler parent;

    /**
     * 创建新的绑定处理程序实例。
     */
    public AbstractBindHandler() {
        this(BindHandler.DEFAULT);
    }

    /**
     * 创建具有特定父级的新绑定处理程序实例。
     *
     * @param parent 父处理程序
     */
    public AbstractBindHandler(BindHandler parent) {
        Assert.notNull(parent, "Parent must not be null");
        this.parent = parent;
    }

    @Override
    public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
        return this.parent.onStart(name, target, context);
    }

    @Override
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
        return this.parent.onSuccess(name, target, context, result);
    }

    @Override
    public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error) throws Exception {
        return this.parent.onFailure(name, target, context, error);
    }

    @Override
    public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) throws Exception {
        this.parent.onFinish(name, target, context, result);
    }
}
