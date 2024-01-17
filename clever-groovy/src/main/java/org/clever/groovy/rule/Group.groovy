package org.clever.groovy.rule

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j

@Slf4j
@Canonical
@ToString
class Group {
    /**
     * 所属的 Strategy
     */
    protected final Strategy strategy
    /**
     * 规则集合
     */
    protected List<Rule> rules = []
    /**
     * 是否命中当前规则组的语句块
     */
    protected Closure<Boolean> whenClosure = { true }
    /**
     * 规则组ID
     */
    Long groupId
    /**
     * 规则组编码
     */
    String groupCode
    /**
     * 规则组名称
     */
    String groupName
    /**
     * 规则组属性
     */
    Map<String, Object> groupProps = [:]
    /**
     * 规则组固定的 when 匹配上下文变量值 {@code Map<上下文变量名, 上下文变量值>}
     */
    Map<String, Object> whenProp = [:]

    Group(Strategy strategy) {
        this.strategy = strategy
    }

    /**
     * 是否命中当前规则组的语句块
     */
    def when(Closure<Boolean> closure) {
        this.whenClosure = closure
        return this
    }

    /**
     * 规则定义
     */
    @SuppressWarnings('DuplicatedCode')
    def rule(@DelegatesTo(Rule.class) Closure closure) {
        Rule rule = new Rule(this)
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = rule
        closure()
        this.rules.add(rule)
        return this
    }
}
