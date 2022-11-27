package org.clever.data.jdbc.support;

import lombok.SneakyThrows;
import org.clever.data.jdbc.type.*;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/09 16:32 <br/>
 */
public class JdbcTypeMappingUtils {
    private static final Map<Integer, TypeHandler<?>> DEFAULT_TYPE_MAPPING;

    static {
        IntegerTypeHandler integerTypeHandler = new IntegerTypeHandler();
        LongTypeHandler longTypeHandler = new LongTypeHandler();
        DoubleTypeHandler doubleTypeHandler = new DoubleTypeHandler();
        StringTypeHandler stringTypeHandler = new StringTypeHandler();
        ClobTypeHandler clobTypeHandler = new ClobTypeHandler();
        NStringTypeHandler nStringTypeHandler = new NStringTypeHandler();
        NClobTypeHandler nClobTypeHandler = new NClobTypeHandler();
        SqlxmlTypeHandler sqlxmlTypeHandler = new SqlxmlTypeHandler();
        BooleanTypeHandler booleanTypeHandler = new BooleanTypeHandler();
        BigDecimalTypeHandler bigDecimalTypeHandler = new BigDecimalTypeHandler();
        DateOnlyTypeHandler dateOnlyTypeHandler = new DateOnlyTypeHandler();
        TimeOnlyTypeHandler timeOnlyTypeHandler = new TimeOnlyTypeHandler();
        SqlTimestampTypeHandler sqlTimestampTypeHandler = new SqlTimestampTypeHandler();
        BlobTypeHandler blobTypeHandler = new BlobTypeHandler();
        BlobByteObjectArrayTypeHandler blobByteObjectArrayTypeHandler = new BlobByteObjectArrayTypeHandler();
        // 注册映射关系
        Map<Integer, TypeHandler<?>> defaultTypeMapping = new HashMap<>();
        // Integer
        defaultTypeMapping.put(Types.TINYINT, integerTypeHandler);
        defaultTypeMapping.put(Types.SMALLINT, integerTypeHandler);
        defaultTypeMapping.put(Types.INTEGER, integerTypeHandler);
        // Long
        defaultTypeMapping.put(Types.BIGINT, longTypeHandler);
        // Double
        defaultTypeMapping.put(Types.FLOAT, doubleTypeHandler);
        defaultTypeMapping.put(Types.DOUBLE, doubleTypeHandler);
        // String
        defaultTypeMapping.put(Types.CHAR, stringTypeHandler);
        defaultTypeMapping.put(Types.VARCHAR, stringTypeHandler);
        defaultTypeMapping.put(Types.LONGVARCHAR, stringTypeHandler);
        defaultTypeMapping.put(Types.CLOB, clobTypeHandler);
        defaultTypeMapping.put(Types.NCHAR, nStringTypeHandler);
        defaultTypeMapping.put(Types.NVARCHAR, nStringTypeHandler);
        defaultTypeMapping.put(Types.NCLOB, nClobTypeHandler);
        defaultTypeMapping.put(Types.SQLXML, sqlxmlTypeHandler);
        // Boolean
        defaultTypeMapping.put(Types.BIT, booleanTypeHandler);
        defaultTypeMapping.put(Types.BOOLEAN, booleanTypeHandler);
        // BigDecimal
        defaultTypeMapping.put(Types.REAL, bigDecimalTypeHandler);
        defaultTypeMapping.put(Types.NUMERIC, bigDecimalTypeHandler);
        defaultTypeMapping.put(Types.DECIMAL, bigDecimalTypeHandler);
        // java.util.Date
        defaultTypeMapping.put(Types.DATE, dateOnlyTypeHandler);
        defaultTypeMapping.put(Types.TIME, timeOnlyTypeHandler);
        // java.sql.Timestamp
        defaultTypeMapping.put(Types.TIMESTAMP, sqlTimestampTypeHandler);
        // byte[]
        defaultTypeMapping.put(Types.BINARY, blobTypeHandler);
        defaultTypeMapping.put(Types.VARBINARY, blobByteObjectArrayTypeHandler);
        defaultTypeMapping.put(Types.LONGVARBINARY, blobByteObjectArrayTypeHandler);
        defaultTypeMapping.put(Types.BLOB, blobByteObjectArrayTypeHandler);
        // 映射配置不可以变
        DEFAULT_TYPE_MAPPING = Collections.unmodifiableMap(defaultTypeMapping);
    }

    @SneakyThrows
    public static Object getColumnType(ResultSet rs, int columnIndex, Object jdbcObj) {
        int columnType = rs.getMetaData().getColumnType(columnIndex);
        TypeHandler<?> typeHandler = DEFAULT_TYPE_MAPPING.get(columnType);
        if (typeHandler == null) {
            return jdbcObj;
        }
        return typeHandler.getResult(rs, columnIndex);
    }
}

