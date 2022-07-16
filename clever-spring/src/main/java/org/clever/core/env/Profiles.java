package org.clever.core.env;

import java.util.function.Predicate;

/**
 * {@link Environment}可能接受{@linkplain Environment#acceptsProfiles(Profiles)}的Profile谓词。
 * 可以直接实现，或者更常见的是，使用{@link #of(String...) of(...)}工厂方法创建
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 16:15 <br/>
 */
@FunctionalInterface
public interface Profiles {
    /**
     * 测试此{@code Profiles}实例是否与给定的活动{@code Profiles}谓词匹配。
     *
     * @param activeProfiles 测试给定profile当前是否处于活动状态的谓词
     */
    boolean matches(Predicate<String> activeProfiles);

    /**
     * 新建 {@link Profiles} 实例，该实例根据给定的profile字符串检查匹配项。<br/>
     * 如果给定的任何一个profile字符串匹配，则返回的实例将匹配。<br/>
     * profile字符串可以包含简单的profile名称(例如“production”)或profile表达式。
     * profile表达式允许表达更复杂的profile逻辑，例如“production & cloud”。<br/>
     * profile表达式中支持以下运算符 <br/>
     * <ul>
     * <li>{@code !} - NOT逻辑</li>
     * <li>{@code &} - AND逻辑</li>
     * <li>{@code |} - OR逻辑</li>
     * </ul>
     * 请注意，如果不使用括号，则不能混合使用 & 和 | 运算符。
     * 例如，{@code "a & b | c"}不是有效的表达式；必须表示为{@code "(a & b) | c"}或{@code "a & (b | c)"}
     *
     * @param profiles the <em>profile strings</em> to include
     * @return a new {@link Profiles} instance
     */
    static Profiles of(String... profiles) {
        return ProfilesParser.parse(profiles);
    }
}
