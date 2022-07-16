package org.clever.boot.logging.logback;

import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.util.OptionHelper;
import org.clever.core.env.Environment;
import org.xml.sax.Attributes;

/**
 * 支持 {@code <cleverProperty>} 标记的Logback {@link Action}。
 * 允许从 environment 中获取logback属性。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:13 <br/>
 */
class SpringPropertyAction extends Action {
    private static final String SOURCE_ATTRIBUTE = "source";
    private static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";

    private final Environment environment;

    SpringPropertyAction(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void begin(InterpretationContext context, String elementName, Attributes attributes) {
        String name = attributes.getValue(NAME_ATTRIBUTE);
        String source = attributes.getValue(SOURCE_ATTRIBUTE);
        Scope scope = ActionUtil.stringToScope(attributes.getValue(SCOPE_ATTRIBUTE));
        String defaultValue = attributes.getValue(DEFAULT_VALUE_ATTRIBUTE);
        if (OptionHelper.isEmpty(name) || OptionHelper.isEmpty(source)) {
            addError("The \"name\" and \"source\" attributes of <cleverProperty> must be set");
        }
        ActionUtil.setProperty(context, name, getValue(source, defaultValue), scope);
    }

    private String getValue(String source, String defaultValue) {
        if (this.environment == null) {
            addWarn("No clever Environment available to resolve " + source);
            return defaultValue;
        }
        return this.environment.getProperty(source, defaultValue);
    }

    @Override
    public void end(InterpretationContext context, String name) throws ActionException {
    }
}
