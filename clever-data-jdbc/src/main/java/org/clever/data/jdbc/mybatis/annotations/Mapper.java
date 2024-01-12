package org.clever.data.jdbc.mybatis.annotations;

import org.clever.core.RenameStrategy;

import java.lang.annotation.*;

/**
 * MyBatis Mapper 配置
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/10 14:24 <br/>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Mapper {
    /**
     * Mapper xml文件路径
     */
    String mapperPath() default "";

    /**
     * 对应 Mapper xml文件的 sql id
     */
    String sqlId() default "";

    /**
     * 执行的操作类型
     */
    Ops ops() default Ops.Query;

    /**
     * 返回的表字段重命名策略
     */
    RenameStrategy rename() default RenameStrategy.ToCamel;

    /**
     * 当查询一条数据时(Mapper返回值不是集合)，是否使用 queryFirst。 <br/>
     * 值为 false 表示需要使用 queryOne
     */
    boolean first() default true;

    /**
     * 当Mapper返回值是数值类型时(int/Integer/long/Long)，是否使用 SQL Count 查询获取查询结果数据量
     */
    boolean count() default false;

    /**
     * 设置游标读取查询数据时的批次大小(小于等于0表示不设置)
     */
    int batchSize() default -1;

    enum Ops {
        /**
         * 查询操作(select)
         */
        Query,
        /**
         * 更新操作(insert、update、delete)
         */
        Update,
//        /**
//         * 执行存储过程或函数???
//         */
//        Call,
    }
}
