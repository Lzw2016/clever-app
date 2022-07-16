package org.clever.beans.propertyeditors;

import org.clever.util.ClassUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link Class java.lang.Class} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:29 <br/>
 *
 * @see Class#forName
 * @see org.clever.util.ClassUtils#forName(String, ClassLoader)
 */
public class ClassEditor extends PropertyEditorSupport {
    private final ClassLoader classLoader;

    public ClassEditor() {
        this(null);
    }

    public ClassEditor(ClassLoader classLoader) {
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Class<?> clazz = (Class<?>) getValue();
        if (clazz != null) {
            return ClassUtils.getQualifiedName(clazz);
        } else {
            return "";
        }
    }
}
