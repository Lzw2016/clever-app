package org.clever.data.jdbc.support;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCountCallbackHandler;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 可中断的行数据回调实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/03 23:32 <br/>
 */
public class InterruptRowCallbackHandler extends RowCountCallbackHandler implements ResultSetExtractor<Void> {
    /**
     * 是否已经中断数据回调
     */
    protected boolean interrupted = false;

    @Override
    public Void extractData(ResultSet rs) throws SQLException, DataAccessException {
        processStart();
        while (rs.next()) {
            this.processRow(rs);
            if (interrupted) {
                break;
            }
        }
        processEnd();
        return null;
    }

    /**
     * 处理数据行之前的回调
     */
    public void processStart() {
    }

    /**
     * 处理数据行之后的回调
     */
    public void processEnd() {
    }
}
