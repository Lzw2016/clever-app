package org.clever.data.jdbc.querydsl.sql.types;

import com.querydsl.sql.types.AbstractType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * 处理 oracle nvarchar 类型
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/12 15:58 <br/>
 */
public class NStringType extends AbstractType<String> {
    public static final NStringType SINGLETON = new NStringType();

    public NStringType() {
        super(Types.NVARCHAR);
    }

    public NStringType(int type) {
        super(type);
    }

    @Override
    public String getValue(ResultSet rs, int startIndex) throws SQLException {
        return rs.getNString(startIndex);
    }

    @Override
    public Class<String> getReturnedClass() {
        return String.class;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, String value) throws SQLException {
        st.setNString(startIndex, value);
    }
}
