package org.clever.validation;

import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * {@link Errors} 接口的抽象实现。
 * 提供对评估错误的通用访问；但是，没有定义{@link ObjectError ObjectErrors}和{@link FieldError FieldErrors}的具体管理。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:45 <br/>
 */
public abstract class AbstractErrors implements Errors, Serializable {
    private String nestedPath = "";
    private final Deque<String> nestedPathStack = new ArrayDeque<>();

    @Override
    public void setNestedPath(String nestedPath) {
        doSetNestedPath(nestedPath);
        this.nestedPathStack.clear();
    }

    @Override
    public String getNestedPath() {
        return this.nestedPath;
    }

    @Override
    public void pushNestedPath(String subPath) {
        this.nestedPathStack.push(getNestedPath());
        doSetNestedPath(getNestedPath() + subPath);
    }

    @Override
    public void popNestedPath() throws IllegalStateException {
        try {
            String formerNestedPath = this.nestedPathStack.pop();
            doSetNestedPath(formerNestedPath);
        } catch (NoSuchElementException ex) {
            throw new IllegalStateException("Cannot pop nested path: no nested path on stack");
        }
    }

    /**
     * 实际上设置了嵌套路径。委托给 setNestedPath 和 pushNestedPath。
     */
    protected void doSetNestedPath(String nestedPath) {
        if (nestedPath == null) {
            nestedPath = "";
        }
        nestedPath = canonicalFieldName(nestedPath);
        if (nestedPath.length() > 0 && !nestedPath.endsWith(Errors.NESTED_PATH_SEPARATOR)) {
            nestedPath += Errors.NESTED_PATH_SEPARATOR;
        }
        this.nestedPath = nestedPath;
    }

    /**
     * 关于此实例的嵌套路径，将给定字段转换为其完整路径。
     */
    protected String fixedField(String field) {
        if (StringUtils.hasLength(field)) {
            return getNestedPath() + canonicalFieldName(field);
        } else {
            String path = getNestedPath();
            return (path.endsWith(Errors.NESTED_PATH_SEPARATOR) ? path.substring(0, path.length() - NESTED_PATH_SEPARATOR.length()) : path);
        }
    }

    /**
     * 确定给定字段的规范字段名称。
     * <p>默认实现只是按原样返回字段名称。
     *
     * @param field 原始字段名称
     * @return 规范字段名称
     */
    protected String canonicalFieldName(String field) {
        return field;
    }

    @Override
    public void reject(String errorCode) {
        reject(errorCode, null, null);
    }

    @Override
    public void reject(String errorCode, String defaultMessage) {
        reject(errorCode, null, defaultMessage);
    }

    @Override
    public void rejectValue(String field, String errorCode) {
        rejectValue(field, errorCode, null, null);
    }

    @Override
    public void rejectValue(String field, String errorCode, String defaultMessage) {
        rejectValue(field, errorCode, null, defaultMessage);
    }

    @Override
    public boolean hasErrors() {
        return !getAllErrors().isEmpty();
    }

    @Override
    public int getErrorCount() {
        return getAllErrors().size();
    }

    @Override
    public List<ObjectError> getAllErrors() {
        List<ObjectError> result = new ArrayList<>();
        result.addAll(getGlobalErrors());
        result.addAll(getFieldErrors());
        return Collections.unmodifiableList(result);
    }

    @Override
    public boolean hasGlobalErrors() {
        return (getGlobalErrorCount() > 0);
    }

    @Override
    public int getGlobalErrorCount() {
        return getGlobalErrors().size();
    }

    @Override
    public ObjectError getGlobalError() {
        List<ObjectError> globalErrors = getGlobalErrors();
        return (!globalErrors.isEmpty() ? globalErrors.get(0) : null);
    }

    @Override
    public boolean hasFieldErrors() {
        return (getFieldErrorCount() > 0);
    }

    @Override
    public int getFieldErrorCount() {
        return getFieldErrors().size();
    }

    @Override
    public FieldError getFieldError() {
        List<FieldError> fieldErrors = getFieldErrors();
        return (!fieldErrors.isEmpty() ? fieldErrors.get(0) : null);
    }

    @Override
    public boolean hasFieldErrors(String field) {
        return (getFieldErrorCount(field) > 0);
    }

    @Override
    public int getFieldErrorCount(String field) {
        return getFieldErrors(field).size();
    }

    @Override
    public List<FieldError> getFieldErrors(String field) {
        List<FieldError> fieldErrors = getFieldErrors();
        List<FieldError> result = new ArrayList<>();
        String fixedField = fixedField(field);
        for (FieldError error : fieldErrors) {
            if (isMatchingFieldError(fixedField, error)) {
                result.add(error);
            }
        }
        return Collections.unmodifiableList(result);
    }

    @Override
    public FieldError getFieldError(String field) {
        List<FieldError> fieldErrors = getFieldErrors(field);
        return (!fieldErrors.isEmpty() ? fieldErrors.get(0) : null);
    }

    @Override
    public Class<?> getFieldType(String field) {
        Object value = getFieldValue(field);
        return (value != null ? value.getClass() : null);
    }

    /**
     * 检查给定的 FieldError 是否与给定的字段匹配。
     *
     * @param field      我们正在查找 FieldErrors 的字段
     * @param fieldError 候选人FieldError
     * @return FieldError 是否匹配给定的字段
     */
    protected boolean isMatchingFieldError(String field, FieldError fieldError) {
        if (field.equals(fieldError.getField())) {
            return true;
        }
        // 优化：使用 charAt 和 regionMatches 代替 endsWith 和 startsWith (SPR-11304)
        int endIndex = field.length() - 1;
        return endIndex >= 0
                && field.charAt(endIndex) == '*'
                && (endIndex == 0 || field.regionMatches(0, fieldError.getField(), 0, endIndex));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName());
        sb.append(": ").append(getErrorCount()).append(" errors");
        for (ObjectError error : getAllErrors()) {
            sb.append('\n').append(error);
        }
        return sb.toString();
    }
}
