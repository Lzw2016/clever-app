package org.clever.beans;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.StringJoiner;

/**
 * 组合异常，由单个PropertyAccessException实例组成。
 * 此类的对象在绑定过程开始时创建，并根据需要向其中添加错误。<br/>
 * 当绑定过程遇到应用程序级PropertyAccessExceptions时，它将继续，
 * 应用那些可以应用的更改，并将被拒绝的更改存储在此类的对象中
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:26 <br/>
 */
public class PropertyBatchUpdateException extends BeansException {
    /**
     * PropertyAccessException对象列表
     */
    private final PropertyAccessException[] propertyAccessExceptions;

    /**
     * @param propertyAccessExceptions PropertyAccessExceptions列表
     */
    public PropertyBatchUpdateException(PropertyAccessException[] propertyAccessExceptions) {
        super(null, null);
        Assert.notEmpty(propertyAccessExceptions, "At least 1 PropertyAccessException required");
        this.propertyAccessExceptions = propertyAccessExceptions;
    }

    /**
     * 如果返回0，则在绑定期间不会遇到错误
     */
    public final int getExceptionCount() {
        return this.propertyAccessExceptions.length;
    }

    /**
     * 返回存储在此对象中的propertyAccessExceptions数组。如果没有错误，将返回空数组(非null)
     */
    public final PropertyAccessException[] getPropertyAccessExceptions() {
        return this.propertyAccessExceptions;
    }

    /**
     * 返回此字段的异常，如果没有异常，则返回null
     */
    public PropertyAccessException getPropertyAccessException(String propertyName) {
        for (PropertyAccessException pae : this.propertyAccessExceptions) {
            if (ObjectUtils.nullSafeEquals(propertyName, pae.getPropertyName())) {
                return pae;
            }
        }
        return null;
    }

    @Override
    public String getMessage() {
        StringJoiner stringJoiner = new StringJoiner("; ", "Failed properties: ", "");
        for (PropertyAccessException exception : this.propertyAccessExceptions) {
            stringJoiner.add(exception.getMessage());
        }
        return stringJoiner.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append("; nested PropertyAccessExceptions (");
        sb.append(getExceptionCount()).append(") are:");
        for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
            sb.append('\n').append("PropertyAccessException ").append(i + 1).append(": ");
            sb.append(this.propertyAccessExceptions[i]);
        }
        return sb.toString();
    }

    @SuppressWarnings({"DuplicatedCode", "SynchronizationOnLocalVariableOrMethodParameter"})
    @Override
    public void printStackTrace(PrintStream ps) {
        synchronized (ps) {
            ps.println(getClass().getName() + "; nested PropertyAccessException details (" + getExceptionCount() + ") are:");
            for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
                ps.println("PropertyAccessException " + (i + 1) + ":");
                this.propertyAccessExceptions[i].printStackTrace(ps);
            }
        }
    }

    @SuppressWarnings({"DuplicatedCode", "SynchronizationOnLocalVariableOrMethodParameter"})
    @Override
    public void printStackTrace(PrintWriter pw) {
        synchronized (pw) {
            pw.println(getClass().getName() + "; nested PropertyAccessException details (" + getExceptionCount() + ") are:");
            for (int i = 0; i < this.propertyAccessExceptions.length; i++) {
                pw.println("PropertyAccessException " + (i + 1) + ":");
                this.propertyAccessExceptions[i].printStackTrace(pw);
            }
        }
    }

    @Override
    public boolean contains(Class<?> exType) {
        if (exType == null) {
            return false;
        }
        if (exType.isInstance(this)) {
            return true;
        }
        for (PropertyAccessException pae : this.propertyAccessExceptions) {
            if (pae.contains(exType)) {
                return true;
            }
        }
        return false;
    }
}
