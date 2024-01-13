package org.clever.data.jdbc.mapper;

import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.data.jdbc.entity.EntityData;
import org.clever.data.jdbc.mybatis.annotations.Mapper;
import org.clever.data.jdbc.mybatis.annotations.Param;
import org.clever.data.jdbc.support.BatchData;
import org.clever.data.jdbc.support.DbColumnMetaData;
import org.clever.data.jdbc.support.InsertResult;
import org.clever.data.jdbc.support.RowData;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/10 14:22 <br/>
 */
public interface Mapper01 {
    List<Map<String, Object>> t01(Long id);

    @Mapper(sqlId = "t01")
    Map<String, Object> t02(Long id);

    @Mapper(sqlId = "t01")
    List<EntityData> t03(Long id);

    @Mapper(sqlId = "t01")
    EntityData t04(Long id);

    @Mapper(sqlId = "t01")
    LinkedList<EntityData> t05(Long id);

    @Mapper(sqlId = "t01")
    HashMap<String, Object> t06(Long id);

    @Mapper(sqlId = "t01")
    List<DbColumnMetaData> t07(Long id);

    Long t08(Long id);

    Date t09(Long id);

    String t10(String str);

    @Mapper(sqlId = "t08", first = false)
    BigDecimal t11(Long id);

    @Mapper(sqlId = "t01")
    EntityData[] t12(Long id);




    List<EntityData> test(@Param("123") EntityData param, QueryBySort sort, Function<BatchData, Boolean> call_1, String a);

    EntityData test1(QueryByPage page, Function<BatchData, Boolean> call_1, Consumer<RowData> call_2);

    EntityData[] qMany2();

    Collection<Map<String, Object>> qMany3();

    Map<String, Object>[] qMany4();

    Collection<EntityData> qSort(QueryBySort sort);

    Collection<EntityData> qPage(QueryByPage pagination);

    void qCursor(Function<BatchData, Boolean> callback);

    void qCursor2(Consumer<BatchData> consumer);

    void qCursor3(Function<RowData, Boolean> callback);

    void qCursor3(Consumer<RowData> consumer);

    int update(String sqlId);

    int[] batchUpdate();

    InsertResult insert();

    void call();

    Map<String, Object> callGet();

    EntityData callGet2();

    Object callGet3();
}
