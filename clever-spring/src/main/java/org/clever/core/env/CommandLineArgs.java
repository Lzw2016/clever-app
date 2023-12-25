package org.clever.core.env;

import java.util.*;

/**
 * 命令行参数的简单表示，分为“选项参数”和“非选项参数”。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/25 09:26 <br/>
 *
 * @see SimpleCommandLineArgsParser
 */
class CommandLineArgs {
    private final Map<String, List<String>> optionArgs = new HashMap<>();
    private final List<String> nonOptionArgs = new ArrayList<>();

    /**
     * 为给定选项名称添加选项参数，并将给定值添加到与此选项关联的值列表（其中可能有零个或多个）。
     * 给定值可以是 {@code null}，表示指定的选项没有关联值（例如“--foo”与“--foo=bar”）。
     */
    public void addOptionArg(String optionName, String optionValue) {
        if (!this.optionArgs.containsKey(optionName)) {
            this.optionArgs.put(optionName, new ArrayList<>());
        }
        if (optionValue != null) {
            this.optionArgs.get(optionName).add(optionValue);
        }
    }

    /**
     * 返回命令行上存在的所有选项参数的集合。
     */
    public Set<String> getOptionNames() {
        return Collections.unmodifiableSet(this.optionArgs.keySet());
    }

    /**
     * 返回具有给定名称的选项是否出现在命令行上。
     */
    public boolean containsOption(String optionName) {
        return this.optionArgs.containsKey(optionName);
    }

    /**
     * 返回与给定选项关联的值列表。
     * {@code null} 表示该选项不存在；空列表表示没有值与此选项关联。
     */
    public List<String> getOptionValues(String optionName) {
        return this.optionArgs.get(optionName);
    }

    /**
     * 将给定值添加到非选项参数列表中。
     */
    public void addNonOptionArg(String value) {
        this.nonOptionArgs.add(value);
    }

    /**
     * 返回在命令行上指定的非选项参数的列表。
     */
    public List<String> getNonOptionArgs() {
        return Collections.unmodifiableList(this.nonOptionArgs);
    }
}
