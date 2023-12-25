package org.clever.core.env;

import org.clever.util.StringUtils;

import java.util.List;

/**
 * 由简单字符串数组支持的 {@link CommandLinePropertySource} 实现。
 * <p>
 * <h3>目的</h3>
 * <p>此 {@code CommandLinePropertySource} 实现旨在提供最简单的方法来解析命令行参数。
 * 与所有 {@code CommandLinePropertySource} 实现一样，
 * 命令行参数分为两个不同的组：<em>选项参数</em>和<em>非选项参数</em>，
 * 如下所述<em>（一些从 {@link SimpleCommandLineArgsParser} 的 Javadoc 复制的部分）</em>：
 * <p>
 * <h3>使用选项参数</h3>
 * <p>选项参数必须遵守确切的语法：
 * <pre class="code">--optName[=optValue]</pre>
 *
 * <p>也就是说，选项必须以“{@code --}”为前缀，并且可以指定也可以不指定值。
 * 如果指定了值，则名称和值必须用等号 (“=”) 分隔，<em>不带空格</em>。该值可以选择为空字符串。
 * <p>
 * <h4>选项参数的有效示例</h4>
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
 * <h3>典型用法</h3>
 * <pre>{@code
 *   public static void main(String[] args) {
 *       PropertySource<?> ps = new SimpleCommandLinePropertySource(args);
 *       // ...
 *   }
 * }</pre>
 * <p>请参阅 {@link CommandLinePropertySource} 以获取完整的一般用法示例。
 * <p>
 * <h3>超越基础知识</h3>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/12/25 09:23 <br/>
 *
 * @see CommandLinePropertySource
 */
public class SimpleCommandLinePropertySource extends CommandLinePropertySource<CommandLineArgs> {
    /**
     * 创建一个具有默认名称并由给定的命令行参数 {@code String[]} 支持的新 {@code SimpleCommandLinePropertySource}。
     *
     * @see CommandLinePropertySource#COMMAND_LINE_PROPERTY_SOURCE_NAME
     * @see CommandLinePropertySource#CommandLinePropertySource(Object)
     */
    public SimpleCommandLinePropertySource(String... args) {
        super(new SimpleCommandLineArgsParser().parse(args));
    }

    /**
     * 创建一个具有给定名称并由给定命令行参数 {@code String[]} 支持的新 {@code SimpleCommandLinePropertySource}。
     */
    public SimpleCommandLinePropertySource(String name, String[] args) {
        super(name, new SimpleCommandLineArgsParser().parse(args));
    }

    /**
     * 获取选项参数的属性名称
     */
    @Override
    public String[] getPropertyNames() {
        return StringUtils.toStringArray(this.source.getOptionNames());
    }

    @Override
    protected boolean containsOption(String name) {
        return this.source.containsOption(name);
    }

    @Override
    protected List<String> getOptionValues(String name) {
        return this.source.getOptionValues(name);
    }

    @Override
    protected List<String> getNonOptionArgs() {
        return this.source.getNonOptionArgs();
    }
}
