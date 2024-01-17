package org.clever.groovy.rule

import java.util.stream.Collectors

class StrategyContext {
    /**
     * 是否已经 break(break之后)
     */
    boolean broke = false
    /**
     * 每次执行 {@link Rule#yieldResult(Object) yieldResult} 后的回调函数
     */
    RuleCallBack callBack
    /**
     * 命中的规则集合
     */
    final List<Rule> matchRules = []
    /**
     * 命中的规则组集合
     */
    final List<Group> matchGroups = []
    /**
     * 规则执行的异常信息
     */
    RuleException err
    /**
     * 返回结果
     */
    final List result = []

    List<Map<String, Object>> getRuleProps() {
        return matchRules.stream().map({ it.getProps() }).collect(Collectors.toList())
    }

    List<Map<String, Object>> getGroupProps() {
        return matchGroups.stream().map({ it.getGroupProps() }).collect(Collectors.toList())
    }

    Map<String, Object> getRuleProp() {
        if (matchRules.isEmpty()) {
            return [:]
        }
        return matchRules[0].getProps()
    }

    Map<String, Object> getGroupProp() {
        if (matchGroups.isEmpty()) {
            return [:]
        }
        return matchGroups[0].getGroupProps()
    }
}
