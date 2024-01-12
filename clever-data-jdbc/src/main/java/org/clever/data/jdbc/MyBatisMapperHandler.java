package org.clever.data.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.RenameStrategy;
import org.clever.core.exception.ExceptionUtils;
import org.clever.data.jdbc.mybatis.MapperMethodInfo;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.mybatis.annotations.Mapper;
import org.clever.data.jdbc.mybatis.utils.MyBatisMapperUtils;
import org.clever.util.Assert;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

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

    public MyBatisMapperHandler(Class<?> clazz, MyBatisMapperSql mapperSql, Jdbc jdbc) {
        Assert.notNull(clazz, "参数 clazz 不能为 null");
        Assert.notNull(mapperSql, "参数 mapperSql 不能为 null");
        Assert.notNull(jdbc, "参数 jdbc 不能为 null");
        this.clazz = clazz;
        this.mapperSql = mapperSql;
        this.jdbc = jdbc;
        this.clazzMapper = clazz.getAnnotation(Mapper.class);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 处理 Object 自带函数
        if (Object.class.equals(method.getDeclaringClass())) {
            try {
                return method.invoke(this, args);
            } catch (Throwable e) {
                throw ExceptionUtils.unchecked(e);
            }
        }
        MapperMethodInfo methodInfo = getMethodInfo(method);
        // 判断使用
        // queryMetaData
        // queryCount
        // queryOne、queryFirst
        // queryMany
        // queryBySort、queryByPage、queryForCursor
        // callGet、call
        // update、batchUpdate、insert
        log.info("--> {}", methodInfo);
        return null;
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
