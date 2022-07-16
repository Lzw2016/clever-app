package org.clever.util;

import java.util.Comparator;
import java.util.Map;

/**
 * 用于基于字符串的路径匹配的策略接口<br/>
 * 默认实现是{@link AntPathMatcher}，支持Ant风格的语法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 22:21 <br/>
 *
 * @see AntPathMatcher
 */
public interface PathMatcher {
    /**
     * 给定的路径是否表示可由该接口的实现匹配的模式？
     * 如果返回值为false，则不必使用{@link #match}方法，
     * 因为对静态路径字符串进行直接相等比较将得到相同的结果
     *
     * @param path 要检查的路径
     */
    boolean isPattern(String path);

    /**
     * 根据此PathMatcher的匹配策略，将给定{@code path}与给定{@code pattern}匹配
     *
     * @param pattern 要匹配的模式
     * @param path    测试路径
     * @return 如果提供的路径匹配，则为true；如果不匹配，则为false
     */
    boolean match(String pattern, String path);

    /**
     * 根据此PathMatcher的匹配策略，将给定{@code path}与给定{@code pattern}的相应部分进行匹配。
     * 确定模式是否至少与给定的基本路径匹配，假设完整路径也可能匹配
     *
     * @param pattern 要匹配的模式
     * @param path    测试路径
     * @return 如果提供的路径匹配，则为true；如果不匹配，则为false
     */
    boolean matchStart(String pattern, String path);

    /**
     * 提取{@code path}中匹配{@code pattern}的部分，如：
     * <pre>{@code
     * pattern  = "myroot/*.html"
     * path     = "myroot/myfile.html"
     * return   ->"myfile.html"
     *
     * }</pre>
     *
     * @param pattern 路径模式
     * @param path    完整路径
     * @return 给定路径的模式映射部分（从不为null）
     */
    String extractPathWithinPattern(String pattern, String path);

    /**
     * 给定一个模式和完整路径，提取URI模板变量。URI模板变量通过花括号('{'和'}')表示，如：
     * <pre>{@code
     * pattern  = "/hotels/{hotel}"
     * path     = "/hotels/1"
     * return   ->Map{"hotel" -> "1"}
     * }</pre>
     *
     * @param pattern 路径模式，可能包含URI模板
     * @param path    从中提取模板变量的完整路径
     * @return 一个映射，包含作为键的变量名；变量值作为值
     */
    Map<String, String> extractUriTemplateVariables(String pattern, String path);

    /**
     * 给定完整路径，返回一个比较器{@link Comparator}，该比较器适合按照路径的明确性顺序对模式进行排序。
     * 使用的完整算法取决于底层实现，但通常情况下，返回的比较器将对列表进行排序{@linkplain java.util.List#sort(java.util.Comparator) sort}，
     * 以便在泛型模式之前出现更多特定的模式
     *
     * @param path 用于比较的完整路径
     * @return 一种比较器，能够按明确性的顺序对模式进行排序
     */
    Comparator<String> getPatternComparator(String path);

    /**
     * 将两个模式组合为返回的新模式。
     * 用于组合这两种模式的完整算法取决于底层实现
     *
     * @param pattern1 第一种模式
     * @param pattern2 第二种模式
     * @return 两种模式的结合
     * @throws IllegalArgumentException 当两种模式无法组合时
     */
    String combine(String pattern1, String pattern2);
}
