package org.clever.validation;

import org.clever.beans.PropertyEditorRegistry;
import org.clever.util.Assert;

import java.beans.PropertyEditor;
import java.util.List;
import java.util.Map;

/**
 * 当绑定错误被认为是致命的时抛出。
 * 实现 {@link BindingResult} 接口（及其超级接口 {@link Errors}）以允许直接分析绑定错误。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:00 <br/>
 *
 * @see BindingResult
 * @see DataBinder#getBindingResult()
 * @see DataBinder#close()
 */
public class BindException extends Exception implements BindingResult {
    private final BindingResult bindingResult;

    /**
     * @param bindingResult 要包装的 BindingResult 实例
     */
    public BindException(BindingResult bindingResult) {
        Assert.notNull(bindingResult, "BindingResult must not be null");
        this.bindingResult = bindingResult;
    }

    /**
     * @param target     要绑定到的目标 bean
     * @param objectName 目标对象的名称
     * @see BeanPropertyBindingResult
     */
    public BindException(Object target, String objectName) {
        Assert.notNull(target, "Target object must not be null");
        this.bindingResult = new BeanPropertyBindingResult(target, objectName);
    }

    /**
     * 返回此 BindException 包装的 BindingResult。
     */
    public final BindingResult getBindingResult() {
        return this.bindingResult;
    }

    @Override
    public String getObjectName() {
        return this.bindingResult.getObjectName();
    }

    @Override
    public void setNestedPath(String nestedPath) {
        this.bindingResult.setNestedPath(nestedPath);
    }

    @Override
    public String getNestedPath() {
        return this.bindingResult.getNestedPath();
    }

    @Override
    public void pushNestedPath(String subPath) {
        this.bindingResult.pushNestedPath(subPath);
    }

    @Override
    public void popNestedPath() throws IllegalStateException {
        this.bindingResult.popNestedPath();
    }

    @Override
    public void reject(String errorCode) {
        this.bindingResult.reject(errorCode);
    }

    @Override
    public void reject(String errorCode, String defaultMessage) {
        this.bindingResult.reject(errorCode, defaultMessage);
    }

    @Override
    public void reject(String errorCode, Object[] errorArgs, String defaultMessage) {
        this.bindingResult.reject(errorCode, errorArgs, defaultMessage);
    }

    @Override
    public void rejectValue(String field, String errorCode) {
        this.bindingResult.rejectValue(field, errorCode);
    }

    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage) {
        this.bindingResult.rejectValue(field, errorCode, defaultMessage);
    }

    @Override
    public void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage) {
        this.bindingResult.rejectValue(field, errorCode, errorArgs, defaultMessage);
    }

    @Override
    public void addAllErrors(Errors errors) {
        this.bindingResult.addAllErrors(errors);
    }

    @Override
    public boolean hasErrors() {
        return this.bindingResult.hasErrors();
    }

    @Override
    public int getErrorCount() {
        return this.bindingResult.getErrorCount();
    }

    @Override
    public List<ObjectError> getAllErrors() {
        return this.bindingResult.getAllErrors();
    }

    @Override
    public boolean hasGlobalErrors() {
        return this.bindingResult.hasGlobalErrors();
    }

    @Override
    public int getGlobalErrorCount() {
        return this.bindingResult.getGlobalErrorCount();
    }

    @Override
    public List<ObjectError> getGlobalErrors() {
        return this.bindingResult.getGlobalErrors();
    }

    @Override
    public ObjectError getGlobalError() {
        return this.bindingResult.getGlobalError();
    }

    @Override
    public boolean hasFieldErrors() {
        return this.bindingResult.hasFieldErrors();
    }

    @Override
    public int getFieldErrorCount() {
        return this.bindingResult.getFieldErrorCount();
    }

    @Override
    public List<FieldError> getFieldErrors() {
        return this.bindingResult.getFieldErrors();
    }

    @Override
    public FieldError getFieldError() {
        return this.bindingResult.getFieldError();
    }

    @Override
    public boolean hasFieldErrors(String field) {
        return this.bindingResult.hasFieldErrors(field);
    }

    @Override
    public int getFieldErrorCount(String field) {
        return this.bindingResult.getFieldErrorCount(field);
    }

    @Override
    public List<FieldError> getFieldErrors(String field) {
        return this.bindingResult.getFieldErrors(field);
    }

    @Override
    public FieldError getFieldError(String field) {
        return this.bindingResult.getFieldError(field);
    }

    @Override
    public Object getFieldValue(String field) {
        return this.bindingResult.getFieldValue(field);
    }

    @Override
    public Class<?> getFieldType(String field) {
        return this.bindingResult.getFieldType(field);
    }

    @Override
    public Object getTarget() {
        return this.bindingResult.getTarget();
    }

    @Override
    public Map<String, Object> getModel() {
        return this.bindingResult.getModel();
    }

    @Override
    public Object getRawFieldValue(String field) {
        return this.bindingResult.getRawFieldValue(field);
    }

    @Override
    public PropertyEditor findEditor(String field, Class<?> valueType) {
        return this.bindingResult.findEditor(field, valueType);
    }

    @Override
    public PropertyEditorRegistry getPropertyEditorRegistry() {
        return this.bindingResult.getPropertyEditorRegistry();
    }

    @Override
    public String[] resolveMessageCodes(String errorCode) {
        return this.bindingResult.resolveMessageCodes(errorCode);
    }

    @Override
    public String[] resolveMessageCodes(String errorCode, String field) {
        return this.bindingResult.resolveMessageCodes(errorCode, field);
    }

    @Override
    public void addError(ObjectError error) {
        this.bindingResult.addError(error);
    }

    @Override
    public void recordFieldValue(String field, Class<?> type, Object value) {
        this.bindingResult.recordFieldValue(field, type, value);
    }

    @Override
    public void recordSuppressedField(String field) {
        this.bindingResult.recordSuppressedField(field);
    }

    @Override
    public String[] getSuppressedFields() {
        return this.bindingResult.getSuppressedFields();
    }

    /**
     * 返回有关此对象中保存的错误的诊断信息。
     */
    @Override
    public String getMessage() {
        return this.bindingResult.toString();
    }

    @Override
    public boolean equals(Object other) {
        return (this == other || this.bindingResult.equals(other));
    }

    @Override
    public int hashCode() {
        return this.bindingResult.hashCode();
    }
}
