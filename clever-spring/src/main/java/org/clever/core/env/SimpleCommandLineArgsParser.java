package org.clever.core.env;

/**
 * 解析命令行参数的 {@code String[]} 以填充 {@link CommandLineArgs} 对象。
 * <p>
 * <h3>使用选项参数</h3>
 * <p>选项参数必须遵守确切的语法：
 * <pre class="code">--optName[=optValue]</pre>
 * <p>也就是说，选项必须以“{@code --}”为前缀，并且可以指定也可以不指定值。
 * 如果指定了值，则名称和值必须用等号 (“=”) 分隔，<em>不带空格</em>。
 * 该值可以选择为空字符串。
 * <h4>Valid examples of option arguments</h4>
 * <pre class="code">
 * --foo
 * --foo=
 * --foo=""
 * --foo=bar
 * --foo="bar then baz"
 * --foo=bar,baz,biz</pre>
 *
 * <h4>选项参数的无效示例</h4>
 * <pre class="code">
 * -foo
 * --foo bar
 * --foo = bar
 * --foo=bar --foo=baz --foo=biz</pre>
 *
 * <h3>使用非选项参数</h3>
 * <p>在命令行中指定的任何和所有不带“{@code --}”选项前缀的参数都将被视为“非选项参数”，
 * 并可通过 {@link CommandLineArgs#getNonOptionArgs()} 方法获取。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/25 10:39 <br/>
 */
class SimpleCommandLineArgsParser {
    /**
     * 根据上面描述的规则解析给定的 {@code String} 数组，返回一个完全填充的 {@link CommandLineArgs} 对象。
     *
     * @param args 命令行参数，通常来自 {@code main()} 方法
     */
    public CommandLineArgs parse(String... args) {
        CommandLineArgs commandLineArgs = new CommandLineArgs();
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String optionText = arg.substring(2);
                String optionName;
                String optionValue = null;
                int indexOfEqualsSign = optionText.indexOf('=');
                if (indexOfEqualsSign > -1) {
                    optionName = optionText.substring(0, indexOfEqualsSign);
                    optionValue = optionText.substring(indexOfEqualsSign + 1);
                } else {
                    optionName = optionText;
                }
                if (optionName.isEmpty()) {
                    throw new IllegalArgumentException("Invalid argument syntax: " + arg);
                }
                commandLineArgs.addOptionArg(optionName, optionValue);
            } else {
                commandLineArgs.addNonOptionArg(arg);
            }
        }
        return commandLineArgs;
    }
}

