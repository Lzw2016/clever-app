package org.clever.data.jdbc.p6spy;

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.clever.data.jdbc.support.SqlLoggerUtils;

/**
 * 格式化 sql 日志
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/12/11 22:32 <br/>
 */
@Data
public class P6SpyFormatter implements MessageFormattingStrategy {
    /**
     * 标记慢SQL的执行时间(毫秒)
     */
    private volatile int slow = 800;

    /**
     * @param connectionId 连接数据库的id
     * @param now          当前time的毫秒数
     * @param elapsed      操作完成所需的时间(以毫秒为单位)
     * @param category     操作的类别
     * @param prepared     预编译的sql语句
     * @param sql          执行的sql语句
     * @param url          执行sql语句的数据库url
     */
    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        if (Category.COMMIT.getName().equalsIgnoreCase(category)) {
            return String.format(
                "id=%s %s %sms",
                StringUtils.rightPad(String.valueOf(connectionId), 3),
                StringUtils.rightPad(category, 9),
                StringUtils.leftPad(String.valueOf(elapsed), 4)
            );
        }
        // P6Util.singleLine(sql)
        return String.format(
            elapsed >= slow ? "id=%s %s %sms [slow]%s" : "id=%s %s %sms %s",
            StringUtils.rightPad(String.valueOf(connectionId), 3),
            StringUtils.rightPad(category, 9),
            StringUtils.leftPad(String.valueOf(elapsed), 4),
            SqlLoggerUtils.deleteWhitespace(sql)
        );
    }
}
