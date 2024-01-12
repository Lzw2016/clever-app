package org.clever.data.jdbc.mybatis;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.clever.core.RenameStrategy;
import org.clever.data.jdbc.mybatis.annotations.Mapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/11 17:29 <br/>
 */
@Builder
@EqualsAndHashCode
@Getter
public class MapperMethodInfo {
    /**
     * Mapper原始函数
     */
    private final Method method;

    // --------------------------------------------------------------------------------------------
    // Mapper
    // --------------------------------------------------------------------------------------------
    /**
     * Mapper xml文件路径
     */
    private final String mapperPath;
    /**
     * 对应 Mapper xml文件的 sql id
     */
    private final String sqlId;
    /**
     * 执行的操作类型
     */
    private final Mapper.Ops ops;
    /**
     * 返回的表字段重命名策略
     */
    private final RenameStrategy rename;
    /**
     * 当查询一条数据时(Mapper返回值不是集合)，是否使用 queryFirst。 <br/>
     * 值为 false 表示需要使用 queryOne
     */
    private final boolean first;
    /**
     * 当Mapper返回值是数值类型时(int/Integer/long/Long)，是否使用 SQL Count 查询获取查询结果数据量
     */
    private final boolean count;
    /**
     * 设置游标读取查询数据时的批次大小(小于等于0表示不设置)
     */
    private final int batchSize;

    // --------------------------------------------------------------------------------------------
    // return
    // --------------------------------------------------------------------------------------------
    /**
     * 是否无返回值
     */
    private final boolean returnVoid;
    /**
     * 返回类型是 List 集合
     */
    private final boolean returnList;
    /**
     * 创建返回 List 对象的函数(可能为null)
     */
    private final CreateObject<List<?>> newList;
    /**
     * 返回类型是 Set 集合
     */
    private final boolean returnSet;
    /**
     * 创建返回 Set 对象的函数(可能为null)
     */
    private final CreateObject<Set<?>> newSet;
    /**
     * 返回类型是数组(Array)
     */
    private final boolean returnArray;
    /**
     * 返回类型是Map
     */
    private final boolean returnMap;
    /**
     * 创建返回 Map 对象的函数(可能为null)
     */
    private final CreateObject<Map<?, ?>> newMap;
    /**
     * 返回一个简单类型(基本类型或者实体类)
     */
    private final boolean returnSimple;
    /**
     * returnList、returnSet、returnArray时, 元素项类型
     */
    private final Class<?> returnItemType;
    /**
     * returnItemType 是否是Map类型
     */
    private final boolean returnItemMap;
    /**
     * returnItemMap 时，创建 Item Map 对象的函数(可能为null)
     */
    private final CreateObject<Map<?, ?>> newItemMap;
    /**
     * returnList、returnSet、returnArray时, 元素项类型是DbColumnMetaData(使用queryMetaData)
     */
    private final boolean queryMetaData;

    // --------------------------------------------------------------------------------------------
    // param
    // --------------------------------------------------------------------------------------------
    /**
     * SQL参数信息 {@code Map<idx, name>}
     */
    private final Map<Integer, String> params;
    /**
     * QueryBySort 类型参数的位置
     */
    private final Integer sortParamIdx;
    /**
     * QueryByPage 类型参数的位置
     */
    private final Integer pageParamIdx;
    /**
     * 使用游标读取数据时，回调参数的位置
     */
    private final Integer cursorParamIdx;
    /**
     * 使用游标读取数据时，回调方式是Consumer(Function/Consumer)
     */
    private final boolean cursorParamConsumer;
    /**
     * 使用游标读取数据时，是否使用 BatchData
     */
    private final boolean cursorUseBatch;

    public MapperMethodInfo(Method method,
                            String mapperPath,
                            String sqlId,
                            Mapper.Ops ops,
                            RenameStrategy rename,
                            boolean first,
                            boolean count,
                            int batchSize,
                            boolean returnVoid,
                            boolean returnList,
                            CreateObject<List<?>> newList,
                            boolean returnSet,
                            CreateObject<Set<?>> newSet,
                            boolean returnArray,
                            boolean returnMap,
                            CreateObject<Map<?, ?>> newMap,
                            boolean returnSimple,
                            Class<?> returnItemType,
                            boolean returnItemMap,
                            CreateObject<Map<?, ?>> newItemMap,
                            boolean queryMetaData,
                            Map<Integer, String> params,
                            Integer sortParamIdx,
                            Integer pageParamIdx,
                            Integer cursorParamIdx,
                            boolean cursorParamConsumer,
                            boolean cursorUseBatch) {
        this.method = method;
        this.mapperPath = mapperPath;
        this.sqlId = sqlId;
        this.ops = ops;
        this.rename = rename;
        this.first = first;
        this.count = count;
        this.batchSize = batchSize;
        this.returnVoid = returnVoid;
        this.returnList = returnList;
        this.newList = newList;
        this.returnSet = returnSet;
        this.newSet = newSet;
        this.returnArray = returnArray;
        this.returnMap = returnMap;
        this.newMap = newMap;
        this.returnSimple = returnSimple;
        this.returnItemType = returnItemType;
        this.returnItemMap = returnItemMap;
        this.newItemMap = newItemMap;
        this.queryMetaData = queryMetaData;
        this.params = params;
        this.sortParamIdx = sortParamIdx;
        this.pageParamIdx = pageParamIdx;
        this.cursorParamIdx = cursorParamIdx;
        this.cursorParamConsumer = cursorParamConsumer;
        this.cursorUseBatch = cursorUseBatch;
    }
}
