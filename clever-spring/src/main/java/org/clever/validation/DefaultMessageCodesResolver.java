package org.clever.validation;

import org.clever.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * {@link MessageCodesResolver} 接口的默认实现。
 *
 * <p>将按以下顺序为对象错误创建两个消息代码（使用 {@link Format#PREFIX_ERROR_CODE 前缀} {@link #setMessageCodeFormatter(MessageCodeFormatter) 格式化程序时}）：
 * <ul>
 * <li>1.: code + "." + object name
 * <li>2.: code
 * </ul>
 *
 * <p>将为字段规范创建四个消息代码，顺序如下：
 * <ul>
 * <li>1.: code + "." + object name + "." + field
 * <li>2.: code + "." + field
 * <li>3.: code + "." + field type
 * <li>4.: code
 * </ul>
 *
 * <p>例如，在代码“typeMismatch”、对象名称“user”、字段“age”的情况下：
 * <ul>
 * <li>1. try "typeMismatch.user.age"
 * <li>2. try "typeMismatch.age"
 * <li>3. try "typeMismatch.int"
 * <li>4. try "typeMismatch"
 * </ul>
 *
 * <p>因此，可以利用此解析算法来显示绑定错误的特定消息，如“required”和“typeMismatch”：
 * <ul>
 * <li>at the object + field level ("age" field, but only on "user");
 * <li>at the field level (all "age" fields, no matter which object name);
 * <li>or at the general level (all fields, on any object).
 * </ul>
 *
 * <p>如果是数组、{@link List} 或 {@link java.util.Map} 属性，则会生成特定元素和整个集合的代码。假设对象“user”中数组“groups”的字段“name”：
 * <ul>
 * <li>1. try "typeMismatch.user.groups[0].name"
 * <li>2. try "typeMismatch.user.groups.name"
 * <li>3. try "typeMismatch.groups[0].name"
 * <li>4. try "typeMismatch.groups.name"
 * <li>5. try "typeMismatch.name"
 * <li>6. try "typeMismatch.java.lang.String"
 * <li>7. try "typeMismatch"
 * </ul>
 *
 * <p>默认情况下，{@code errorCode} 将放置在构造的消息字符串的开头。
 * {@link #setMessageCodeFormatter(MessageCodeFormatter) messageCodeFormatter} 属性可用于指定替代串联 {@link MessageCodeFormatter 格式}。
 *
 * <p>为了将所有代码分组到资源包中的特定类别，例如“validation.typeMismatch.name”而不是默认的“typeMismatch.name”，考虑指定要应用的 {@link #setPrefix prefix}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:52 <br/>
 */
public class DefaultMessageCodesResolver implements MessageCodesResolver, Serializable {
    /**
     * 此实现在解析消息代码时使用的分隔符。
     */
    public static final String CODE_SEPARATOR = ".";
    private static final MessageCodeFormatter DEFAULT_FORMATTER = Format.PREFIX_ERROR_CODE;

    private String prefix = "";
    private MessageCodeFormatter formatter = DEFAULT_FORMATTER;

    /**
     * 指定要应用于此解析器构建的任何代码的前缀。
     * <p>默认为无。例如，指定“验证”。获取错误代码，如“validation.typeMismatch.name”。
     */
    public void setPrefix(String prefix) {
        this.prefix = (prefix != null ? prefix : "");
    }

    /**
     * 返回要应用于此解析器构建的任何代码的前缀。
     * <p>如果没有前缀，则返回一个空字符串。
     */
    protected String getPrefix() {
        return this.prefix;
    }

    /**
     * 指定此解析器构建的消息代码的格式。
     * <p>默认值为 {@link Format#PREFIX_ERROR_CODE}。
     *
     * @see Format
     */
    public void setMessageCodeFormatter(MessageCodeFormatter formatter) {
        this.formatter = (formatter != null ? formatter : DEFAULT_FORMATTER);
    }

    @Override
    public String[] resolveMessageCodes(String errorCode, String objectName) {
        return resolveMessageCodes(errorCode, objectName, "", null);
    }

    /**
     * 为给定的代码和字段构建代码列表：对象/字段特定代码、字段特定代码、普通错误代码。
     * <p>数组、列表和映射针对特定元素和整个集合进行解析。
     * <p>有关生成代码的详细信息，请参阅 {@link DefaultMessageCodesResolver}
     *
     * @return 代码列表
     */
    @Override
    public String[] resolveMessageCodes(String errorCode, String objectName, String field, Class<?> fieldType) {
        Set<String> codeList = new LinkedHashSet<>();
        List<String> fieldList = new ArrayList<>();
        buildFieldList(field, fieldList);
        addCodes(codeList, errorCode, objectName, fieldList);
        int dotIndex = field.lastIndexOf('.');
        if (dotIndex != -1) {
            buildFieldList(field.substring(dotIndex + 1), fieldList);
        }
        addCodes(codeList, errorCode, null, fieldList);
        if (fieldType != null) {
            addCode(codeList, errorCode, null, fieldType.getName());
        }
        addCode(codeList, errorCode, null, null);
        return StringUtils.toStringArray(codeList);
    }

    private void addCodes(Collection<String> codeList, String errorCode, String objectName, Iterable<String> fields) {
        for (String field : fields) {
            addCode(codeList, errorCode, objectName, field);
        }
    }

    private void addCode(Collection<String> codeList, String errorCode, String objectName, String field) {
        codeList.add(postProcessMessageCode(this.formatter.format(errorCode, objectName, field)));
    }

    /**
     * 将提供的 {@code field} 的键控和非键控条目添加到提供的字段列表中。
     */
    protected void buildFieldList(String field, List<String> fieldList) {
        fieldList.add(field);
        String plainField = field;
        int keyIndex = plainField.lastIndexOf('[');
        while (keyIndex != -1) {
            int endKeyIndex = plainField.indexOf(']', keyIndex);
            if (endKeyIndex != -1) {
                plainField = plainField.substring(0, keyIndex) + plainField.substring(endKeyIndex + 1);
                fieldList.add(plainField);
                keyIndex = plainField.lastIndexOf('[');
            } else {
                keyIndex = -1;
            }
        }
    }

    /**
     * 后处理由此解析器构建的给定消息代码。
     * <p>默认实现应用指定的前缀（如果有）。
     *
     * @param code 此解析器构建的消息代码
     * @return 要返回的最终消息代码
     * @see #setPrefix
     */
    protected String postProcessMessageCode(String code) {
        return getPrefix() + code;
    }

    /**
     * 常见的消息代码格式。
     *
     * @see MessageCodeFormatter
     * @see DefaultMessageCodesResolver#setMessageCodeFormatter(MessageCodeFormatter)
     */
    public enum Format implements MessageCodeFormatter {
        /**
         * 在生成的消息代码的开头加上错误代码的前缀。例如：{@code errorCode + "." + 对象名称 + "." + 字段}
         */
        PREFIX_ERROR_CODE {
            @Override
            public String format(String errorCode, String objectName, String field) {
                return toDelimitedString(errorCode, objectName, field);
            }
        },

        /**
         * 在生成的消息代码末尾添加错误代码。例如：{@code 对象名称 + "." + 字段 + "." + 错误代码}
         */
        POSTFIX_ERROR_CODE {
            @Override
            public String format(String errorCode, String objectName, String field) {
                return toDelimitedString(objectName, field, errorCode);
            }
        };

        /**
         * 连接给定的元素，用 {@link DefaultMessageCodesResolver#CODE_SEPARATOR} 分隔每个元素，完全跳过零长度或空元素。
         */
        public static String toDelimitedString(String... elements) {
            StringJoiner rtn = new StringJoiner(CODE_SEPARATOR);
            for (String element : elements) {
                if (StringUtils.hasLength(element)) {
                    rtn.add(element);
                }
            }
            return rtn.toString();
        }
    }
}
