package org.clever.core.validator;

import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/13 11:10 <br/>
 */
@Data
public class FieldError {
    /**
     * 验证的字段名
     */
    private final String filed;
    /**
     * 验证的字段值
     */
    private final Object value;
    /**
     * 验证的错误消息
     */
    private final String message;
    /**
     * 验证所使用的JSR 303注解
     */
    private final String code;

    /**
     * 获取 message 列表
     */
    public static List<String> getMessages(Collection<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return new ArrayList<>();
        }
        return errors.stream().filter(Objects::nonNull)
                .map(error -> error.message)
                .collect(Collectors.toList());
    }

    /**
     * 获取错误列表: 格式 “filed + separator + message”
     */
    public static List<String> getErrors(Collection<FieldError> errors) {
        return getErrors(errors, null);
    }

    /**
     * 获取错误列表: 格式 “filed + separator + message”
     */
    public static List<String> getErrors(Collection<FieldError> errors, String separator) {
        if (errors == null || errors.isEmpty()) {
            return new ArrayList<>();
        }
        final String tmp = separator == null ? ": " : separator;
        return errors.stream().filter(Objects::nonNull)
                .map(error -> error.filed + tmp + error.message)
                .collect(Collectors.toList());
    }

    /**
     * 获取 filed 与 message 的映射集合{@code Map<filed, message>}
     */
    public static Map<String, String> getErrorMap(Collection<FieldError> errors) {
        if (errors == null || errors.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, String> map = new LinkedHashMap<>(errors.size());
        for (FieldError error : errors) {
            if (error == null) {
                continue;
            }
            map.put(error.filed, error.message);
        }
        return map;
    }
}
