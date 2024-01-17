package org.clever.groovy.rule;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/06/26 20:09 <br/>
 */
@FunctionalInterface
public interface RuleCallBack {
    /**
     * 每次执行 {@link Rule#yieldResult(Object) yieldResult} 后的回调函数
     *
     * @param strategy 当前的 Strategy
     * @param rule     当前的 Rule
     * @param res      {@link Rule#yieldResult(Object) yieldResult} 函数的参数值
     */
    void callback(Strategy strategy, Rule rule, Object res);
}
