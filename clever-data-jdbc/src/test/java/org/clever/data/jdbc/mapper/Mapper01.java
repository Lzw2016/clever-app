package org.clever.data.jdbc.mapper;

import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.data.jdbc.entity.EntityData;
import org.clever.data.jdbc.mybatis.annotations.Param;
import org.clever.data.jdbc.support.BatchData;
import org.clever.data.jdbc.support.DbColumnMetaData;
import org.clever.data.jdbc.support.InsertResult;
import org.clever.data.jdbc.support.RowData;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/10 14:22 <br/>
 */
public interface Mapper01 {
    List<EntityData> test(@Param("123") EntityData param, QueryBySort sort, Function<BatchData, Boolean> call_1, String a);

    EntityData test1(QueryByPage page, Function<BatchData, Boolean> call_1, Consumer<RowData> call_2);

    List<DbColumnMetaData> queryMetaData();

    Map<String, Object> qOne();

    EntityData qOne2();

    String qOne3();

    Long qOne4();

    Integer qOne5();

    Double qOne6();

    Float qOne7();

    BigDecimal qOne8();

    Boolean qOne9();

    Date qOne10();

    Timestamp qOne11();

    Map<String, Object> qFirst();

    EntityData qFirst2();

    Collection<EntityData> qMany();

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
