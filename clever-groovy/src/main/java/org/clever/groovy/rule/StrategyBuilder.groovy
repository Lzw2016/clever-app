package org.clever.groovy.rule

public class StrategyBuilder {
    public String strategyName;
    public String strategyCode;

    public List<GroupBuilder> groups = Lists.newArrayList();

    public List<RuleBuilder> rules = Lists.newArrayList();

    public static StrategyBuilder create() {
        return new StrategyBuilder();
    }

    GroupBuilder addGroup() {
        GroupBuilder group = new GroupBuilder();
        this.groups.add(group);
        return group;
    }

    RuleBuilder addRule() {
        RuleBuilder rule = new RuleBuilder();
        this.rules.add(rule);
        return rule;
    }

    /**
     * 写成 Strategy.define 代码
     */
    String toCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
Strategy.define {
    ${safeCode('strategyName', this.strategyName)}
    ${safeCode('strategyCode', this.strategyCode)}
""");
        for (GroupBuilder group : groups) {
            sb.append("\n")
            group.toCode(sb)
        }

        for (RuleBuilder rule : rules) {
            sb.append("\n")
            rule.toCode(sb)
        }

        sb.append("""
}""");

        return sb.toString();
    }

    static class RuleBuilder {
        public Long ruleId;
        public String ruleName;
        public String ruleCode;
        public String when;
        public String then;
        public Map<String, Object> props = null
        public Map<String, Object> whenProp = null

        public void toCode(StringBuilder sb, boolean indent4 = false) {
            // 缩进
            String b = indent4 ? "    " : "";
            String value = """
    rule {
        ${safeCode('ruleId', this.ruleId)}
        ${safeCode('ruleName', this.ruleName)}
        ${safeCode('ruleCode', this.ruleCode)}
        ${safeMap('props', this.props)}
        ${safeMap('whenProp', this.whenProp)}
        ${safeWhen(this.when)}
        ${safeThen(this.then)}
    }"""
            value.split("\n").each {
                if (it != null && it.trim().length() > 0) {
                    sb.append(b).append(it).append("\n")
                }
            }
        }
    }

    static class GroupBuilder {
        public Long groupId;
        public String groupName;
        public String groupCode;
        public String when;
        public Map<String, Object> groupProps = null
        public Map<String, Object> whenProp = null

        public List<RuleBuilder> rules = Lists.newArrayList();

        RuleBuilder addRule() {
            RuleBuilder rule = new RuleBuilder();
            this.rules.add(rule);
            return rule;
        }

        public void toCode(StringBuilder sb) {
            String value = """
    group {
        ${safeCode('groupId', this.groupId)}
        ${safeCode('groupName', this.groupName)}
        ${safeCode('groupCode', this.groupCode)}
        ${safeMap('groupProps', this.groupProps)}
        ${safeMap('whenProp', this.whenProp)}
        ${safeWhen(this.when)}
"""
            value.split("\n").each {
                if (it != null && it.trim().length() > 0) {
                    sb.append(it).append("\n")
                }
            }

            for (RuleBuilder rule : rules) {
                sb.append("\n")
                rule.toCode(sb, true)
            }
            sb.append("    }")
        }
    }
}
