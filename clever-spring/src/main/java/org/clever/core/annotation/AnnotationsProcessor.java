package org.clever.core.annotation;

import java.lang.annotation.Annotation;

/**
 * 处理注解时的回调接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 14:56 <br/>
 */
@FunctionalInterface
interface AnnotationsProcessor<C, R> {
    /**
     * 将要处理聚合时调用。此方法可能会返回一个非空结果，从而中断进一步处理
     *
     * @param context        处理注解的上下文信息
     * @param aggregateIndex 即将处理的聚合索引
     * @return 如果不需要进一步处理，则返回非空结果
     */
    default R doWithAggregate(C context, int aggregateIndex) {
        return null;
    }

    /**
     * 当可以处理注解数组时调用。此方法可能会返回一个非空结果，从而中断进一步处理
     *
     * @param context        处理注解的上下文信息
     * @param aggregateIndex 提供的注解的聚合索引
     * @param source         注解的原始来源
     * @param annotations    要处理的注解(此数组可能包含空元素)
     * @return 如果不需要进一步处理，则返回非空结果
     */
    R doWithAnnotations(C context, int aggregateIndex, Object source, Annotation[] annotations);

    /**
     * 获取要返回的最终结果。默认情况下，此方法返回最后一次处理的结果
     *
     * @param result 最后一个提前退出结果，如果没有，则为空
     * @return 要返回给调用方的最终结果
     */
    default R finish(R result) {
        return result;
    }
}