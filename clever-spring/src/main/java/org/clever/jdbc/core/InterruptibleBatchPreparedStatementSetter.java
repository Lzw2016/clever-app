package org.clever.jdbc.core;

/**
 * {@link BatchPreparedStatementSetter}接口的扩展，添加了批处理耗尽检查。
 *
 * <p>此界面允许您发出批次结束的信号，而不必预先确定确切的批次大小。
 * 批量大小仍然受到尊重，但它现在是批量的最大大小。
 *
 * <p>每次调用{@link #setValues}后，都会调用{@link #isBatchExhausted}方法，
 * 以确定是否添加了一些值，或者是否已确定该批是完整的，并且在上次调用{@code setValues}期间没有提供其他值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:45 <br/>
 *
 * @see JdbcTemplate#batchUpdate(String, BatchPreparedStatementSetter)
 */
public interface InterruptibleBatchPreparedStatementSetter extends BatchPreparedStatementSetter {
    /**
     * 返回批处理是否完成，即在上次{@code setValues}调用期间是否没有添加其他值。
     * <p>注意：如果此方法返回true，则将忽略上次{@code setValues}调用期间可能设置的任何参数！
     * 如果在{@code setValues}实现开始时检测到耗尽，请确保设置了相应的内部标志，使该方法基于标志返回true。
     *
     * @param i 我们在批中发布的语句的索引，从0开始
     * @return 批次是否已用完
     * @see #setValues
     */
    boolean isBatchExhausted(int i);
}
