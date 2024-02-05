package org.clever.js.api.utils;

import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;

import java.util.Objects;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/06/03 16:04 <br/>
 */
public class ScriptCodeUtils {
    public static final String COMPRESS_CHAR_SPACE = " ";

    /**
     * 压缩脚本代码字符串
     *
     * @param code         脚本代码字符串
     * @param compressChar 压缩替换字符串
     * @param keepWrap     是否保持换行符号
     */
    public static String compressCode(String code, String compressChar, boolean keepWrap) {
        if (StringUtils.isBlank(code)) {
            return "";
        }
        StringBuilder sb = new StringBuilder(code.length());
        boolean current = false;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (keepWrap && Objects.equals('\n', c)) {
                sb.append(c);
                continue;
            }
            boolean pre = current;
            current = Character.isWhitespace(c);
            if (current) {
                continue;
            }
            int length = sb.length();
            if (pre && length > 0 && !Character.isWhitespace(sb.charAt(length - 1))) {
                sb.append(compressChar);
            }
            sb.append(c);
        }
        return StringUtils.trim(sb.toString());
    }

    /**
     * 压缩脚本代码字符串
     *
     * @param code     脚本代码字符串
     * @param keepWrap 是否保持换行符号
     */
    public static String compressCode(String code, boolean keepWrap) {
        return compressCode(code, COMPRESS_CHAR_SPACE, keepWrap);
    }

    /**
     * 把js代码片段包装成一个js函数代码(保留原始的代码行号)
     *
     * @param code   js代码片段
     * @param unique 函数名的唯一标识
     * @return js函数代码
     */
    public static String wrapFunction(String code, String unique) {
        return String.format("function __wrap_fuc_%1$s(args) {;%2$s\n}\n__fuc_autogenerate_%1$s;", Conv.asString(unique), Conv.asString(code));
    }
}
