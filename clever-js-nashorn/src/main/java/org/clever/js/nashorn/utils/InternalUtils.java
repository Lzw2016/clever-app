package org.clever.js.nashorn.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.clever.js.api.GlobalConstant;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/08/08 21:04 <br/>
 */
public class InternalUtils {
    /**
     * 时间格式
     */
    private static final Pattern Date_Pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");

    public static final InternalUtils Instance = new InternalUtils();

    private InternalUtils() {
    }

    /**
     * 判断两个对象是不是同一个对象(内存地址相同)
     */
    public boolean same(Object a, Object b) {
        return a == b;
    }

    /**
     * 把时间格式化成标准的格式(只支持格式 2019-08-26T08:35:24.566Z)
     */
    public String formatDate(String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        if (str.length() != 24) {
            return str;
        }
        Matcher matcher = Date_Pattern.matcher(str);
        if (!matcher.matches()) {
            return str;
        }
        //return str.replace('T', ' ').substring(0, str.length() - 5);
        try {
            Date date = DateUtils.parseDate(str, GlobalConstant.JS_Default_Format);
            // 8个小时的时差
            date = DateUtils.addHours(date, 8);
            return DateFormatUtils.format(date, GlobalConstant.JS_Default_Format);
        } catch (ParseException e) {
            return str;
        }
    }
}
