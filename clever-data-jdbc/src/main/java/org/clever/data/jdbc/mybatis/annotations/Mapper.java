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
    Ops ops() default Ops.Auto;

    /**
     * 返回的表字段重命名策略。<br/>
     * 与 {@code Jdbc.DEFAULT_RESULT_RENAME} 保持一致
     */
    RenameStrategy rename() default RenameStrategy.None;

    /**
     * 当查询一条数据时(Mapper返回值不是集合)，是否使用 queryFirst。 <br/>
     * 值为 false 表示需要使用 queryOne
     */
    boolean first() default true;

    /**
     * 当Mapper返回值是整数类型时(int/Integer/long/Long)，是否使用 SQL Count 查询获取查询结果数据量
     */
    boolean count() default false;

    /**
     * 设置游标读取查询数据时的批次大小(小于等于0表示不设置)。 <br/>
     * 与 {@code BatchDataReaderCallback.DEFAULT_BATCH_SIZE} 保持一致
     */
    int batchSize() default 200;

    enum Ops {
        /**
         * 自动判断 Query、Update、Call
         */
        Auto,
        /**
         * 查询操作(select)
         */
        Query,
        /**
         * 更新操作(insert、update、delete)
         */
        Update,
        /**
         * 执行存储过程或函数
         */
        Call,
    }
}
