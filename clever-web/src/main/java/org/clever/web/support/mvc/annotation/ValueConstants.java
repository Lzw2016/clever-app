package org.clever.web.support.mvc.annotation;

/**
 * 绑定注释之间共享的公共值常量。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/04 22:36 <br/>
 */
public interface ValueConstants {
    /**
     * 常量定义一个无默认值的值-作为我们不能在注释属性中使用的{@code null}的替换。
     * <p>这是16个unicode字符的人工排列，其唯一目的是永远不匹配用户声明的值。
     *
     * @see RequestParam#defaultValue()
     * @see RequestHeader#defaultValue()
     * @see CookieValue#defaultValue()
     */
    String DEFAULT_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";
}
