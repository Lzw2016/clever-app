package org.clever.task.core.cron;

import java.io.Serializable;
import java.text.ParseException;
import java.util.*;

/**
 * 为类 unix 的 cron 表达式提供解析器和求值器。
 * Cron 表达式提供了指定复杂时间组合的能力，例如“每周一至周五上午 8:00”或“每月最后一个周五凌晨 1:30”。
 * <p>
 * Cron 表达式由 6 个必填字段和一个由空格分隔的可选字段组成。
 * 各字段分别说明如下：
 * <table cellspacing="8">
 * <tr>
 * <th align="left">字段名称</th>
 * <th align="left">&nbsp;</th>
 * <th align="left">允许值</th>
 * <th align="left">&nbsp;</th>
 * <th align="left">允许的特殊字符</th>
 * </tr>
 * <tr>
 * <td align="left"><code>Seconds</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-59</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Minutes</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-59</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Hours</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-23</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Day-of-month</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>1-31</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * ? / L W</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Month</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>0-11 or JAN-DEC</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Day-of-Week</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>1-7 or SUN-SAT</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * ? / L #</code></td>
 * </tr>
 * <tr>
 * <td align="left"><code>Year (Optional)</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>empty, 1970-2199</code></td>
 * <td align="left">&nbsp;</th>
 * <td align="left"><code>, - * /</code></td>
 * </tr>
 * </table>
 * (1) '*' 字符用于指定所有值。例如，分钟字段中的“*”表示“每分钟”。<br />
 * <p>
 * (2) 这 '？'日期和星期几字段允许字符。它用于指定“无特定值”。当您需要在两个字段之一而不是另一个字段中指定某些内容时，这很有用。<br />
 * <p>
 * (3) '-' 字符用于指定范围 例如，小时字段中的“10-12”表示“第 10、11 和 12 小时”。<br />
 * <p>
 * (4) ',' 字符用于指定附加值。例如，星期几字段中的“MON,WED,FRI”表示“星期一、星期三和星期五”。<br />
 * <p>
 * (5) '/' 字符用于指定增量。例如，秒字段中的“0/15”表示“秒 0、15、30 和 45”。
 * 秒字段中的“5/15”表示“第 5、20、35 和 50 秒”。在“/”之前指定“*”等同于指定 0 是开始的值。
 * 本质上，对于表达式中的每个字段，都有一组可以打开或关闭的数字。
 * 对于秒和分钟，数字范围从 0 到 59。对于小时 0 到 23，对于月份中的第 0 到 31 天，以及对于月份 0 到 11（JAN 到 DEC）。
 * “/”字符只是帮助您打开给定集合中的每个“第 n 个”值。因此，月份字段中的“7/6”只打开月份“7”，并不意味着每 6 个月，请注意这一点。<br />
 * <p>
 * (6) “L”字符可用于日期和星期几字段。该字符是“last”的简写，但在两个字段中具有不同的含义。
 * 例如，day-of-month 字段中的值“L”表示“该月的最后一天”- 1 月的第 31 天，非闰年的 2 月的第 28 天。
 * 如果单独用于星期几字段，它仅表示“7”或“SAT”。
 * 但如果在星期几字段中用在另一个值之后，则表示“该月的最后一个 xxx 日”——例如“6L”表示“该月的最后一个星期五”。
 * 您还可以指定从该月最后一天开始的偏移量，例如“L-3”，这表示该日历月的倒数第三天。
 * 使用“L”选项时，重要的是不要指定列表或值范围，因为您会得到令人困惑/意外的结果。<br />
 * <p>
 * (7) “W”字符可用于日期字段。此字符用于指定最接近给定日期的工作日（周一至周五）。
 * 例如，如果您将“15W”指定为日期字段的值，则含义是：“离该月 15 日最近的工作日”。
 * 因此，如果 15 日是星期六，触发器将在 14 日星期五触发。
 * 如果 15 号是星期天，触发器将在 16 号星期一触发。如果 15 号是星期二，那么它将在 15 号星期二触发。
 * 但是，如果您指定“1W”作为日期的值，并且 1 号是星期六，触发器将在 3 号星期一触发，因为它不会“跳过”一个月的日期边界。
 * “W”字符只能在日期是一天而不是日期范围或列表时指定。<br />
 * <p>
 * (8) 'L' 和 'W' 字符也可以组合用于日期表达式以生成 'LW'，翻译为“该月的最后一个工作日”。<br />
 * <p>
 * (9) 星期几字段允许使用“#”字符。该字符用于指定该月的“第 n 个”XXX 日。
 * 例如，day-of-week 字段中的值“6#3”表示该月的第三个星期五（第 6 天 = 星期五，“#3”= 该月的第 3 个星期五）。
 * 其他示例：“2#1”= 每月的第一个星期一，“4#5”= 每月的第五个星期三。
 * 请注意，如果您指定“#5”并且该月中给定的星期几不是第 5 天，那么该月将不会触发。
 * 如果使用“#”字符，则星期几字段中只能有一个表达式（“3#1,6#3”无效，因为有两个表达式）。
 * 合法字符以及月份和星期几的名称不区分大小写。<br />
 * <br />
 * <b>注意:</b>
 * <ul>
 * <li>对指定星期几和月份值的支持不完整（您需要在其中一个字段中使用“？”字符）。</li>
 * <li>支持溢出范围 - 即左侧的数字大于右侧的数字。您可能会执行 22-2 以赶上晚上 10 点，直到凌晨 2 点，或者您可能有 11 月至 2 月。
 * 非常重要的是要注意，过度使用溢出范围会创建没有意义的范围，并且没有做出任何努力来确定 CronExpression 选择哪种解释。
 * 一个例子是“0 0 14-6 ? * FRI-MON”。</li>
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/08/08 16:14 <br/>
 */
@SuppressWarnings("all")
public final class CronExpression implements Serializable, Cloneable {
    private static final long serialVersionUID = 12423409423L;

    protected static final int SECOND = 0;
    protected static final int MINUTE = 1;
    protected static final int HOUR = 2;
    protected static final int DAY_OF_MONTH = 3;
    protected static final int MONTH = 4;
    protected static final int DAY_OF_WEEK = 5;
    protected static final int YEAR = 6;
    protected static final int ALL_SPEC_INT = 99; // '*'
    protected static final int NO_SPEC_INT = 98; // '?'
    protected static final Integer ALL_SPEC = ALL_SPEC_INT;
    protected static final Integer NO_SPEC = NO_SPEC_INT;

    protected static final Map<String, Integer> monthMap = new HashMap<String, Integer>(20);
    protected static final Map<String, Integer> dayMap = new HashMap<String, Integer>(60);

    static {
        monthMap.put("JAN", 0);
        monthMap.put("FEB", 1);
        monthMap.put("MAR", 2);
        monthMap.put("APR", 3);
        monthMap.put("MAY", 4);
        monthMap.put("JUN", 5);
        monthMap.put("JUL", 6);
        monthMap.put("AUG", 7);
        monthMap.put("SEP", 8);
        monthMap.put("OCT", 9);
        monthMap.put("NOV", 10);
        monthMap.put("DEC", 11);

        dayMap.put("SUN", 1);
        dayMap.put("MON", 2);
        dayMap.put("TUE", 3);
        dayMap.put("WED", 4);
        dayMap.put("THU", 5);
        dayMap.put("FRI", 6);
        dayMap.put("SAT", 7);
    }

    private final String cronExpression;
    private TimeZone timeZone = null;
    protected transient TreeSet<Integer> seconds;
    protected transient TreeSet<Integer> minutes;
    protected transient TreeSet<Integer> hours;
    protected transient TreeSet<Integer> daysOfMonth;
    protected transient TreeSet<Integer> months;
    protected transient TreeSet<Integer> daysOfWeek;
    protected transient TreeSet<Integer> years;

    protected transient boolean lastdayOfWeek = false;
    protected transient int nthdayOfWeek = 0;
    protected transient boolean lastdayOfMonth = false;
    protected transient boolean nearestWeekday = false;
    protected transient int lastdayOffset = 0;
    protected transient boolean expressionParsed = false;

    public static final int MAX_YEAR = Calendar.getInstance().get(Calendar.YEAR) + 100;

    /**
     * 根据指定的参数构造一个新的 <code>CronExpression</code>
     *
     * @param cronExpression 新对象应表示的 cron 表达式的字符串表示形式
     * @throws ParseException 如果字符串表达式无法解析为有效的 <code>CronExpression</code>
     */
    public CronExpression(String cronExpression) throws ParseException {
        if (cronExpression == null) {
            throw new IllegalArgumentException("cronExpression cannot be null");
        }
        this.cronExpression = cronExpression.toUpperCase(Locale.US);
        buildExpression(this.cronExpression);
    }

    /**
     * 构造一个新的 {@code CronExpression} 作为现有实例的副本
     *
     * @param expression 要复制的现有 cron 表达式
     */
    public CronExpression(CronExpression expression) {
        // 我们不在这里调用其他构造函数，因为我们需要吞下 ParseException。我们还省略了一些健全性检查，因为它在逻辑上是不可触发的
        this.cronExpression = expression.getCronExpression();
        try {
            buildExpression(cronExpression);
        } catch (ParseException ex) {
            throw new AssertionError();
        }
        if (expression.getTimeZone() != null) {
            setTimeZone((TimeZone) expression.getTimeZone().clone());
        }
    }

    /**
     * 指示给定日期是否满足 cron 表达式。
     * 请注意，毫秒被忽略，因此落在同一秒的不同毫秒的两个 Date 在此处将始终具有相同的结果。
     *
     * @param date 评估日期
     * @return 一个布尔值，指示给定日期是否满足 cron 表达式
     */
    public boolean isSatisfiedBy(Date date) {
        Calendar testDateCal = Calendar.getInstance(getTimeZone());
        testDateCal.setTime(date);
        testDateCal.set(Calendar.MILLISECOND, 0);
        Date originalDate = testDateCal.getTime();
        testDateCal.add(Calendar.SECOND, -1);
        Date timeAfter = getTimeAfter(testDateCal.getTime());
        return ((timeAfter != null) && (timeAfter.equals(originalDate)));
    }

    /**
     * 返回满足 cron 表达式的给定日期时间<i>之后</i>的下一个日期时间。
     *
     * @param date 开始搜索下一个有效日期时间的日期时间
     * @return 下一个有效日期时间
     */
    public Date getNextValidTimeAfter(Date date) {
        return getTimeAfter(date);
    }

    /**
     * 返回给定日期时间之后不满足表达式的下一个日期时间
     *
     * @param date 开始搜索下一个无效日期时间的日期时间
     * @return 下一个有效日期时间
     */
    public Date getNextInvalidTimeAfter(Date date) {
        long difference = 1000;
        // 回到最接近的秒数，这样差异就会准确
        Calendar adjustCal = Calendar.getInstance(getTimeZone());
        adjustCal.setTime(date);
        adjustCal.set(Calendar.MILLISECOND, 0);
        Date lastDate = adjustCal.getTime();
        Date newDate;
        // FUTURE_TODO：（QUARTZ-481）改进这个！以下是此问题的 BAD 解决方案。这里的性能会很差，这取决于 cron 表达式。然而，这是一个解决方案。
        // 继续获取下一个包含时间，直到相隔时间超过一秒。此时，lastDate 是最后一个有效的触发时间。我们紧随其后返回第二个。
        while (difference == 1000) {
            newDate = getTimeAfter(lastDate);
            if (newDate == null) {
                break;
            }
            difference = newDate.getTime() - lastDate.getTime();
            if (difference == 1000) {
                lastDate = newDate;
            }
        }
        return new Date(lastDate.getTime() + 1000);
    }

    /**
     * 返回将解析此 <code>CronExpression</code> 的时区
     */
    public TimeZone getTimeZone() {
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        return timeZone;
    }

    /**
     * 设置将解析此 <code>CronExpression</code> 的时区
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * 返回 <code>CronExpression<code> 的字符串表示形式
     *
     * @return <code>CronExpression<code> 的字符串表示
     */
    @Override
    public String toString() {
        return cronExpression;
    }

    /**
     * 指示指定的 cron 表达式是否可以解析为有效的 cron 表达式
     *
     * @param cronExpression 要评估的表达式
     * @return 指示给定表达式是否为有效的 cron 表达式的布尔值
     */
    public static boolean isValidExpression(String cronExpression) {
        try {
            new CronExpression(cronExpression);
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }

    public static void validateExpression(String cronExpression) throws ParseException {
        new CronExpression(cronExpression);
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // 表达式解析函数
    //
    ////////////////////////////////////////////////////////////////////////////

    protected void buildExpression(String expression) throws ParseException {
        expressionParsed = true;
        try {
            if (seconds == null) {
                seconds = new TreeSet<Integer>();
            }
            if (minutes == null) {
                minutes = new TreeSet<Integer>();
            }
            if (hours == null) {
                hours = new TreeSet<Integer>();
            }
            if (daysOfMonth == null) {
                daysOfMonth = new TreeSet<Integer>();
            }
            if (months == null) {
                months = new TreeSet<Integer>();
            }
            if (daysOfWeek == null) {
                daysOfWeek = new TreeSet<Integer>();
            }
            if (years == null) {
                years = new TreeSet<Integer>();
            }
            int exprOn = SECOND;
            StringTokenizer exprsTok = new StringTokenizer(expression, " \t", false);
            while (exprsTok.hasMoreTokens() && exprOn <= YEAR) {
                String expr = exprsTok.nextToken().trim();
                // 如果 L 与一个月的其他日期一起使用，则抛出异常
                if (exprOn == DAY_OF_MONTH && expr.indexOf('L') != -1 && expr.length() > 1 && expr.contains(",")) {
                    throw new ParseException("Support for specifying 'L' and 'LW' with other days of the month is not implemented", -1);
                }
                // 如果 L 与一周中的其他日子一起使用，则抛出异常
                if (exprOn == DAY_OF_WEEK && expr.indexOf('L') != -1 && expr.length() > 1 && expr.contains(",")) {
                    throw new ParseException("Support for specifying 'L' with other days of the week is not implemented", -1);
                }
                if (exprOn == DAY_OF_WEEK && expr.indexOf('#') != -1 && expr.indexOf('#', expr.indexOf('#') + 1) != -1) {
                    throw new ParseException("Support for specifying multiple \"nth\" days is not implemented.", -1);
                }
                StringTokenizer vTok = new StringTokenizer(expr, ",");
                while (vTok.hasMoreTokens()) {
                    String v = vTok.nextToken();
                    storeExpressionVals(0, v, exprOn);
                }
                exprOn++;
            }
            if (exprOn <= DAY_OF_WEEK) {
                throw new ParseException("Unexpected end of expression.", expression.length());
            }
            if (exprOn <= YEAR) {
                storeExpressionVals(0, "*", YEAR);
            }
            TreeSet<Integer> dow = getSet(DAY_OF_WEEK);
            TreeSet<Integer> dom = getSet(DAY_OF_MONTH);
            // 从下面的 UnsupportedOperationException 复制逻辑
            boolean dayOfMSpec = !dom.contains(NO_SPEC);
            boolean dayOfWSpec = !dow.contains(NO_SPEC);
            if (!dayOfMSpec || dayOfWSpec) {
                if (!dayOfWSpec || dayOfMSpec) {
                    throw new ParseException("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.", 0);
                }
            }
        } catch (ParseException pe) {
            throw pe;
        } catch (Exception e) {
            throw new ParseException("Illegal cron expression format (" + e.toString() + ")", 0);
        }
    }

    protected int storeExpressionVals(int pos, String s, int type) throws ParseException {
        int incr = 0;
        int i = skipWhiteSpace(pos, s);
        if (i >= s.length()) {
            return i;
        }
        char c = s.charAt(i);
        if ((c >= 'A') && (c <= 'Z') && (!s.equals("L")) && (!s.equals("LW")) && (!s.matches("^L-[0-9]*[W]?"))) {
            String sub = s.substring(i, i + 3);
            int sval = -1;
            int eval = -1;
            if (type == MONTH) {
                sval = getMonthNumber(sub) + 1;
                if (sval <= 0) {
                    throw new ParseException("Invalid Month value: '" + sub + "'", i);
                }
                if (s.length() > i + 3) {
                    c = s.charAt(i + 3);
                    if (c == '-') {
                        i += 4;
                        sub = s.substring(i, i + 3);
                        eval = getMonthNumber(sub) + 1;
                        if (eval <= 0) {
                            throw new ParseException("Invalid Month value: '" + sub + "'", i);
                        }
                    }
                }
            } else if (type == DAY_OF_WEEK) {
                sval = getDayOfWeekNumber(sub);
                if (sval < 0) {
                    throw new ParseException("Invalid Day-of-Week value: '" + sub + "'", i);
                }
                if (s.length() > i + 3) {
                    c = s.charAt(i + 3);
                    if (c == '-') {
                        i += 4;
                        sub = s.substring(i, i + 3);
                        eval = getDayOfWeekNumber(sub);
                        if (eval < 0) {
                            throw new ParseException("Invalid Day-of-Week value: '" + sub + "'", i);
                        }
                    } else if (c == '#') {
                        try {
                            i += 4;
                            nthdayOfWeek = Integer.parseInt(s.substring(i));
                            if (nthdayOfWeek < 1 || nthdayOfWeek > 5) {
                                throw new Exception();
                            }
                        } catch (Exception e) {
                            throw new ParseException("A numeric value between 1 and 5 must follow the '#' option", i);
                        }
                    } else if (c == 'L') {
                        lastdayOfWeek = true;
                        i++;
                    }
                }

            } else {
                throw new ParseException("Illegal characters for this position: '" + sub + "'", i);
            }
            if (eval != -1) {
                incr = 1;
            }
            addToSet(sval, eval, incr, type);
            return (i + 3);
        }

        if (c == '?') {
            i++;
            if ((i + 1) < s.length() && (s.charAt(i) != ' ' && s.charAt(i + 1) != '\t')) {
                throw new ParseException("Illegal character after '?': " + s.charAt(i), i);
            }
            if (type != DAY_OF_WEEK && type != DAY_OF_MONTH) {
                throw new ParseException("'?' can only be specified for Day-of-Month or Day-of-Week.", i);
            }
            if (type == DAY_OF_WEEK && !lastdayOfMonth) {
                int val = daysOfMonth.last();
                if (val == NO_SPEC_INT) {
                    throw new ParseException("'?' can only be specified for Day-of-Month -OR- Day-of-Week.", i);
                }
            }
            addToSet(NO_SPEC_INT, -1, 0, type);
            return i;
        }
        if (c == '*' || c == '/') {
            if (c == '*' && (i + 1) >= s.length()) {
                addToSet(ALL_SPEC_INT, -1, incr, type);
                return i + 1;
            } else if (c == '/' && ((i + 1) >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t')) {
                throw new ParseException("'/' must be followed by an integer.", i);
            } else if (c == '*') {
                i++;
            }
            c = s.charAt(i);
            if (c == '/') {
                // 是否指定增量？
                i++;
                if (i >= s.length()) {
                    throw new ParseException("Unexpected end of string.", i);
                }
                incr = getNumericValue(s, i);
                i++;
                if (incr > 10) {
                    i++;
                }
                checkIncrementRange(incr, type, i);
            } else {
                incr = 1;
            }
            addToSet(ALL_SPEC_INT, -1, incr, type);
            return i;
        } else if (c == 'L') {
            i++;
            if (type == DAY_OF_MONTH) {
                lastdayOfMonth = true;
            }
            if (type == DAY_OF_WEEK) {
                addToSet(7, 7, 0, type);
            }
            if (type == DAY_OF_MONTH && s.length() > i) {
                c = s.charAt(i);
                if (c == '-') {
                    ValueSet vs = getValue(0, s, i + 1);
                    lastdayOffset = vs.value;
                    if (lastdayOffset > 30)
                        throw new ParseException("Offset from last day must be <= 30", i + 1);
                    i = vs.pos;
                }
                if (s.length() > i) {
                    c = s.charAt(i);
                    if (c == 'W') {
                        nearestWeekday = true;
                        i++;
                    }
                }
            }
            return i;
        } else if (c >= '0' && c <= '9') {
            int val = Integer.parseInt(String.valueOf(c));
            i++;
            if (i >= s.length()) {
                addToSet(val, -1, -1, type);
            } else {
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    ValueSet vs = getValue(val, s, i);
                    val = vs.value;
                    i = vs.pos;
                }
                i = checkNext(i, s, val, type);
                return i;
            }
        } else {
            throw new ParseException("Unexpected character: " + c, i);
        }
        return i;
    }

    private void checkIncrementRange(int incr, int type, int idxPos) throws ParseException {
        if (incr > 59 && (type == SECOND || type == MINUTE)) {
            throw new ParseException("Increment > 60 : " + incr, idxPos);
        } else if (incr > 23 && (type == HOUR)) {
            throw new ParseException("Increment > 24 : " + incr, idxPos);
        } else if (incr > 31 && (type == DAY_OF_MONTH)) {
            throw new ParseException("Increment > 31 : " + incr, idxPos);
        } else if (incr > 7 && (type == DAY_OF_WEEK)) {
            throw new ParseException("Increment > 7 : " + incr, idxPos);
        } else if (incr > 12 && (type == MONTH)) {
            throw new ParseException("Increment > 12 : " + incr, idxPos);
        }
    }

    protected int checkNext(int pos, String s, int val, int type) throws ParseException {
        int end = -1;
        int i = pos;
        if (i >= s.length()) {
            addToSet(val, end, -1, type);
            return i;
        }
        char c = s.charAt(pos);
        if (c == 'L') {
            if (type == DAY_OF_WEEK) {
                if (val < 1 || val > 7)
                    throw new ParseException("Day-of-Week values must be between 1 and 7", -1);
                lastdayOfWeek = true;
            } else {
                throw new ParseException("'L' option is not valid here. (pos=" + i + ")", i);
            }
            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }
        if (c == 'W') {
            if (type == DAY_OF_MONTH) {
                nearestWeekday = true;
            } else {
                throw new ParseException("'W' option is not valid here. (pos=" + i + ")", i);
            }
            if (val > 31)
                throw new ParseException("The 'W' option does not make sense with values larger than 31 (max number of days in a month)", i);
            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }
        if (c == '#') {
            if (type != DAY_OF_WEEK) {
                throw new ParseException("'#' option is not valid here. (pos=" + i + ")", i);
            }
            i++;
            try {
                nthdayOfWeek = Integer.parseInt(s.substring(i));
                if (nthdayOfWeek < 1 || nthdayOfWeek > 5) {
                    throw new Exception();
                }
            } catch (Exception e) {
                throw new ParseException("A numeric value between 1 and 5 must follow the '#' option", i);
            }
            TreeSet<Integer> set = getSet(type);
            set.add(val);
            i++;
            return i;
        }
        if (c == '-') {
            i++;
            c = s.charAt(i);
            int v = Integer.parseInt(String.valueOf(c));
            end = v;
            i++;
            if (i >= s.length()) {
                addToSet(val, end, 1, type);
                return i;
            }
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                ValueSet vs = getValue(v, s, i);
                end = vs.value;
                i = vs.pos;
            }
            if (i < s.length() && ((c = s.charAt(i)) == '/')) {
                i++;
                c = s.charAt(i);
                int v2 = Integer.parseInt(String.valueOf(c));
                i++;
                if (i >= s.length()) {
                    addToSet(val, end, v2, type);
                    return i;
                }
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    ValueSet vs = getValue(v2, s, i);
                    int v3 = vs.value;
                    addToSet(val, end, v3, type);
                    i = vs.pos;
                    return i;
                } else {
                    addToSet(val, end, v2, type);
                    return i;
                }
            } else {
                addToSet(val, end, 1, type);
                return i;
            }
        }
        if (c == '/') {
            if ((i + 1) >= s.length() || s.charAt(i + 1) == ' ' || s.charAt(i + 1) == '\t') {
                throw new ParseException("'/' must be followed by an integer.", i);
            }
            i++;
            c = s.charAt(i);
            int v2 = Integer.parseInt(String.valueOf(c));
            i++;
            if (i >= s.length()) {
                checkIncrementRange(v2, type, i);
                addToSet(val, end, v2, type);
                return i;
            }
            c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                ValueSet vs = getValue(v2, s, i);
                int v3 = vs.value;
                checkIncrementRange(v3, type, i);
                addToSet(val, end, v3, type);
                i = vs.pos;
                return i;
            } else {
                throw new ParseException("Unexpected character '" + c + "' after '/'", i);
            }
        }
        addToSet(val, end, 0, type);
        i++;
        return i;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public String getExpressionSummary() {
        StringBuilder buf = new StringBuilder();
        buf.append("seconds: ");
        buf.append(getExpressionSetSummary(seconds));
        buf.append("\n");
        buf.append("minutes: ");
        buf.append(getExpressionSetSummary(minutes));
        buf.append("\n");
        buf.append("hours: ");
        buf.append(getExpressionSetSummary(hours));
        buf.append("\n");
        buf.append("daysOfMonth: ");
        buf.append(getExpressionSetSummary(daysOfMonth));
        buf.append("\n");
        buf.append("months: ");
        buf.append(getExpressionSetSummary(months));
        buf.append("\n");
        buf.append("daysOfWeek: ");
        buf.append(getExpressionSetSummary(daysOfWeek));
        buf.append("\n");
        buf.append("lastdayOfWeek: ");
        buf.append(lastdayOfWeek);
        buf.append("\n");
        buf.append("nearestWeekday: ");
        buf.append(nearestWeekday);
        buf.append("\n");
        buf.append("NthDayOfWeek: ");
        buf.append(nthdayOfWeek);
        buf.append("\n");
        buf.append("lastdayOfMonth: ");
        buf.append(lastdayOfMonth);
        buf.append("\n");
        buf.append("years: ");
        buf.append(getExpressionSetSummary(years));
        buf.append("\n");
        return buf.toString();
    }

    protected String getExpressionSetSummary(Set<Integer> set) {
        if (set.contains(NO_SPEC)) {
            return "?";
        }
        if (set.contains(ALL_SPEC)) {
            return "*";
        }
        StringBuilder buf = new StringBuilder();
        Iterator<Integer> itr = set.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            Integer iVal = itr.next();
            String val = iVal.toString();
            if (!first) {
                buf.append(",");
            }
            buf.append(val);
            first = false;
        }
        return buf.toString();
    }

    protected String getExpressionSetSummary(ArrayList<Integer> list) {
        if (list.contains(NO_SPEC)) {
            return "?";
        }
        if (list.contains(ALL_SPEC)) {
            return "*";
        }
        StringBuilder buf = new StringBuilder();
        Iterator<Integer> itr = list.iterator();
        boolean first = true;
        while (itr.hasNext()) {
            Integer iVal = itr.next();
            String val = iVal.toString();
            if (!first) {
                buf.append(",");
            }
            buf.append(val);
            first = false;
        }
        return buf.toString();
    }

    protected int skipWhiteSpace(int i, String s) {
        for (; i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t'); i++) {
            ;
        }
        return i;
    }

    protected int findNextWhiteSpace(int i, String s) {
        for (; i < s.length() && (s.charAt(i) != ' ' || s.charAt(i) != '\t'); i++) {
            ;
        }
        return i;
    }

    protected void addToSet(int val, int end, int incr, int type) throws ParseException {
        TreeSet<Integer> set = getSet(type);
        if (type == SECOND || type == MINUTE) {
            if ((val < 0 || val > 59 || end > 59) && (val != ALL_SPEC_INT)) {
                throw new ParseException("Minute and Second values must be between 0 and 59", -1);
            }
        } else if (type == HOUR) {
            if ((val < 0 || val > 23 || end > 23) && (val != ALL_SPEC_INT)) {
                throw new ParseException("Hour values must be between 0 and 23", -1);
            }
        } else if (type == DAY_OF_MONTH) {
            if ((val < 1 || val > 31 || end > 31) && (val != ALL_SPEC_INT) && (val != NO_SPEC_INT)) {
                throw new ParseException("Day of month values must be between 1 and 31", -1);
            }
        } else if (type == MONTH) {
            if ((val < 1 || val > 12 || end > 12) && (val != ALL_SPEC_INT)) {
                throw new ParseException("Month values must be between 1 and 12", -1);
            }
        } else if (type == DAY_OF_WEEK) {
            if ((val == 0 || val > 7 || end > 7) && (val != ALL_SPEC_INT) && (val != NO_SPEC_INT)) {
                throw new ParseException("Day-of-Week values must be between 1 and 7", -1);
            }
        }
        if ((incr == 0 || incr == -1) && val != ALL_SPEC_INT) {
            if (val != -1) {
                set.add(val);
            } else {
                set.add(NO_SPEC);
            }
            return;
        }
        int startAt = val;
        int stopAt = end;
        if (val == ALL_SPEC_INT && incr <= 0) {
            incr = 1;
            // 放入标记，还要填充值
            set.add(ALL_SPEC);
        }
        if (type == SECOND || type == MINUTE) {
            if (stopAt == -1) {
                stopAt = 59;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 0;
            }
        } else if (type == HOUR) {
            if (stopAt == -1) {
                stopAt = 23;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 0;
            }
        } else if (type == DAY_OF_MONTH) {
            if (stopAt == -1) {
                stopAt = 31;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == MONTH) {
            if (stopAt == -1) {
                stopAt = 12;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == DAY_OF_WEEK) {
            if (stopAt == -1) {
                stopAt = 7;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1;
            }
        } else if (type == YEAR) {
            if (stopAt == -1) {
                stopAt = MAX_YEAR;
            }
            if (startAt == -1 || startAt == ALL_SPEC_INT) {
                startAt = 1970;
            }
        }
        // 如果范围的末尾在开始之前，那么我们需要溢出到下一天、下一个月等。
        // 这是通过添加该类型的最大数量并使用模数 max 来确定要添加的值来完成的。
        int max = -1;
        if (stopAt < startAt) {
            switch (type) {
                case SECOND:
                    max = 60;
                    break;
                case MINUTE:
                    max = 60;
                    break;
                case HOUR:
                    max = 24;
                    break;
                case MONTH:
                    max = 12;
                    break;
                case DAY_OF_WEEK:
                    max = 7;
                    break;
                case DAY_OF_MONTH:
                    max = 31;
                    break;
                case YEAR:
                    throw new IllegalArgumentException("Start year must be less than stop year");
                default:
                    throw new IllegalArgumentException("Unexpected type encountered");
            }
            stopAt += max;
        }
        for (int i = startAt; i <= stopAt; i += incr) {
            if (max == -1) {
                // ie: 没有最大值可以溢出
                set.add(i);
            } else {
                // 取模得到真实值
                int i2 = i % max;
                // 1 索引范围不应包括。 0，并且应包括它们的最大值
                if (i2 == 0 && (type == MONTH || type == DAY_OF_WEEK || type == DAY_OF_MONTH)) {
                    i2 = max;
                }
                set.add(i2);
            }
        }
    }

    TreeSet<Integer> getSet(int type) {
        switch (type) {
            case SECOND:
                return seconds;
            case MINUTE:
                return minutes;
            case HOUR:
                return hours;
            case DAY_OF_MONTH:
                return daysOfMonth;
            case MONTH:
                return months;
            case DAY_OF_WEEK:
                return daysOfWeek;
            case YEAR:
                return years;
            default:
                return null;
        }
    }

    protected ValueSet getValue(int v, String s, int i) {
        char c = s.charAt(i);
        StringBuilder s1 = new StringBuilder(String.valueOf(v));
        while (c >= '0' && c <= '9') {
            s1.append(c);
            i++;
            if (i >= s.length()) {
                break;
            }
            c = s.charAt(i);
        }
        ValueSet val = new ValueSet();
        val.pos = (i < s.length()) ? i : i + 1;
        val.value = Integer.parseInt(s1.toString());
        return val;
    }

    protected int getNumericValue(String s, int i) {
        int endOfVal = findNextWhiteSpace(i, s);
        String val = s.substring(i, endOfVal);
        return Integer.parseInt(val);
    }

    protected int getMonthNumber(String s) {
        Integer integer = monthMap.get(s);
        if (integer == null) {
            return -1;
        }
        return integer;
    }

    protected int getDayOfWeekNumber(String s) {
        Integer integer = dayMap.get(s);
        if (integer == null) {
            return -1;
        }
        return integer;
    }

    ////////////////////////////////////////////////////////////////////////////
    //
    // 计算函数
    //
    ////////////////////////////////////////////////////////////////////////////

    public Date getTimeAfter(Date afterTime) {
        // 计算仅基于公历年份。
        Calendar cl = new GregorianCalendar(getTimeZone());
        // 向前移动一秒钟，因为我们正在计算给定时间之后的时间
        afterTime = new Date(afterTime.getTime() + 1000);
        // CronTrigger does not deal with milliseconds
        cl.setTime(afterTime);
        cl.set(Calendar.MILLISECOND, 0);
        boolean gotOne = false;
        // 循环直到我们计算出下一个时间，或者我们已经过了 endTime
        while (!gotOne) {
            //if (endTime != null && cl.getTime().after(endTime)) return null;
            if (cl.get(Calendar.YEAR) > 2999) {
                // 防止死循环...
                return null;
            }
            SortedSet<Integer> st = null;
            int t = 0;
            int sec = cl.get(Calendar.SECOND);
            int min = cl.get(Calendar.MINUTE);
            // 获得第二名.................................................
            st = seconds.tailSet(sec);
            if (st != null && st.size() != 0) {
                sec = st.first();
            } else {
                sec = seconds.first();
                min++;
                cl.set(Calendar.MINUTE, min);
            }
            cl.set(Calendar.SECOND, sec);
            min = cl.get(Calendar.MINUTE);
            int hr = cl.get(Calendar.HOUR_OF_DAY);
            t = -1;
            // 得到分钟.................................................
            st = minutes.tailSet(min);
            if (st != null && st.size() != 0) {
                t = min;
                min = st.first();
            } else {
                min = minutes.first();
                hr++;
            }
            if (min != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, min);
                setCalendarHour(cl, hr);
                continue;
            }
            cl.set(Calendar.MINUTE, min);
            hr = cl.get(Calendar.HOUR_OF_DAY);
            int day = cl.get(Calendar.DAY_OF_MONTH);
            t = -1;
            // 得到小时...................................................
            st = hours.tailSet(hr);
            if (st != null && st.size() != 0) {
                t = hr;
                hr = st.first();
            } else {
                hr = hours.first();
                day++;
            }
            if (hr != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.DAY_OF_MONTH, day);
                setCalendarHour(cl, hr);
                continue;
            }
            cl.set(Calendar.HOUR_OF_DAY, hr);
            day = cl.get(Calendar.DAY_OF_MONTH);
            int mon = cl.get(Calendar.MONTH) + 1;
            // '+ 1' 因为这个字段的日历是从。0 开始的，而我们是从 1 开始的
            t = -1;
            int tmon = mon;
            // 得到一天...................................................
            boolean dayOfMSpec = !daysOfMonth.contains(NO_SPEC);
            boolean dayOfWSpec = !daysOfWeek.contains(NO_SPEC);
            if (dayOfMSpec && !dayOfWSpec) { // get day by day of month rule
                st = daysOfMonth.tailSet(day);
                if (lastdayOfMonth) {
                    if (!nearestWeekday) {
                        t = day;
                        day = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                        day -= lastdayOffset;
                        if (t > day) {
                            mon++;
                            if (mon > 12) {
                                mon = 1;
                                tmon = 3333;
                                // 确保下面的 mon != tmon 测试失败
                                cl.add(Calendar.YEAR, 1);
                            }
                            day = 1;
                        }
                    } else {
                        t = day;
                        day = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                        day -= lastdayOffset;
                        Calendar tcal = Calendar.getInstance(getTimeZone());
                        tcal.set(Calendar.SECOND, 0);
                        tcal.set(Calendar.MINUTE, 0);
                        tcal.set(Calendar.HOUR_OF_DAY, 0);
                        tcal.set(Calendar.DAY_OF_MONTH, day);
                        tcal.set(Calendar.MONTH, mon - 1);
                        tcal.set(Calendar.YEAR, cl.get(Calendar.YEAR));
                        int ldom = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                        int dow = tcal.get(Calendar.DAY_OF_WEEK);
                        if (dow == Calendar.SATURDAY && day == 1) {
                            day += 2;
                        } else if (dow == Calendar.SATURDAY) {
                            day -= 1;
                        } else if (dow == Calendar.SUNDAY && day == ldom) {
                            day -= 2;
                        } else if (dow == Calendar.SUNDAY) {
                            day += 1;
                        }
                        tcal.set(Calendar.SECOND, sec);
                        tcal.set(Calendar.MINUTE, min);
                        tcal.set(Calendar.HOUR_OF_DAY, hr);
                        tcal.set(Calendar.DAY_OF_MONTH, day);
                        tcal.set(Calendar.MONTH, mon - 1);
                        Date nTime = tcal.getTime();
                        if (nTime.before(afterTime)) {
                            day = 1;
                            mon++;
                        }
                    }
                } else if (nearestWeekday) {
                    t = day;
                    day = daysOfMonth.first();
                    Calendar tcal = Calendar.getInstance(getTimeZone());
                    tcal.set(Calendar.SECOND, 0);
                    tcal.set(Calendar.MINUTE, 0);
                    tcal.set(Calendar.HOUR_OF_DAY, 0);
                    tcal.set(Calendar.DAY_OF_MONTH, day);
                    tcal.set(Calendar.MONTH, mon - 1);
                    tcal.set(Calendar.YEAR, cl.get(Calendar.YEAR));
                    int ldom = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    int dow = tcal.get(Calendar.DAY_OF_WEEK);
                    if (dow == Calendar.SATURDAY && day == 1) {
                        day += 2;
                    } else if (dow == Calendar.SATURDAY) {
                        day -= 1;
                    } else if (dow == Calendar.SUNDAY && day == ldom) {
                        day -= 2;
                    } else if (dow == Calendar.SUNDAY) {
                        day += 1;
                    }
                    tcal.set(Calendar.SECOND, sec);
                    tcal.set(Calendar.MINUTE, min);
                    tcal.set(Calendar.HOUR_OF_DAY, hr);
                    tcal.set(Calendar.DAY_OF_MONTH, day);
                    tcal.set(Calendar.MONTH, mon - 1);
                    Date nTime = tcal.getTime();
                    if (nTime.before(afterTime)) {
                        day = daysOfMonth.first();
                        mon++;
                    }
                } else if (st != null && st.size() != 0) {
                    t = day;
                    day = st.first();
                    // 确保我们不会超额运行一个较短的月份，比如 2 月
                    int lastDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    if (day > lastDay) {
                        day = daysOfMonth.first();
                        mon++;
                    }
                } else {
                    day = daysOfMonth.first();
                    mon++;
                }
                if (day != t || mon != tmon) {
                    cl.set(Calendar.SECOND, 0);
                    cl.set(Calendar.MINUTE, 0);
                    cl.set(Calendar.HOUR_OF_DAY, 0);
                    cl.set(Calendar.DAY_OF_MONTH, day);
                    cl.set(Calendar.MONTH, mon - 1);
                    // '- 1' 因为这个字段的日历是从 0 开始的，而我们是从 1 开始的
                    continue;
                }
            } else if (dayOfWSpec && !dayOfMSpec) {
                // 逐日获取规则
                if (lastdayOfWeek) { // 我们在寻找最后 XXX 天吗
                    // 这个月？
                    int dow = daysOfWeek.first(); // 想要的
                    // d-o-w
                    int cDow = cl.get(Calendar.DAY_OF_WEEK); // 当前 d-o-w
                    int daysToAdd = 0;
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    }
                    if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }
                    int lDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    if (day + daysToAdd > lDay) { // 我们已经错过了吗
                        // 最后一个？
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // 这里没有'-1'，因为我们正在宣传这个月
                        continue;
                    }
                    // 查找本月这一天最后一次出现的日期...
                    while ((day + daysToAdd + 7) <= lDay) {
                        daysToAdd += 7;
                    }
                    day += daysToAdd;
                    if (daysToAdd > 0) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' 这里是因为我们不宣传这个月
                        continue;
                    }
                } else if (nthdayOfWeek != 0) {
                    // 我们在寻找这个月的第 N 天吗？
                    int dow = daysOfWeek.first(); // 想要的
                    // d-o-w
                    int cDow = cl.get(Calendar.DAY_OF_WEEK); // 当前 d-o-w
                    int daysToAdd = 0;
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    } else if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }
                    boolean dayShifted = false;
                    if (daysToAdd > 0) {
                        dayShifted = true;
                    }
                    day += daysToAdd;
                    int weekOfMonth = day / 7;
                    if (day % 7 > 0) {
                        weekOfMonth++;
                    }
                    daysToAdd = (nthdayOfWeek - weekOfMonth) * 7;
                    day += daysToAdd;
                    if (daysToAdd < 0 || day > getLastDayOfMonth(mon, cl.get(Calendar.YEAR))) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // 这里没有'-1'，因为我们正在宣传这个月
                        continue;
                    } else if (daysToAdd > 0 || dayShifted) {
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' 在这里因为我们不宣传这个月
                        continue;
                    }
                } else {
                    int cDow = cl.get(Calendar.DAY_OF_WEEK); // 当前 d-o-w
                    int dow = daysOfWeek.first(); // 想要的
                    // d-o-w
                    st = daysOfWeek.tailSet(cDow);
                    if (st != null && st.size() > 0) {
                        dow = st.first();
                    }
                    int daysToAdd = 0;
                    if (cDow < dow) {
                        daysToAdd = dow - cDow;
                    }
                    if (cDow > dow) {
                        daysToAdd = dow + (7 - cDow);
                    }
                    int lDay = getLastDayOfMonth(mon, cl.get(Calendar.YEAR));
                    if (day + daysToAdd > lDay) { // 我们会结束吗
                        // 这个月？
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, 1);
                        cl.set(Calendar.MONTH, mon);
                        // 这里没有'-1'，因为我们正在宣传这个月
                        continue;
                    } else if (daysToAdd > 0) { // 我们在交换日子吗？
                        cl.set(Calendar.SECOND, 0);
                        cl.set(Calendar.MINUTE, 0);
                        cl.set(Calendar.HOUR_OF_DAY, 0);
                        cl.set(Calendar.DAY_OF_MONTH, day + daysToAdd);
                        cl.set(Calendar.MONTH, mon - 1);
                        // '- 1' 因为这个字段的日历是从 0 开始的，而我们是从 1 开始的
                        continue;
                    }
                }
            } else { // dayOfWSpec && !dayOfMSpec
                throw new UnsupportedOperationException("Support for specifying both a day-of-week AND a day-of-month parameter is not implemented.");
            }
            cl.set(Calendar.DAY_OF_MONTH, day);

            mon = cl.get(Calendar.MONTH) + 1;
            // '+ 1' 因为这个字段的日历是从 0 开始的，而我们是从 1 开始的
            int year = cl.get(Calendar.YEAR);
            t = -1;
            // 测试永远不会生成有效的触发日期但不断循环的表达式...
            if (year > MAX_YEAR) {
                return null;
            }
            // 得到月份...................................................
            st = months.tailSet(mon);
            if (st != null && st.size() != 0) {
                t = mon;
                mon = st.first();
            } else {
                mon = months.first();
                year++;
            }
            if (mon != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.HOUR_OF_DAY, 0);
                cl.set(Calendar.DAY_OF_MONTH, 1);
                cl.set(Calendar.MONTH, mon - 1);
                // '- 1' 因为这个字段的日历是从 0 开始的，而我们是从 1 开始的
                cl.set(Calendar.YEAR, year);
                continue;
            }
            cl.set(Calendar.MONTH, mon - 1);
            // '- 1' 因为这个字段的日历是从 0 开始的，而我们是从 1 开始的
            year = cl.get(Calendar.YEAR);
            t = -1;
            // 获取年份...................................................
            st = years.tailSet(year);
            if (st != null && st.size() != 0) {
                t = year;
                year = st.first();
            } else {
                return null; // 用完了几年...
            }
            if (year != t) {
                cl.set(Calendar.SECOND, 0);
                cl.set(Calendar.MINUTE, 0);
                cl.set(Calendar.HOUR_OF_DAY, 0);
                cl.set(Calendar.DAY_OF_MONTH, 1);
                cl.set(Calendar.MONTH, 0);
                // '- 1' 因为这个字段的日历是从 0 开始的，而我们是从 1 开始的
                cl.set(Calendar.YEAR, year);
                continue;
            }
            cl.set(Calendar.YEAR, year);

            gotOne = true;
        }
        // while( !done )
        return cl.getTime();
    }

    /**
     * 将日历提前到特定时间，特别注意夏令时问题。
     *
     * @param cal  要操作的日历
     * @param hour 设定的时间
     */
    protected void setCalendarHour(Calendar cal, int hour) {
        cal.set(Calendar.HOUR_OF_DAY, hour);
        if (cal.get(Calendar.HOUR_OF_DAY) != hour && hour != 24) {
            cal.set(Calendar.HOUR_OF_DAY, hour + 1);
        }
    }

    /**
     * 尚未实现：返回 <code>CronExpression</code> 匹配的给定时间之前的时间。
     */
    public Date getTimeBefore(Date endTime) {
        // FUTURE_TODO: implement QUARTZ-423
        return null;
    }

    /**
     * 尚未实现：返回 <code>CronExpression</code> 匹配的最后一次。
     */
    public Date getFinalFireTime() {
        // FUTURE_TODO：实施 QUARTZ-423
        return null;
    }

    protected boolean isLeapYear(int year) {
        return ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0));
    }

    protected int getLastDayOfMonth(int monthNum, int year) {
        switch (monthNum) {
            case 1:
                return 31;
            case 2:
                return (isLeapYear(year)) ? 29 : 28;
            case 3:
                return 31;
            case 4:
                return 30;
            case 5:
                return 31;
            case 6:
                return 30;
            case 7:
                return 31;
            case 8:
                return 31;
            case 9:
                return 30;
            case 10:
                return 31;
            case 11:
                return 30;
            case 12:
                return 31;
            default:
                throw new IllegalArgumentException("Illegal month number: " + monthNum);
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws java.io.IOException, ClassNotFoundException {
        stream.defaultReadObject();
        try {
            buildExpression(cronExpression);
        } catch (Exception ignore) {
        } // never happens
    }

    @Override
    @Deprecated
    public Object clone() {
        return new CronExpression(this);
    }
}

class ValueSet {
    public int value;

    public int pos;
}
