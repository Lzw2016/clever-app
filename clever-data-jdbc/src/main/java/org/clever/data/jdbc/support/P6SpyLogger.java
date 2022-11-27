package org.clever.data.jdbc.support;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.apache.commons.lang3.StringUtils;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/11 22:32 <br/>
 */
public class P6SpyLogger implements MessageFormattingStrategy {
    /**
     * @param connectionId 连接数据库的id
     * @param now          当前time的毫秒数
     * @param elapsed      操作完成所需的时间(以毫秒为单位)
     * @param category     操作的类别
     * @param prepared     将所有绑定变量替换为实际值的sql语句
     * @param sql          执行的sql语句
     * @param url          执行sql语句的数据库url
     */
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if ("commit".equalsIgnoreCase(category)) {
            return String.format(
                    "id=%s %s %sms",
                    StringUtils.rightPad(String.valueOf(connectionId), 3),
                    StringUtils.rightPad(category, 9),
                    StringUtils.leftPad(String.valueOf(elapsed), 4)
            );
        }
        // P6Util.singleLine(sql)
        return String.format(
                elapsed >= 800 ? "id=%s %s %sms [slow]%s" : "id=%s %s %sms %s",
                StringUtils.rightPad(String.valueOf(connectionId), 3),
                StringUtils.rightPad(category, 9),
                StringUtils.leftPad(String.valueOf(elapsed), 4),
                SqlLoggerUtils.deleteWhitespace(sql)
        );
    }
}
