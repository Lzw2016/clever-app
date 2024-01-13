package org.clever.data.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.support.DefaultConversionService;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.IPage;
import org.clever.data.dynamic.sql.BoundSql;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.jdbc.mybatis.MapperMethodInfo;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.mybatis.annotations.Mapper;
import org.clever.data.jdbc.mybatis.utils.MyBatisMapperUtils;
import org.clever.data.jdbc.support.BatchData;
import org.clever.data.jdbc.support.InsertResult;
import org.clever.data.jdbc.support.RowData;
import org.clever.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * MyBatis Mapper 动态代理实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/10 13:57 <br/>
 */
@Slf4j
public class MyBatisMapperHandler implements InvocationHandler {
    private static final Mapper DEF_CONFIG = (Mapper) Proxy.newProxyInstance(
        Thread.currentThread().getContextClassLoader(),
        new Class[]{Mapper.class},
        (proxy, method, args) -> method.getDefaultValue()
    );
    /**
     * MapperMethodInfo 对象缓存 {@code Map<method signature, MapperMethodInfo>}
     */
    private static final Map<String, MapperMethodInfo> METHOD_INFO_CACHE = new HashMap<>();
    /**
     * mapper class
     */
    private final Class<?> clazz;
    /**
     * 项目列表(优选级由高到底)
     */
    private final List<String> projects;
    /**
     * Mapper动态SQL
     */
    private final MyBatisMapperSql mapperSql;
    /**
     * JDBC数据源
     */
    private final Jdbc jdbc;
    /**
     * mapper class 上的 Mapper 配置
     */
    private final Mapper clazzMapper;

    public MyBatisMapperHandler(Class<?> clazz, List<String> projects, MyBatisMapperSql mapperSql, Jdbc jdbc) {
        Assert.notNull(clazz, "参数 clazz 不能为 null");
        Assert.notNull(mapperSql, "参数 mapperSql 不能为 null");
        Assert.notNull(jdbc, "参数 jdbc 不能为 null");
        this.clazz = clazz;
        if (projects == null) {
            projects = Collections.emptyList();
        }
        this.projects = Collections.unmodifiableList(projects);
        this.mapperSql = mapperSql;
        this.jdbc = jdbc;
        this.clazzMapper = clazz.getAnnotation(Mapper.class);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 处理 Object 自带函数
        if (Object.class.equals(method.getDeclaringClass())) {
            try {
                return method.invoke(this, args);
            } catch (Throwable e) {
                throw ExceptionUtils.unchecked(e);
            }
        }
        // 获取 Mapper Method 信息
        MapperMethodInfo methodInfo = getMethodInfo(method);
        // 获取 SQL 信息
        Map<String, Object> parameter = new HashMap<>(methodInfo.getParams().size());
        methodInfo.getParams().forEach((idx, name) -> parameter.put(name, args[idx]));
        SqlSource sqlSource = mapperSql.getSqlSource(
            methodInfo.getSqlId(), methodInfo.getMapperPath(), jdbc.getDbType(), projects.toArray(new String[0])
        );
        Assert.notNull(sqlSource, "SQL不存在, sqlId=" + methodInfo.getSqlId() + ", file=" + methodInfo.getMapperPath());
        final BoundSql boundSql = sqlSource.getBoundSql(jdbc.getDbType(), parameter);
        // 执行 SQL
        final String errMsgSuffix = "Method=(class=" + method.getDeclaringClass().getName() + ", method=" + method.getName() + ")";
        Mapper.Ops ops = methodInfo.getOps();
        if (Objects.equals(ops, Mapper.Ops.Auto)) {
            ops = autoOps(methodInfo);
        }
        switch (ops) {
            case Query:
                return query(methodInfo, boundSql.getNamedParameterSql(), boundSql.getParameterMap(), args, errMsgSuffix);
            case Update:
                return update(methodInfo, boundSql.getNamedParameterSql(), boundSql.getParameterMap(), args, errMsgSuffix);
            case Call:
                return call(methodInfo, boundSql.getNamedParameterSql(), boundSql.getParameterMap(), args, errMsgSuffix);
            default:
                throw new UnsupportedOperationException("无效的 Mapper.Ops=" + methodInfo.getOps());
        }
    }

    private Mapper.Ops autoOps(MapperMethodInfo methodInfo) {
        final Class<?> returnType = methodInfo.getMethod().getReturnType();
        // returnVoid
        if (methodInfo.isReturnVoid()) {
            if (methodInfo.getCursorParamIdx() == null) {
                return Mapper.Ops.Call;
            }
        }
        // returnList returnSet returnArray
        if (methodInfo.isReturnList() || methodInfo.isReturnSet() || methodInfo.isReturnArray()) {
            if (MyBatisMapperUtils.isIntType(methodInfo.getReturnItemType())) {
                return Mapper.Ops.Update;
            }
        }
        // returnSimple
        if (methodInfo.isReturnSimple()) {
            if (Objects.equals(returnType, InsertResult.class)) {
                return Mapper.Ops.Update;
            }
        }
        return Mapper.Ops.Query;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object query(MapperMethodInfo methodInfo, String sql, Map<String, Object> parameter, Object[] args, String errMsgSuffix) throws Throwable {
        final Class<?> returnType = methodInfo.getMethod().getReturnType();
        // returnVoid -> queryForCursor
        if (methodInfo.isReturnVoid()) {
            Assert.notNull(methodInfo.getCursorParamIdx(), "Mapper.Ops=Query且return void时, 必须设置Function<BatchData/RowData, Boolean>、Consumer<BatchData/RowData>回调参数, " + errMsgSuffix);
            Object callback = args[methodInfo.getCursorParamIdx()];
            // Consumer<BatchData/RowData>
            if (methodInfo.isCursorParamConsumer()) {
                if (methodInfo.isCursorUseBatch() && methodInfo.getBatchSize() > 0) {
                    jdbc.queryForCursor(sql, parameter, methodInfo.getBatchSize(), (Consumer<BatchData>) callback, methodInfo.getRename());
                } else {
                    jdbc.queryForCursor(sql, parameter, (Consumer<RowData>) callback, methodInfo.getRename());
                }
            } else {
                // Function<BatchData/RowData, Boolean>
                if (methodInfo.isCursorUseBatch() && methodInfo.getBatchSize() > 0) {
                    jdbc.queryForCursor(sql, parameter, methodInfo.getBatchSize(), (Function<BatchData, Boolean>) callback, methodInfo.getRename());
                } else {
                    jdbc.queryForCursor(sql, parameter, (Function<RowData, Boolean>) callback, methodInfo.getRename());
                }
            }
            return null;
        }
        final QueryByPage queryByPage = methodInfo.getPageParamIdx() == null ? null : (QueryByPage) args[methodInfo.getPageParamIdx()];
        final QueryBySort queryBySort = methodInfo.getSortParamIdx() == null ? null : (QueryBySort) args[methodInfo.getSortParamIdx()];
        // returnList returnSet returnArray -> queryMany(queryBySort、queryByPage、queryMetaData)
        if (methodInfo.isReturnList() || methodInfo.isReturnSet() || methodInfo.isReturnArray()) {
            List<?> list;
            if (methodInfo.isReturnItemMap() || methodInfo.getReturnItemType() == null) {
                if (queryByPage != null) {
                    IPage<Map<String, Object>> page = jdbc.queryByPage(sql, queryByPage, parameter, methodInfo.getRename());
                    list = page.getRecords();
                } else if (queryBySort != null) {
                    list = jdbc.queryBySort(sql, queryBySort, parameter, methodInfo.getRename());
                } else {
                    list = jdbc.queryMany(sql, parameter, methodInfo.getRename());
                }
                // 应用 Map 类型
                if (methodInfo.getNewItemMap() != null
                    && methodInfo.getReturnItemType() != null
                    && !list.isEmpty()
                    && !methodInfo.getReturnItemType().isAssignableFrom(list.get(0).getClass())) {
                    List<Object> newList = new ArrayList<>(list.size());
                    for (Object row : list) {
                        Map<Object, Object> newRow = methodInfo.getNewItemMap().create();
                        newRow.putAll((Map<?, ?>) row);
                        newList.add(newRow);
                    }
                    list = newList;
                }
            } else if (methodInfo.isQueryMetaData()) {
                list = jdbc.queryMetaData(sql, parameter, methodInfo.getRename());
            } else {
                Assert.isFalse(MyBatisMapperUtils.isBaseType(methodInfo.getReturnItemType()), "Mapper.Ops=Query时, 返回的集合项类型不能是基本类型, 必须是Map或Entity, " + errMsgSuffix);
                if (queryByPage != null) {
                    IPage<?> page = jdbc.queryByPage(sql, queryByPage, parameter, methodInfo.getReturnItemType());
                    list = page.getRecords();
                } else if (queryBySort != null) {
                    list = jdbc.queryBySort(sql, queryBySort, parameter, methodInfo.getReturnItemType());
                } else {
                    list = jdbc.queryMany(sql, parameter, methodInfo.getReturnItemType());
                }
            }
            // 应用返回集合类型 List | Set | []
            if (!returnType.isAssignableFrom(list.getClass())) {
                if (methodInfo.isReturnList() && methodInfo.getNewList() != null) {
                    List<Object> newList = methodInfo.getNewList().create();
                    newList.addAll(list);
                    list = newList;
                } else if (methodInfo.isReturnSet() && methodInfo.getNewSet() != null) {
                    Set<Object> set = methodInfo.getNewSet().create();
                    set.addAll(list);
                    return set;
                } else if (methodInfo.isReturnArray()) {
                    return list.toArray();
                }
            }
            return list;
        }
        // returnMap -> queryOne、queryFirst
        if (methodInfo.isReturnMap()) {
            Map<String, Object> map;
            if (methodInfo.isFirst()) {
                map = jdbc.queryFirst(sql, parameter, methodInfo.getRename());
            } else {
                map = jdbc.queryOne(sql, parameter, methodInfo.getRename());
            }
            // 应用 Map 类型
            if (methodInfo.getNewMap() != null && !returnType.isAssignableFrom(map.getClass())) {
                Map<Object, Object> newMap = methodInfo.getNewMap().create();
                newMap.putAll(map);
                map = (Map) newMap;
            }
            return map;
        }
        // returnSimple -> queryCount
        final ConversionService conversionService = DefaultConversionService.getSharedInstance();
        if (MyBatisMapperUtils.isIntType(returnType)) {
            Object res = jdbc.queryCount(sql, parameter);
            return conversionService.convert(res, returnType);
        }
        // returnSimple -> queryOne、queryFirst
        if (methodInfo.isFirst()) {
            return jdbc.queryFirst(sql, parameter, returnType);
        }
        return jdbc.queryOne(sql, parameter, returnType);
    }

    private Object update(MapperMethodInfo methodInfo, String sql, Map<String, Object> parameter, Object[] args, String errMsgSuffix) {
        final Class<?> returnType = methodInfo.getMethod().getReturnType();
        final ConversionService conversionService = DefaultConversionService.getSharedInstance();
        // returnSimple -> update、insert
        if (methodInfo.isReturnSimple()) {
            Object res;
            if (Objects.equals(returnType, InsertResult.class)) {
                res = jdbc.insert(sql, parameter);
            } else {
                Assert.isTrue(MyBatisMapperUtils.isIntType(returnType), "Mapper.Ops=Update时, 返回值只能是int/Integer/long/Long, " + errMsgSuffix);
                res = jdbc.update(sql, parameter);
                res = conversionService.convert(res, returnType);
            }
            return res;
        }
        // returnArray -> batchUpdate
        if ((methodInfo.isReturnList() || methodInfo.isReturnArray()) && methodInfo.isParamOnlyList()) {
            Assert.isTrue(MyBatisMapperUtils.isIntType(methodInfo.getReturnItemType()), "Mapper.Ops=Update且return List/Array时, List/Array元素项只能是int/Integer/long/Long类型, " + errMsgSuffix);
            int[] res = jdbc.batchUpdate(sql, (List<?>) args[0]);
            return Arrays.stream(res).mapToObj(num -> conversionService.convert(num, methodInfo.getReturnItemType())).toArray();
        }
        throw new IllegalArgumentException("Mapper.Ops=Update时, 返回值只能是Number(int/Integer/long/Long)、List<Number>、Number[]类型, " + errMsgSuffix);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object call(MapperMethodInfo methodInfo, String sql, Map<String, Object> parameter, Object[] args, String errMsgSuffix) throws Throwable {
        final Class<?> returnType = methodInfo.getMethod().getReturnType();
        // returnVoid -> call
        if (methodInfo.isReturnVoid()) {
            jdbc.call(sql, args);
            return null;
        }
        // returnMap -> callGet
        if (methodInfo.isReturnMap()) {
            Map<String, Object> map = jdbc.callGet(sql, parameter);
            // 应用 Map 类型
            if (methodInfo.getNewMap() != null && !returnType.isAssignableFrom(map.getClass())) {
                Map<Object, Object> newMap = methodInfo.getNewMap().create();
                newMap.putAll(map);
                map = (Map) newMap;
            }
            return map;
        }
        throw new IllegalArgumentException("Mapper.Ops=Call时, 只能返回void、Map, " + errMsgSuffix);
    }

    private MapperMethodInfo getMethodInfo(Method method) {
        final String key = MyBatisMapperUtils.signature(method);
        boolean needAdd = true;
        MapperMethodInfo methodInfo = METHOD_INFO_CACHE.get(key);
        if (methodInfo != null && MyBatisMapperUtils.sameClassLoader(method, methodInfo.getMethod())) {
            needAdd = false;
        }
        if (!needAdd) {
            return methodInfo;
        }
        synchronized (METHOD_INFO_CACHE) {
            // 二次确认
            methodInfo = METHOD_INFO_CACHE.get(key);
            if (methodInfo != null && MyBatisMapperUtils.sameClassLoader(method, methodInfo.getMethod())) {
                return methodInfo;
            }
            // 创建新的 MapperMethodInfo
            final Mapper config = method.getAnnotation(Mapper.class);
            MapperMethodInfo.MapperMethodInfoBuilder builder = MapperMethodInfo.builder()
                .mapperPath(getMapperPath(config))
                .sqlId(getSqlId(config, method))
                .ops(getOps(config))
                .rename(getRename(config))
                .first(getFirst(config))
                .count(getCount(config))
                .batchSize(getBatchSize(config));
            MyBatisMapperUtils.fillMethodReturn(method, builder);
            MyBatisMapperUtils.fillMethodParams(method, builder);
            methodInfo = builder.build();
            METHOD_INFO_CACHE.put(key, methodInfo);
        }
        return methodInfo;
    }

    private String getMapperPath(Mapper mapper) {
        if (mapper != null && StringUtils.isNotBlank(mapper.mapperPath())) {
            return mapper.mapperPath();
        }
        if (clazzMapper != null && StringUtils.isNotBlank(clazzMapper.mapperPath())) {
            return clazzMapper.mapperPath();
        }
        return StringUtils.replace(clazz.getName(), ".", "/") + ".xml";
    }

    private String getSqlId(Mapper mapper, Method method) {
        if (mapper != null && StringUtils.isNotBlank(mapper.sqlId())) {
            return mapper.sqlId();
        }
        if (clazzMapper != null && StringUtils.isNotBlank(clazzMapper.sqlId())) {
            return clazzMapper.sqlId();
        }
        return method.getName();
    }

    private Mapper.Ops getOps(Mapper mapper) {
        if (mapper != null && mapper.ops() != null) {
            return mapper.ops();
        }
        if (clazzMapper != null && clazzMapper.ops() != null) {
            return clazzMapper.ops();
        }
        return DEF_CONFIG.ops();
    }

    private RenameStrategy getRename(Mapper mapper) {
        if (mapper != null && mapper.rename() != null) {
            return mapper.rename();
        }
        if (clazzMapper != null && clazzMapper.rename() != null) {
            return clazzMapper.rename();
        }
        return DEF_CONFIG.rename();
    }

    private boolean getFirst(Mapper mapper) {
        if (mapper != null) {
            return mapper.first();
        }
        if (clazzMapper != null) {
            return clazzMapper.first();
        }
        return DEF_CONFIG.first();
    }

    private boolean getCount(Mapper mapper) {
        if (mapper != null) {
            return mapper.count();
        }
        if (clazzMapper != null) {
            return clazzMapper.count();
        }
        return DEF_CONFIG.count();
    }

    private int getBatchSize(Mapper mapper) {
        if (mapper != null) {
            return mapper.batchSize();
        }
        if (clazzMapper != null) {
            return clazzMapper.batchSize();
        }
        return DEF_CONFIG.batchSize();
    }
}
