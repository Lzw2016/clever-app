package org.clever.groovy.rule

import groovy.transform.Canonical
import groovy.transform.ToString
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils

@Slf4j
@Canonical
@ToString
class Strategy {
    /**
     * Strategy 运行的返回值
     */
    protected static final ThreadLocal<StrategyContext> STRATEGY_CONTEXT = new ThreadLocal<>()
    /**
     * 获取当前调用线程的 StrategyContext
     */
    protected static StrategyContext getContext() {
        synchronized (STRATEGY_CONTEXT) {
            StrategyContext strategyContext = STRATEGY_CONTEXT.get()
            if (strategyContext == null) {
                strategyContext = new StrategyContext()
                STRATEGY_CONTEXT.set(strategyContext)
            }
            return strategyContext
        }
    }

    protected static void clearContext() {
        STRATEGY_CONTEXT.remove()
    }

    /**
     * 策略定义
     */
    static Strategy define(@DelegatesTo(Strategy.class) Closure closure) {
        Strategy strategy = new Strategy()
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = strategy
        closure()
        return strategy
    }

    /**
     * 策略内部编码
     */
    String strategyCode
    /**
     * 策略名称
     */
    String strategyName
    /**
     * 规则集合
     */
    protected final List<Rule> rules = []
    /**
     * 规则组集合
     */
    protected final List<Group> groups = []

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
        rule.ruleName = ruleName
        this.rules.add(rule)
        return this
    }

    /**
     * 规则分组定义
     */
    def group(@DelegatesTo(Group.class) Closure closure) {
        Group group = new Group(this)
        closure.resolveStrategy = Closure.DELEGATE_ONLY
        closure.delegate = group
        closure()
        this.groups.add(group)
        return this
    }

    StrategyContext runStrategy(RuleCallBack callBack) {
        // 运行前的检查
        if (!rules.isEmpty() && !groups.isEmpty()) {
            throw new RuleException("规则定义错误，rules 与 groups 不能同时存在")
        }
        // 开始运行
        StrategyContext strategyContext = getContext()
        strategyContext.broke = false
        strategyContext.callBack = callBack
        try {
            if (!rules.isEmpty()) {
                runRules(rules, strategyContext)
            }
            if (!groups.isEmpty()) {
                runGroups(groups, strategyContext)
            }
        } finally {
            clearContext()
        }
        return strategyContext
    }

    StrategyContext runStrategy() {
        return runStrategy(null)
    }

    /**
     * 运行当前 Strategy
     * @param callBack 运行时的回调函数
     */
    List runListCallback(RuleCallBack callBack) {
        StrategyContext strategyContext = runStrategy(callBack)
        // 处理返回值
        return strategyContext.getResult()
    }

    /**
     * 运行当前 Strategy
     * @param callBack 运行时的回调函数
     */
    def runCallback(RuleCallBack callBack) {
        List result = runListCallback(callBack)
        return result.size() == 1 ? result[0] : result
    }

    /**
     * 运行当前 Strategy
     */
    List runList() {
        return runListCallback(null)
    }

    /**
     * 运行当前 Strategy
     */
    def run() {
        return runCallback(null)
    }

    private static void runRules(List<Rule> rules, StrategyContext strategyContext) {
        if (strategyContext.broke) {
            return
        }
        for (final def rule in rules) {
            if (strategyContext.broke) {
                return
            }
            // log.debug("进入规则: " + rule.getTitle())
            boolean match = matchWhenProp(rule.whenProp)
            if (!match) {
                continue
            }
            match = false
            Closure<Boolean> whenClosure = rule.getWhenClosure()
            Closure<Void> thenClosure = rule.getThenClosure()
            // log.info('规则判断 RunWhen: ' + rule.getTitle())
            if (whenClosure == null || whenClosure() == true) {
                log.info('命中规则 then - ' + rule.getTitle())
                match = true
                if (thenClosure != null) {
                    thenClosure()
                }
                if (rule.rules != null && rule.rules.size() > 0) {
                    // 执行子规则
                    runRules(rule.rules, strategyContext)
                }
                if (rule.ruleBreak) {
                    // 命中规则后中断执行
                    strategyContext.broke = true
                    log.info('中断执行 ruleBreak - ' + rule.getTitle())
                }
            }
            if (rule.ruleBreakAny) {
                // 强制中断执行（无论是否匹配）
                strategyContext.broke = true
                log.info('中断执行 ruleBreakAny - ' + rule.getTitle())
            }
            if (match) {
                strategyContext.matchRules.add(rule)
            }
        }
    }

    private static void runGroups(List<Group> groups, StrategyContext strategyContext) {
        for (final def group in groups) {
            log.debug("开始匹配Group: groupId={} | groupCode={} | groupName={}", group.groupId, group.groupCode, group.groupName)
            boolean match = matchWhenProp(group.whenProp)
            if (!match) {
                continue
            }
            if (group.whenClosure == null || group.whenClosure() == true) {
                log.debug("命中规则组: groupId={} | groupCode={} | groupName={}", group.groupId, group.groupCode, group.groupName)
                strategyContext.matchGroups.add(group)
                runRules(group.rules, strategyContext)
                break
            }
        }
    }

    private static boolean matchWhenProp(Map<String, Object> whenProp) {
        boolean match = true
        if (whenProp != null) {
            for (final def entry in whenProp.entrySet()) {
                String varName = entry.getKey()
                Object varValue = entry.getValue()
                if (StringUtils.isBlank(varName)) {
                    continue
                }
                // match = Objects.equals(BizContext.getValue(varName), varValue)
                // if (!match) {
                //     break
                // }
            }
        }
        return match
    }
}
