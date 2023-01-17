package org.clever.core;

import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.joda.time.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/30 17:23 <br/>
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    public static final String HH_mm_ss = "HH:mm:ss";
    public static final String yyyy_MM_dd = "yyyy-MM-dd";
    public static final String yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss";
    public static final String yyyy_MM_dd_HH_mm = "yyyy-MM-dd HH:mm";

    /**
     * 定义可能出现的时间日期格式<br />
     * 参考: <a href="https://www.jianshu.com/p/cf2f1f26dd0a">连接</a>
     */
    private static final String[] parsePatterns = {
            yyyy_MM_dd, yyyy_MM_dd_HH_mm_ss, yyyy_MM_dd_HH_mm,
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm",
            "yyyyMMdd", "yyyyMMdd HH:mm:ss", "yyyyMMdd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    };

    /**
     * 得到当前时间的日期字符串形式，如：2016-4-27、2016-4-27 21:57:15<br/>
     *
     * @param pattern 日期格式字符串，如："yyyy-MM-dd" "HH:mm:ss" "E"
     */
    public static String getCurrentDate(String pattern) {
        return DateFormatUtils.format(new Date(), pattern);
    }

    /**
     * 得到当前时间的日期字符串，格式（yyyy-MM-dd）<br/>
     */
    public static String getCurrentDate() {
        return getCurrentDate("yyyy-MM-dd");
    }

    /**
     * 得到当前时间字符串 格式（HH:mm:ss）
     *
     * @return 当前时间字符串，如：12:14:21
     */
    public static String getCurrentTime() {
        return DateFormatUtils.format(new Date(), "HH:mm:ss");
    }

    /**
     * 得到当前日期和时间字符串 格式（yyyy-MM-dd HH:mm:ss）
     *
     * @return 当前时间字符串，如：2014-01-02 10:14:10
     */
    public static String getCurrentDateTime() {
        return DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 根据时间数，得到日期字符串<br/>
     *
     * @param dateTime 时间数，可通过System.currentTimeMillis()得到
     * @param pattern  时间格式字符串，如："yyyy-MM-dd HH:mm:ss"，默认是：yyyy-MM-dd
     * @return 时间字符串
     */
    public static String getDate(long dateTime, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            pattern = "yyyy-MM-dd";
        }
        return DateFormatUtils.format(new Date(dateTime), pattern);
    }

    /**
     * 根据时间数，得到日期字符串，格式：yyyy-MM-dd HH:mm:ss<br/>
     *
     * @param dateTime 时间数，可通过System.currentTimeMillis()得到
     * @return 时间字符串，如：2014-03-02 03:12:03
     */
    public static String getDate(long dateTime) {
        return DateFormatUtils.format(new Date(dateTime), "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 得到当前年份字符串 格式（yyyy）
     *
     * @return 当前年字符串，如：2014
     */
    public static String getYear() {
        return DateFormatUtils.format(new Date(), "yyyy");
    }

    /**
     * 得到当前月份字符串 格式（MM）
     *
     * @return 当前月字符串，如：02
     */
    public static String getMonth() {
        return DateFormatUtils.format(new Date(), "MM");
    }

    /**
     * 得到当天字符串 格式（dd）
     *
     * @return 当前天字符串，如：21
     */
    public static String getDay() {
        return DateFormatUtils.format(new Date(), "dd");
    }

    /**
     * 得到当前星期字符串 格式（E）星期几
     *
     * @return 当前日期是星期几，如：5
     */
    public static String getWeek() {
        return DateFormatUtils.format(new Date(), "E");
    }

    /**
     * 得到日期字符串 默认格式（yyyy-MM-dd）
     *
     * @param date    日期对象
     * @param pattern 日期格式，如："yyyy-MM-dd" "HH:mm:ss" "E"
     */
    public static String formatToString(Date date, String pattern) {
        String formatDate;
        if (StringUtils.isNotBlank(pattern)) {
            formatDate = DateFormatUtils.format(date, pattern);
        } else {
            formatDate = DateFormatUtils.format(date, "yyyy-MM-dd");
        }
        return formatDate;
    }

    /**
     * 得到日期时间字符串，转换格式（yyyy-MM-dd HH:mm:ss）
     *
     * @param date 日期对象
     * @return 日期格式字符串，如：2015-03-01 10:21:14
     */
    public static String formatToString(Date date) {
        return formatToString(date, "yyyy-MM-dd HH:mm:ss");
    }

    @SneakyThrows
    public static Date parseDate(final String str, final String... parsePatterns) {
        return org.apache.commons.lang3.time.DateUtils.parseDate(str, parsePatterns);
    }

    /**
     * 日期型字符串转化为日期，转换格式（yyyy-MM-dd HH:mm:ss）
     *
     * @param str 日期格式字符串，如：2015-03-01 10:21:14
     */
    public static Date parseDate(String str) {
        return parseDate(String.valueOf(str), "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd");
    }

    /**
     * 日期型字符串转化为日期,支持格式如下：<br/>
     * "yyyy-MM-dd"<br/>
     * "yyyy-MM-dd HH:mm:ss"<br/>
     * "yyyy-MM-dd HH:mm"<br/>
     * "yyyy/MM/dd"<br/>
     * "yyyy/MM/dd HH:mm:ss"<br/>
     * "yyyy/MM/dd HH:mm"<br/>
     * "yyyyMMdd"<br/>
     * "yyyyMMdd HH:mm:ss"<br/>
     * "yyyyMMdd HH:mm"<br/>
     * "yyyy-MM-dd'T'HH:mm:ss.SSSZ"<br/>
     * "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"<br/>
     * 时间搓(毫秒)<br/>
     *
     * @param str 日期字符串，如：2014/03/01 12:15:10
     * @return 失败返回 null
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        if (str instanceof Date) {
            return (Date) str;
        }
        if (str instanceof Long || str instanceof Integer) {
            long time = (long) str;
            return new Date(time);
        }
        if (String.valueOf(str).length() != 8 && NumberUtils.isDigits(String.valueOf(str))) {
            long time = NumberUtils.toLong(String.valueOf(str), -1L);
            if (time != -1L) {
                return new Date(time);
            }
        }
        return parseDate(String.valueOf(str), parsePatterns);
    }

    /**
     * 获取两个时间之间的年数，“end - start” 的年数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的年数
     */
    public static int pastYears(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Years.yearsBetween(startDt, endDt).getYears();
    }

    /**
     * 获取两个时间之间的年数，“end - start” 的月数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的月数
     */
    public static int getMonths(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Months.monthsBetween(startDt, endDt).getMonths();
    }

    /**
     * 获取两个时间之间的天数，“end - start” 的周数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的周数
     */
    public static int getWeeks(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Weeks.weeksBetween(startDt, endDt).getWeeks();
    }

    /**
     * 获取两个时间之间的天数，“end - start” 的天数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的天数
     */
    public static int pastDays(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Days.daysBetween(startDt, endDt).getDays();
    }

    /**
     * 获取两个时间之间的小时数，“end - start” 的小时数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的小时数
     */
    public static int pastHours(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Hours.hoursBetween(startDt, endDt).getHours();
    }

    /**
     * 获取两个时间之间的分钟数，“end - start” 的分钟数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的分钟数
     */
    public static int pastMinutes(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Minutes.minutesBetween(startDt, endDt).getMinutes();
    }

    /**
     * 获取两个时间之间的秒数，“end - start” 的秒数
     *
     * @param start 开始时间
     * @param end   结束时间
     * @return “end - start” 的秒数
     */
    public static int pastSeconds(Date start, Date end) {
        DateTime startDt = new DateTime(start);
        DateTime endDt = new DateTime(end);
        return Seconds.secondsBetween(startDt, endDt).getSeconds();
    }

    /**
     * 得到指定时间当天的开始时间<br/>
     * 例如：传入"2014-01-03 08:36:21" 返回 "2014-01-03 00:00:00"
     */
    @SneakyThrows
    public static Date getDayStartTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(formatToString(date, "yyyy-MM-dd") + " 00:00:00");
    }

    /**
     * 得到指定时间当天的截止时间<br/>
     * 例如：传入"2014-01-03 08:36:21" 返回 "2014-01-03 23:59:59"
     */
    @SneakyThrows
    public static Date getDayEndTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(formatToString(date, "yyyy-MM-dd") + " 23:59:59");
    }

    /**
     * 得到指定时间当小时的开始时间<br/>
     * 例如：传入"2014-01-03 08:36:21" 返回 "2014-01-03 08:00:00"
     */
    @SneakyThrows
    public static Date getHourStartTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(formatToString(date, "yyyy-MM-dd HH") + ":00:00");
    }

    /**
     * 得到指定时间当小时的截止时间<br/>
     * 例如：传入"2014-01-03 08:36:21" 返回 "2014-01-03 08:59:59"
     */
    @SneakyThrows
    public static Date getHourEndTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.parse(formatToString(date, "yyyy-MM-dd HH") + ":59:59");
    }

    /**
     * 获取当前时间
     */
    public static Date now() {
        return new Date();
    }

    /**
     * 获取当前日期，不包含时间
     */
    public static Date today() {
        return new LocalDate().toDate();
    }

    /**
     * 指定年月日构建日期对象
     */
    public static Date createDate(int year, int month, int day) {
        return createDate(year, month, day, 0, 0, 0);
    }

    /**
     * 指定年月日 小时 分钟秒 构建日期对象
     */
    public static Date createDate(int year, int month, int day, int hour, int minute, int second) {
        DateTime dateTime = new DateTime(year, month, day, 0, 0, 0);
        return dateTime.toDate();
    }

    /**
     * 将时间字符串转为时间戳
     * <p>time 格式为 yyyy-MM-dd HH:mm:ss</p>
     *
     * @param time 时间字符串
     * @return 毫秒时间戳
     */
    @SneakyThrows
    public static long string2Millis(final String time) {
        return parseDate(time, "yyyy-MM-dd HH:mm:ss").getTime();
    }

    private static final TimeZone TZ = TimeZone.getTimeZone("GMT+:08:00");

    public static Calendar getCalendar() {
        return Calendar.getInstance(TZ);
    }

    public static int getYear(Timestamp now) {
        Calendar calc = getCalendar();
        calc.setTime(new Date(now.getTime()));
        return calc.get(Calendar.YEAR);
    }

    public static int getMonth(Timestamp now) {
        Calendar calc = getCalendar();
        calc.setTime(new Date(now.getTime()));
        return calc.get(Calendar.MONTH);
    }

    public static int getDay(Timestamp now) {
        Calendar calc = getCalendar();
        calc.setTime(new Date(now.getTime()));
        return calc.get(Calendar.DAY_OF_MONTH);
    }

    public static Timestamp toTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }

    /**
     * 获取按天累加的日期序列
     */
    public static List<Date> getDaySeq(Date start, Date end) {
        final Calendar calc = getCalendar();
        List<Date> dates = new ArrayList<>();
        for (calc.setTime(start); calc.getTime().compareTo(end) <= 0; calc.add(Calendar.DAY_OF_WEEK, 1)) {
            dates.add(calc.getTime());
        }
        return dates;
    }

    /**
     * 获取按月累加的日期序列, 返回 201601 201602 201603
     */
    public static List<String> getMonthSeq(Date start, Date end) {
        final Calendar calc = Calendar.getInstance();
        calc.setTime(start);
        if (calc.get(Calendar.DAY_OF_MONTH) > 1) {
            start = parseDate(formatToString(start, "yyyyMM") + "01", "yyyyMMdd");
        }
        calc.setTime(end);
        if (calc.get(Calendar.DAY_OF_MONTH) > 1) {
            end = parseDate(formatToString(end, "yyyyMM") + "01", "yyyyMMdd");
        }
        List<String> list = new ArrayList<>();
        for (calc.setTime(start); calc.getTime().compareTo(end) <= 0; calc.add(Calendar.MONTH, 1)) {
            list.add(formatToString(calc.getTime(), "yyyyMM"));
        }
        return list;
    }
}
