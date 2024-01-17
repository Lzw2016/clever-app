package org.clever.groovy.rule

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j

@Slf4j
@Canonical
@ToString
class Rule {
    /**
     * 所属的 Strategy
     */
    private final Strategy strategy
    /**
     * 所属的 Group
     */
    private final Group group
    /**
     * 所属的 Group
     */
    private final Rule parentRule

    /**
     * 命中规则后是否中断执行
     */
    boolean ruleBreak = false

    /**
     * 是否强制中断执行（无论是否匹配）
     */
    boolean ruleBreakAny = false

    /**
     *
     */
    Closure<Boolean> whenClosure = null

    /**
     *
     */
    Closure<Void> thenClosure = null

    /**
     * 规则ID
     */
    Long ruleId
    /**
     * 规则编码
     */
    String ruleCode
    /**
     * 规则名称
     */
    String ruleName
    /**
     * 规则属性
     */
    Map<String, Object> props = [:]
    /**
     * 规则固定的 when 匹配上下文变量值 {@code Map<上下文变量名, 上下文变量值>}
     */
    Map<String, Object> whenProp = [:]

    /**
     * 规则集合
     */
    protected List<Rule> rules = []

    protected Rule(Rule parentRule) {
        this.strategy = parentRule.strategy
        this.group = parentRule.group
        this.parentRule = parentRule
    }

    protected Rule(Group group) {
        this.strategy = group.strategy
        this.group = group
        this.parentRule = null
    }

    protected Rule(Strategy strategy) {
        this.strategy = strategy
        this.group = null
        this.parentRule = null
    }

    /**
     * 命中规则条件语句块
     * @return true 表示命中规则
     */
    def when(Closure<Boolean> closure) {
        if (this.whenClosure != null) {
            log.warn("Rule[${this.getTitle()}] 的 when 语句块已经定义过了")
        }
        this.whenClosure = closure
        return this
    }

    /**
     * 定义规则符合适用条件之后的执行动作
     */
    def then(Closure<Void> closure) {
        if (this.thenClosure != null) {
            log.warn("Rule[${this.getTitle()}] 的 then 语句块已经定义过了")
        }
        this.thenClosure = closure
        return this
    }

    /**
     * 增加一个 Strategy 的返回值
     */
    void yieldResult(Object res) {
        StrategyContext strategyContext = strategy.getContext()
        strategyContext.result.add(res)
        if (strategyContext.callBack != null) {
            strategyContext.callBack.callback(strategy, this, res)
        }
    }

    /**
     * 终止整个 Strategy 的执行
     */
    void yieldBreak() {
        StrategyContext strategyContext = strategy.getContext()
        strategyContext.broke = true
    }

    /**
     * 终止整个 Strategy 的执行，并且指定返回值
     */
    void returnResult(Object res) {
        StrategyContext strategyContext = strategy.getContext()
        strategyContext.result.add(res)
        strategyContext.broke = true
    }

    /**
     * 抛出异常信息，中断整个 Strategy 的执行
     */
    void throwException(String message) {
        StrategyContext strategyContext = strategy.getContext()
        strategyContext.err = new RuleException(message)
        throw strategyContext.err
    }

    /**
     * 规则定义
     */
    @SuppressWarnings('DuplicatedCode')
    def rule(@DelegatesTo(Rule.class) Closure closure) {
        return this.rule(null, closure)
    }

    /**
     * 规则定义
     */
    @SuppressWarnings('DuplicatedCode')
    def rule(String ruleName, @DelegatesTo(Rule.class) Closure closure) {
        Rule rule = new Rule(this)
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = rule
        closure()
        if (!StringUtils.isBlank(ruleName)) {
            rule.ruleName = ruleName
        }
        this.rules.add(rule)

        return this
    }

    /**
     * 获取规名称
     */
    public getTitle() {
        StringBuilder sbLogStr = new StringBuilder()
        if (this.ruleId != null && this.ruleId != 0L) {
            sbLogStr.append("ruleId=").append(this.ruleId).append("; ")
        }
        if (this.ruleCode != null && this.ruleCode != '') {
            sbLogStr.append("ruleCode=").append(this.ruleCode).append("; ")
        }
        if (this.ruleName != null && this.ruleName != '') {
            sbLogStr.append("ruleName=").append(this.ruleName).append("; ")
        }
        return sbLogStr.toString()
    }
}
