package org.clever.beans;

import org.clever.util.ObjectUtils;
import org.clever.util.ReflectionUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 帮助器类，用于根据可配置的距离计算属性匹配。
 * 提供潜在匹配项的列表以及生成错误消息的简单方法。
 * 适用于java bean属性和字段。主要用于框架内，尤其是绑定设施
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:41 <br/>
 *
 * @see #forProperty(String, Class)
 * @see #forField(String, Class)
 */
public abstract class PropertyMatches {
    /**
     * 默认最大特性距离：2
     */
    public static final int DEFAULT_MAX_DISTANCE = 2;

    // Static factory methods

    /**
     * 为给定bean属性创建PropertyMatches
     *
     * @param propertyName 要查找可能匹配项的属性的名称
     * @param beanClass    用于搜索匹配项的bean类
     */
    public static PropertyMatches forProperty(String propertyName, Class<?> beanClass) {
        return forProperty(propertyName, beanClass, DEFAULT_MAX_DISTANCE);
    }

    /**
     * 为给定bean属性创建PropertyMatches
     *
     * @param propertyName 要查找可能匹配项的属性的名称
     * @param beanClass    用于搜索匹配项的bean类
     * @param maxDistance  允许匹配的最大属性距离
     */
    public static PropertyMatches forProperty(String propertyName, Class<?> beanClass, int maxDistance) {
        return new BeanPropertyMatches(propertyName, beanClass, maxDistance);
    }

    /**
     * 为给定字段属性创建PropertyMatches
     *
     * @param propertyName 要查找可能匹配项的属性的名称
     * @param beanClass    用于搜索匹配项的bean类
     */
    public static PropertyMatches forField(String propertyName, Class<?> beanClass) {
        return forField(propertyName, beanClass, DEFAULT_MAX_DISTANCE);
    }

    /**
     * 为给定字段属性创建PropertyMatches
     *
     * @param propertyName 要查找可能匹配项的属性的名称
     * @param beanClass    用于搜索匹配项的bean类
     * @param maxDistance  允许匹配的最大属性距离
     */
    public static PropertyMatches forField(String propertyName, Class<?> beanClass, int maxDistance) {
        return new FieldPropertyMatches(propertyName, beanClass, maxDistance);
    }

    // Instance state

    private final String propertyName;
    private final String[] possibleMatches;

    /**
     * 为给定属性和可能的匹配项创建新的PropertyMatches实例
     */
    private PropertyMatches(String propertyName, String[] possibleMatches) {
        this.propertyName = propertyName;
        this.possibleMatches = possibleMatches;
    }

    /**
     * 返回请求属性的名称
     */
    public String getPropertyName() {
        return this.propertyName;
    }

    /**
     * 返回计算出的可能匹配项
     */
    public String[] getPossibleMatches() {
        return this.possibleMatches;
    }

    /**
     * 为给定的无效属性名生成错误消息，指示可能的属性匹配
     */
    public abstract String buildErrorMessage();

    // Implementation support for subclasses

    protected void appendHintMessage(StringBuilder msg) {
        msg.append("Did you mean ");
        for (int i = 0; i < this.possibleMatches.length; i++) {
            msg.append('\'');
            msg.append(this.possibleMatches[i]);
            if (i < this.possibleMatches.length - 2) {
                msg.append("', ");
            } else if (i == this.possibleMatches.length - 2) {
                msg.append("', or ");
            }
        }
        msg.append("'?");
    }

    /**
     * 根据Levenshtein算法计算给定两个字符串之间的距离
     *
     * @param s1 第一个字符串
     * @param s2 第二个字符串
     * @return 距离值
     */
    private static int calculateStringDistance(String s1, String s2) {
        if (s1.isEmpty()) {
            return s2.length();
        }
        if (s2.isEmpty()) {
            return s1.length();
        }
        int[][] d = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= s1.length(); i++) {
            char c1 = s1.charAt(i - 1);
            for (int j = 1; j <= s2.length(); j++) {
                int cost;
                char c2 = s2.charAt(j - 1);
                if (c1 == c2) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
            }
        }
        return d[s1.length()][s2.length()];
    }

    // Concrete subclasses

    private static class BeanPropertyMatches extends PropertyMatches {
        public BeanPropertyMatches(String propertyName, Class<?> beanClass, int maxDistance) {
            super(propertyName, calculateMatches(propertyName, BeanUtils.getPropertyDescriptors(beanClass), maxDistance));
        }

        /**
         * 为给定的属性和类生成可能的属性备选方案。内部使用{@code getStringDistance}方法，该方法又使用Levenshtein算法来确定两个字符串之间的距离
         *
         * @param descriptors 要搜索的JavaBeans属性描述符
         * @param maxDistance 要接受的最大距离
         */
        private static String[] calculateMatches(String name, PropertyDescriptor[] descriptors, int maxDistance) {
            List<String> candidates = new ArrayList<>();
            for (PropertyDescriptor pd : descriptors) {
                if (pd.getWriteMethod() != null) {
                    String possibleAlternative = pd.getName();
                    if (calculateStringDistance(name, possibleAlternative) <= maxDistance) {
                        candidates.add(possibleAlternative);
                    }
                }
            }
            Collections.sort(candidates);
            return StringUtils.toStringArray(candidates);
        }

        @Override
        public String buildErrorMessage() {
            StringBuilder msg = new StringBuilder(160);
            msg.append("Bean property '")
                    .append(getPropertyName())
                    .append("' is not writable or has an invalid setter method. ");
            if (!ObjectUtils.isEmpty(getPossibleMatches())) {
                appendHintMessage(msg);
            } else {
                msg.append("Does the parameter type of the setter match the return type of the getter?");
            }
            return msg.toString();
        }
    }

    private static class FieldPropertyMatches extends PropertyMatches {
        public FieldPropertyMatches(String propertyName, Class<?> beanClass, int maxDistance) {
            super(propertyName, calculateMatches(propertyName, beanClass, maxDistance));
        }

        private static String[] calculateMatches(final String name, Class<?> clazz, final int maxDistance) {
            final List<String> candidates = new ArrayList<>();
            ReflectionUtils.doWithFields(clazz, field -> {
                String possibleAlternative = field.getName();
                if (calculateStringDistance(name, possibleAlternative) <= maxDistance) {
                    candidates.add(possibleAlternative);
                }
            });
            Collections.sort(candidates);
            return StringUtils.toStringArray(candidates);
        }

        @Override
        public String buildErrorMessage() {
            StringBuilder msg = new StringBuilder(80);
            msg.append("Bean property '").append(getPropertyName()).append("' has no matching field.");
            if (!ObjectUtils.isEmpty(getPossibleMatches())) {
                msg.append(' ');
                appendHintMessage(msg);
            }
            return msg.toString();
        }
    }
}
