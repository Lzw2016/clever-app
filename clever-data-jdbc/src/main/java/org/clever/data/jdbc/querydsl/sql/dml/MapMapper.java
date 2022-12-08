package org.clever.data.jdbc.querydsl.sql.dml;

import com.querydsl.core.QueryException;
import com.querydsl.core.types.Path;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.dml.AbstractMapper;
import com.querydsl.sql.types.Null;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.NamingUtils;
import org.clever.core.RenameStrategy;
import org.clever.data.jdbc.querydsl.utils.SQLClause;
import org.clever.util.LinkedCaseInsensitiveMap;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * querydsl中使用Map对象传参数: <br/>
 * <pre>{@code
 *  InsertClause.populate(map, MapMapper.DEFAULT)
 *  UpdateClause.populate(map, MapMapper.DEFAULT)
 * }</pre>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/01/29 10:29 <br/>
 */
public class MapMapper extends AbstractMapper<Map<String, ?>> {
    public static final MapMapper DEFAULT = new MapMapper(false);
    public static final MapMapper WITH_NULL_BINDINGS = new MapMapper(true);

    private final boolean withNullBindings;

    public MapMapper() {
        this(false);
    }

    public MapMapper(boolean withNullBindings) {
        this.withNullBindings = withNullBindings;
    }

    @Override
    public Map<Path<?>, Object> createMap(RelationalPath<?> entity, Map<String, ?> dataMap) {
        try {
            Map<Path<?>, Object> values = new LinkedHashMap<>();
            Map<String, Path<?>> columns = getColumns(entity);
            LinkedCaseInsensitiveMap<Object> caseInsensitiveMap = new LinkedCaseInsensitiveMap<>(dataMap.size());
            caseInsensitiveMap.putAll(dataMap);
            for (Map.Entry<String, Path<?>> entry : columns.entrySet()) {
                String fieldName = entry.getKey();
                Path<?> path = entry.getValue();
                Object propertyValue = dataMap.get(fieldName);
                if (propertyValue == null) {
                    propertyValue = caseInsensitiveMap.get(fieldName);
                }
                // 全小写/大写下划线
                if (propertyValue == null) {
                    String name = NamingUtils.rename(fieldName, RenameStrategy.ToUnderline);
                    propertyValue = dataMap.get(name);
                    if (propertyValue == null) {
                        propertyValue = caseInsensitiveMap.get(name);
                    }
                    if (propertyValue == null) {
                        propertyValue = dataMap.get(StringUtils.upperCase(name));
                    }
                    if (propertyValue == null) {
                        propertyValue = caseInsensitiveMap.get(StringUtils.upperCase(name));
                    }
                }
                // 小写驼峰
                if (propertyValue == null) {
                    String name = NamingUtils.rename(fieldName, RenameStrategy.ToCamel);
                    propertyValue = dataMap.get(name);
                    if (propertyValue == null) {
                        propertyValue = caseInsensitiveMap.get(name);
                    }
                }
                propertyValue = SQLClause.getFieldValue(path, propertyValue);
                if (propertyValue == null) {
                    if (!withNullBindings) {
                        continue;
                    }
                    propertyValue = Null.DEFAULT;
                }
                values.put(path, propertyValue);
            }
            return values;
        } catch (Exception e) {
            throw new QueryException(e);
        }
    }
}
