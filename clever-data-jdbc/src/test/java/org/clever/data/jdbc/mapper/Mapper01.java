package org.clever.data.jdbc.mapper;

import org.clever.core.model.request.QueryByPage;
import org.clever.core.model.request.QueryBySort;
import org.clever.core.model.request.page.IPage;
import org.clever.core.model.request.page.Page;
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

//    不支持这种
//    @Mapper(sqlId = "t01")
//    Map<String, Object>[] t13(Long id);

    @Mapper(sqlId = "t01")
    List<EntityData> t14(Long id, QueryBySort sort);

    @Mapper(sqlId = "t01")
    List<EntityData> t15(Long id, QueryByPage page);

    @Mapper(sqlId = "t01")
    IPage<EntityData> t16(Long id, QueryByPage page);

    @Mapper(sqlId = "t01")
    Page<Map<String, Object>> t17(Long id, QueryByPage page);

    @Mapper(sqlId = "t01")
    void t18(Long id, Function<BatchData, Boolean> callback);

    @Mapper(sqlId = "t01")
    void t19(Long id, Function<RowData, Boolean> callback);

    @Mapper(sqlId = "t01")
    void t20(Long id, Consumer<BatchData> callback);

    @Mapper(sqlId = "t01")
    void t21(Long id, Consumer<RowData> callback);

    @Mapper(ops = Mapper.Ops.Update)
    int t22(Long id, @Param("lockCount") Long newValue);

    @Mapper(ops = Mapper.Ops.Update)
    Long t23(Long id, Long newValue);

    InsertResult t24(String str);

    @Mapper(sqlId = "t24", ops = Mapper.Ops.Update)
    long t25(String str);

    @Mapper(sqlId = "t24")
    int[] t26(List<Map<String, Object>> values);

    @Mapper(sqlId = "t24")
    long[] t27(List<Map<String, Object>> values);

    @Mapper(sqlId = "t24")
    Long[] t28(List<Map<String, Object>> values);

    int[] t29(List<EntityData> values);

    @Mapper(ops = Mapper.Ops.Call)
    void t30(String name);

    @Mapper(ops = Mapper.Ops.Call)
    Map<String, Object> t31(String name, Long size);

    Map<String, Object> callGet();

    EntityData callGet2();

    Object callGet3();
}
