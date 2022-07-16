package org.clever.beans.propertyeditors;

import org.clever.util.ClassUtils;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyEditorSupport;
import java.util.StringJoiner;

/**
 * {@code java.lang.Class}数组 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:30 <br/>
 */
public class ClassArrayEditor extends PropertyEditorSupport {
    private final ClassLoader classLoader;

    public ClassArrayEditor() {
        this(null);
    }

    public ClassArrayEditor(ClassLoader classLoader) {
        this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        if (StringUtils.hasText(text)) {
            String[] classNames = StringUtils.commaDelimitedListToStringArray(text);
            Class<?>[] classes = new Class<?>[classNames.length];
            for (int i = 0; i < classNames.length; i++) {
                String className = classNames[i].trim();
                classes[i] = ClassUtils.resolveClassName(className, this.classLoader);
            }
            setValue(classes);
        } else {
            setValue(null);
        }
    }

    @Override
    public String getAsText() {
        Class<?>[] classes = (Class<?>[]) getValue();
        if (ObjectUtils.isEmpty(classes)) {
            return "";
        }
        StringJoiner sj = new StringJoiner(",");
        for (Class<?> klass : classes) {
            sj.add(ClassUtils.getQualifiedName(klass));
        }
        return sj.toString();
    }
}
