package org.clever.core.id;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.clever.core.Assert;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 业务编码成功器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 11:01 <br/>
 */
public class BusinessCodeUtils {
    private final static Pattern EXP_PATTERN = Pattern.compile("\\$\\{.*?}");

    /**
     * 根据自定义模式生成业务编码 <br/>
     * <pre>
     * 支持: ${date_format_pattern}、${seq_size}、${id_size}，例如：
     * CK${yyMMddHHmm}${seq}    -> CK22120108301、CK221201083023
     * CK${yyyyMMdd}_${seq3}    -> CK20221201_001、CK20221201_023
     * CK${yy}-${MMdd}-${seq3}  -> CK22-1201-001、CK22-1201-023
     * </pre>
     * date_format_pattern 参考:
     * <pre>
     *  yy      2位年份
     *  yyyy    4位年份
     *  MM      月份
     *  dd      月份中的天数
     *  HH      24小时数
     *  hh      12小时数
     *  mm      分钟数
     *  ss      秒数
     *  SSS     毫秒数
     *  D       年份中的天数
     *  E       星期几
     *  W       一个月中第几周
     *  w       一年中第几周
     *  a       上下午标识
     *  z       表示时区
     * </pre>
     *
     * @param pattern 自定义模式
     * @param date    时间
     * @param seq     当前序列值
     */
    public static String create(final String pattern, final Date date, final Number seq) {
        Assert.isNotBlank(pattern, "参数 pattern 不能为空");
        Assert.notNull(date, "参数 date 不能为空");
        StringBuilder code = new StringBuilder(32);
        Matcher matcher = EXP_PATTERN.matcher(pattern);
        int idx = 0;
        while (matcher.find()) {
            final int start = matcher.start();
            final String exp = matcher.group().substring(2, matcher.group().length() - 1);
            String value = exp;
            if (start > idx) {
                code.append(pattern, idx, start);
            }
            idx = matcher.end();
            // 开始解析表达式
            String[] seqExpArr = {"seq", "id"};
            String seqExp = Arrays.stream(seqExpArr).filter(exp::startsWith).findFirst().orElse(null);
            if (seqExp != null) {
                // 解析 seq
                if (seq == null) {
                    value = "";
                } else {
                    long num = Math.abs(seq.longValue());
                    // 不使用科学计数法
                    NumberFormat numberFormat = NumberFormat.getInstance();
                    numberFormat.setGroupingUsed(false);
                    value = numberFormat.format(num);
                    int size = NumberUtils.toInt(exp.substring(seqExp.length()), 0);
                    if (size >= 1 && value.length() > size) {
                        value = value.substring(value.length() - size);
                    }
                    // 保证数字位数
                    value = StringUtils.leftPad(value, size, "0");
                }
            } else {
                // 解析 date_format_pattern
                try {
                    value = DateFormatUtils.format(date, exp);
                } catch (Exception ignored) {
                }
            }
            code.append(value);
        }
        if (pattern.length() > idx) {
            code.append(pattern, idx, pattern.length());
        }
        return code.toString();
    }
}
