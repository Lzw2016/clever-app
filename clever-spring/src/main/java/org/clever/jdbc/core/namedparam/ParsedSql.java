package org.clever.jdbc.core.namedparam;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存有关已解析SQL语句的信息。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:51 <br/>
 */
public class ParsedSql {
    private final String originalSql;
    private final List<String> parameterNames = new ArrayList<>();
    private final List<int[]> parameterIndexes = new ArrayList<>();
    private int namedParameterCount;
    private int unnamedParameterCount;
    private int totalParameterCount;

    /**
     * 创建的新实例 {@link ParsedSql}
     *
     * @param originalSql 正在（或将要）分析的SQL语句
     */
    ParsedSql(String originalSql) {
        this.originalSql = originalSql;
    }

    /**
     * 返回正在分析的SQL语句。
     */
    String getOriginalSql() {
        return this.originalSql;
    }

    /**
     * 添加从此SQL语句中解析的命名参数。
     *
     * @param parameterName 参数的名称
     * @param startIndex    原始SQL字符串中的开始索引
     * @param endIndex      原始SQL字符串中的结束索引
     */
    void addNamedParameter(String parameterName, int startIndex, int endIndex) {
        this.parameterNames.add(parameterName);
        this.parameterIndexes.add(new int[]{startIndex, endIndex});
    }

    /**
     * 返回已解析SQL语句中的所有参数（绑定变量）。
     * 此处包含重复出现的相同参数名称。
     */
    List<String> getParameterNames() {
        return this.parameterNames;
    }

    /**
     * 返回指定参数的参数索引。
     *
     * @param parameterPosition 参数的位置（作为参数名称列表中的索引）
     * @return 开始索引和结束索引，组合成长度为2的整数数组
     */
    int[] getParameterIndexes(int parameterPosition) {
        return this.parameterIndexes.get(parameterPosition);
    }

    /**
     * 设置SQL语句中命名参数的计数。
     * 每个参数名称计数一次；此处不计算重复出现的次数。
     */
    void setNamedParameterCount(int namedParameterCount) {
        this.namedParameterCount = namedParameterCount;
    }

    /**
     * 返回SQL语句中命名参数的计数。
     * 每个参数名称计数一次；此处不计算重复出现的次数。
     */
    int getNamedParameterCount() {
        return this.namedParameterCount;
    }

    /**
     * 设置SQL语句中所有未命名参数的计数。
     */
    void setUnnamedParameterCount(int unnamedParameterCount) {
        this.unnamedParameterCount = unnamedParameterCount;
    }

    /**
     * 返回SQL语句中所有未命名参数的计数。
     */
    int getUnnamedParameterCount() {
        return this.unnamedParameterCount;
    }

    /**
     * 设置SQL语句中所有参数的总数。
     * 相同参数名称的重复出现在此处不计算在内。
     */
    void setTotalParameterCount(int totalParameterCount) {
        this.totalParameterCount = totalParameterCount;
    }

    /**
     * 返回SQL语句中所有参数的总数。
     * 相同参数名称的重复出现在此处不计算在内。
     */
    int getTotalParameterCount() {
        return this.totalParameterCount;
    }

    /**
     * 公开原始SQL字符串。
     */
    @Override
    public String toString() {
        return this.originalSql;
    }
}
