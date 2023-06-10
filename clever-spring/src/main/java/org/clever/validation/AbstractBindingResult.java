package org.clever.validation;

import org.clever.beans.PropertyEditorRegistry;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyEditor;
import java.io.Serializable;
import java.util.*;

/**
 * {@link BindingResult} 接口及其超级接口 {@link Errors} 的抽象实现。
 * 封装 {@link ObjectError ObjectErrors} 和 {@link FieldError FieldErrors} 的公共管理。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:45 <br/>
 *
 * @see Errors
 */
public abstract class AbstractBindingResult extends AbstractErrors implements BindingResult, Serializable {
    private final String objectName;
    private MessageCodesResolver messageCodesResolver = new DefaultMessageCodesResolver();
    private final List<ObjectError> errors = new ArrayList<>();
    private final Map<String, Class<?>> fieldTypes = new HashMap<>();
    private final Map<String, Object> fieldValues = new HashMap<>();
    private final Set<String> suppressedFields = new HashSet<>();

    /**
     * @param objectName 目标对象的名称
     * @see DefaultMessageCodesResolver
     */
    protected AbstractBindingResult(String objectName) {
        this.objectName = objectName;
    }

    /**
     * 设置用于将错误解析为消息代码的策略。默认值为 DefaultMessageCodesResolver。
     *
     * @see DefaultMessageCodesResolver
     */
    public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
        Assert.notNull(messageCodesResolver, "MessageCodesResolver must not be null");
        this.messageCodesResolver = messageCodesResolver;
    }

    /**
     * 返回用于将错误解析为消息代码的策略。
     */
    public MessageCodesResolver getMessageCodesResolver() {
        return this.messageCodesResolver;
    }

    //---------------------------------------------------------------------
    // 错误接口的实现
    //---------------------------------------------------------------------

    @Override
    public String getObjectName() {
        return this.objectName;
    }

    @Override
    public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
        addError(new ObjectError(getObjectName(), resolveMessageCodes(errorCode), errorArgs, defaultMessage));
    }

    @Override
    public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
        if (!StringUtils.hasLength(getNestedPath()) && !StringUtils.hasLength(field)) {
            // 我们处于嵌套对象层次结构的顶部，因此当前级别不是字段而是顶部对象。
            // 我们能做的最好的事情就是在这里注册一个全局错误......
            reject(errorCode, errorArgs, defaultMessage);
            return;
        }
        String fixedField = fixedField(field);
        Object newVal = getActualFieldValue(fixedField);
        FieldError fe = new FieldError(getObjectName(), fixedField, newVal, false, resolveMessageCodes(errorCode, field), errorArgs, defaultMessage);
        addError(fe);
    }

    @Override
    public void addAllErrors(Errors errors) {
        if (!errors.getObjectName().equals(getObjectName())) {
            throw new IllegalArgumentException("Errors object needs to have same object name");
        }
        this.errors.addAll(errors.getAllErrors());
    }

    @Override
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    @Override
    public int getErrorCount() {
        return this.errors.size();
    }

    @Override
    public List<ObjectError> getAllErrors() {
        return Collections.unmodifiableList(this.errors);
    }

    @Override
    public List<ObjectError> getGlobalErrors() {
        List<ObjectError> result = new ArrayList<>();
        for (ObjectError objectError : this.errors) {
            if (!(objectError instanceof FieldError)) {
                result.add(objectError);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public ObjectError getGlobalError() {
        for (ObjectError objectError : this.errors) {
            if (!(objectError instanceof FieldError)) {
                return objectError;
            }
        }
        return null;
    }

    @Override
    public List<FieldError> getFieldErrors() {
        List<FieldError> result = new ArrayList<>();
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError) {
                result.add((FieldError) objectError);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public FieldError getFieldError() {
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError) {
                return (FieldError) objectError;
            }
        }
        return null;
    }

    @Override
    public List<FieldError> getFieldErrors(String field) {
        List<FieldError> result = new ArrayList<>();
        String fixedField = fixedField(field);
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError && isMatchingFieldError(fixedField, (FieldError) objectError)) {
                result.add((FieldError) objectError);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public FieldError getFieldError(String field) {
        String fixedField = fixedField(field);
        for (ObjectError objectError : this.errors) {
            if (objectError instanceof FieldError) {
                FieldError fieldError = (FieldError) objectError;
                if (isMatchingFieldError(fixedField, fieldError)) {
                    return fieldError;
                }
            }
        }
        return null;
    }

    @Override
    public Object getFieldValue(String field) {
        FieldError fieldError = getFieldError(field);
        // 出现错误时使用拒绝值，否则使用当前字段值。
        if (fieldError != null) {
            Object value = fieldError.getRejectedValue();
            // 不要对类型不匹配等绑定失败应用格式。
            return (fieldError.isBindingFailure() || getTarget() == null ? value : formatFieldValue(field, value));
        } else if (getTarget() != null) {
            Object value = getActualFieldValue(fixedField(field));
            return formatFieldValue(field, value);
        } else {
            return this.fieldValues.get(field);
        }
    }

    /**
     * 此默认实现根据实际字段值（如果有）确定类型。子类应该覆盖它以确定描述符的类型，即使是 {@code null} 值。
     *
     * @see #getActualFieldValue
     */
    @Override
    public Class<?> getFieldType(String field) {
        if (getTarget() != null) {
            Object value = getActualFieldValue(fixedField(field));
            if (value != null) {
                return value.getClass();
            }
        }
        return this.fieldTypes.get(field);
    }

    //---------------------------------------------------------------------
    // BindingResult 接口的实现
    //---------------------------------------------------------------------

    /**
     * 返回获得状态的模型映射，将错误实例公开为“{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName”和对象本身。
     * <p>请注意，每次调用此方法时都会构建 Map。向地图添加东西然后重新调用此方法将不起作用。
     *
     * @see #getObjectName
     * @see #MODEL_KEY_PREFIX
     */
    @Override
    public Map<String, Object> getModel() {
        Map<String, Object> model = new LinkedHashMap<>(2);
        // Mapping from name to target object.
        model.put(getObjectName(), getTarget());
        // Errors instance, even if no errors.
        model.put(MODEL_KEY_PREFIX + getObjectName(), this);
        return model;
    }

    @Override
    public Object getRawFieldValue(String field) {
        return (getTarget() != null ? getActualFieldValue(fixedField(field)) : null);
    }

    /**
     * 此实现委托给 {@link #getPropertyEditorRegistry() PropertyEditorRegistry} 的编辑器查找工具（如果可用）。
     */
    @Override
    public PropertyEditor findEditor(String field, Class<?> valueType) {
        PropertyEditorRegistry editorRegistry = getPropertyEditorRegistry();
        if (editorRegistry != null) {
            Class<?> valueTypeToUse = valueType;
            if (valueTypeToUse == null) {
                valueTypeToUse = getFieldType(field);
            }
            return editorRegistry.findCustomEditor(valueTypeToUse, fixedField(field));
        } else {
            return null;
        }
    }

    /**
     * 此实现返回 {@code null}。
     */
    @Override
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        return null;
    }

    @Override
    public String[] resolveMessageCodes(String errorCode) {
        return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName());
    }

    @Override
    public String[] resolveMessageCodes(String errorCode, String field) {
        return getMessageCodesResolver().resolveMessageCodes(errorCode, getObjectName(), fixedField(field), getFieldType(field));
    }

    @Override
    public void addError(ObjectError error) {
        this.errors.add(error);
    }

    @Override
    public void recordFieldValue(String field, Class<?> type, Object value) {
        this.fieldTypes.put(field, type);
        this.fieldValues.put(field, value);
    }

    /**
     * 将指定的不允许字段标记为禁止。
     * <p>数据绑定器为检测到的每个字段值调用此方法以针对不允许的字段。
     *
     * @see DataBinder#setAllowedFields
     */
    @Override
    public void recordSuppressedField(String field) {
        this.suppressedFields.add(field);
    }

    /**
     * 返回在绑定过程中被抑制的字段列表。
     * <p>可用于确定是否有任何字段值针对不允许的字段。
     *
     * @see DataBinder#setAllowedFields
     */
    @Override
    public String[] getSuppressedFields() {
        return StringUtils.toStringArray(this.suppressedFields);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BindingResult)) {
            return false;
        }
        BindingResult otherResult = (BindingResult) other;
        return getObjectName().equals(otherResult.getObjectName())
                && ObjectUtils.nullSafeEquals(getTarget(), otherResult.getTarget())
                && getAllErrors().equals(otherResult.getAllErrors());
    }

    @Override
    public int hashCode() {
        return getObjectName().hashCode();
    }

    //---------------------------------------------------------------------
    // 由子类实现/覆盖的模板方法
    //---------------------------------------------------------------------

    /**
     * 返回包装的目标对象。
     */
    @Override
    public abstract Object getTarget();

    /**
     * 提取给定字段的实际字段值。
     *
     * @param field 要检查的字段
     * @return 字段的当前值
     */
    protected abstract Object getActualFieldValue(String field);

    /**
     * 格式化指定字段的给定值。
     * <p>默认实现只是按原样返回字段值。
     *
     * @param field 要检查的字段
     * @param value 字段的值（除了绑定错误之外的拒绝值，或实际字段值）
     * @return 格式化值
     */
    protected Object formatFieldValue(String field, Object value) {
        return value;
    }
}
