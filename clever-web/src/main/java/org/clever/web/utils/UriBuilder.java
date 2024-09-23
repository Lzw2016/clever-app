//package org.clever.web.utils;
//
//import org.clever.util.MultiValueMap;
//
//import java.net.URI;
//import java.util.Collection;
//import java.util.Map;
//import java.util.Optional;
//
///**
// * 使用变量准备和扩展 URI 模板的构建器样式方法。
// *
// * <p>实际上是 {@link UriComponentsBuilder} 的泛化，
// * 但具有直接扩展到 {@link URI} 而不是 {@link UriComponents} 的快捷方式，
// * 并且还留下了常见的问题，例如编码首选项、基本 URI 和其他实现问题。
// *
// * <p>通常通过 UriBuilderFactory 获得，它作为中央组件配置一次并用于创建许多 URL。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/12/24 16:07 <br/>
// *
// * @see UriComponentsBuilder
// */
//public interface UriBuilder {
//    /**
//     * 设置可能包含 URI 模板变量的 URI 方案，也可能是 {@code null} 以清除此构建器的方案。
//     *
//     * @param scheme URI scheme
//     */
//    UriBuilder scheme(String scheme);
//
//    /**
//     * 设置 URI 用户信息，其中可能包含 URI 模板变量，也可以是 {@code null} 以清除此构建器的用户信息。
//     *
//     * @param userInfo URI user info
//     */
//    UriBuilder userInfo(String userInfo);
//
//    /**
//     * 设置可能包含 URI 模板变量的 URI 主机，也可能是 {@code null} 以清除此构建器的主机。
//     *
//     * @param host URI host
//     */
//    UriBuilder host(String host);
//
//    /**
//     * 设置 URI 端口。传递 {@code -1} 将清除此构建器的端口。
//     *
//     * @param port URI port
//     */
//    UriBuilder port(int port);
//
//    /**
//     * 设置 URI 端口。仅当需要使用 URI 变量对端口进行参数化时才使用此方法。否则使用 {@link #port(int)}。传递 {@code null} 将清除此构建器的端口。
//     *
//     * @param port URI port
//     */
//    UriBuilder port(String port);
//
//    /**
//     * 附加到此构建器的路径。
//     * <p>给定值按原样附加到先前的 {@code #path(String) path} 值，而不插入任何额外的斜线。例如：
//     * <pre>{@code
//     * builder.path("/first-").path("value/").path("/{id}").build("123")
//     *
//     * // Results is "/first-value/123"
//     * }</pre>
//     * <p>相比之下，{@link #pathSegment(String...) pathSegment} 确实在各个路径段之间插入斜线。例如：
//     * <pre>{@code
//     * builder.pathSegment("first-value", "second-value").path("/")
//     * // Results is "/first-value/second-value/"
//     * }</pre>
//     * <p>生成的完整路径被规范化以消除重复的斜线。
//     * <p><strong>注意：</strong> 在 {@code path(String) path} 中插入包含斜杠的 URI 变量值时，
//     * 是否对这些值进行编码取决于配置的编码模式。
//     *
//     * @param path URI path
//     */
//    UriBuilder path(String path);
//
//    /**
//     * 覆盖当前路径
//     *
//     * @param path URI 路径，或空路径的 {@code null}
//     */
//    UriBuilder replacePath(String path);
//
//    /**
//     * 使用路径段附加到路径。例如：
//     * <pre>{@code
//     * builder.pathSegment("first-value", "second-value", "{id}").build("123")
//     * // Results is "/first-value/second-value/123"
//     * }</pre>
//     * <p>如果路径段中存在斜杠，则对它们进行编码：
//     * <pre>{@code
//     * builder.pathSegment("ba/z", "{id}").build("a/b")
//     * // Results is "/ba%2Fz/a%2Fb"
//     * }</pre>
//     * 要插入尾部斜杠，请使用 {@link #path} 构建器方法：
//     * <pre>{@code
//     * builder.pathSegment("first-value", "second-value").path("/")
//     * // Results is "/first-value/second-value/"
//     * }</pre>
//     * <p>空路径段将被忽略，因此在生成的完整路径中不会出现重复的斜线。
//     *
//     * @param pathSegments URI path segments
//     */
//    UriBuilder pathSegment(String... pathSegments) throws IllegalArgumentException;
//
//    /**
//     * 将给定的查询字符串解析为查询参数，其中参数用 {@code '&'} 分隔，如果有的话，用 {@code '='} 分隔。
//     * 查询可能包含 URI 模板变量。
//     * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，
//     * 以获取有关处理和编码各个查询参数的更多说明。
//     *
//     * @param query query string
//     */
//    UriBuilder query(String query);
//
//    /**
//     * 清除现有的查询参数，然后委托给 {@link #query(String)}.
//     * <p><strong>注：</strong> 请查看{@link #queryParam(String, Object...)}的Javadoc，
//     * 了解有关单个查询参数的处理和编码的更多说明。
//     *
//     * @param query 查询字符串；{@code null}值将删除所有查询参数。
//     */
//    UriBuilder replaceQuery(String query);
//
//    /**
//     * 追加给定的查询参数。参数名称和值都可能包含URI模板变量，稍后将从值展开。
//     * 如果没有给定值，则生成的URI将仅包含查询参数名称，例如{@code "?foo"}而不是{@code "?foo=bar"}。
//     * <p><strong>注:</strong> 如果应用了编码，将只对查询参数名称或值（如“=”或“&”）中非法的字符进行编码。
//     * 根据<a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>中的语法规则，所有其他合法的都没有编码。
//     * 这包括有时需要编码的“+”，以避免将其解释为编码空间。可以通过使用URI模板变量以及对变量值的更严格编码来应用更严格的编码。
//     *
//     * @param name   查询参数名称
//     * @param values 查询参数值
//     * @see #queryParam(String, Collection)
//     */
//    UriBuilder queryParam(String name, Object... values);
//
//    /**
//     * 带有集合的{@link #queryParam(String, Object...)}的变量。
//     * <p><strong>注: </strong> 请查看{@link #queryParam(String, Object...)}的Javadoc，了解有关单个查询参数的处理和编码的更多说明。
//     *
//     * @param name   查询参数名称
//     * @param values 查询参数值
//     * @see #queryParam(String, Object...)
//     */
//    UriBuilder queryParam(String name, Collection<?> values);
//
//    /**
//     * 如果给定的{@link Optional}具有值，
//     * 则委托给{@link #queryParam(String, Object...)}或{@link #queryParam(String, Collection)}；
//     * 否则，如果该值为空，则根本不添加查询参数。
//     *
//     * @param name  查询参数名称
//     * @param value 可选，为空或保留查询参数值。
//     */
//    UriBuilder queryParamIfPresent(String name, Object value);
//
//    /**
//     * 添加多个查询参数和值。
//     * <p><strong>注: </strong> 请查看{@link #queryParam(String, Object...)}的Javadoc，
//     * 了解有关单个查询参数的处理和编码的更多说明。
//     *
//     * @param params 参数
//     */
//    UriBuilder queryParams(MultiValueMap<String, String> params);
//
//    /**
//     * 设置替换现有值的查询参数值，或者如果没有给定值，则删除查询参数。
//     * <p><strong>注: </strong> 请查看 {@link #queryParam(String, Object...)} 的Javadoc，
//     * 了解有关单个查询参数的处理和编码的更多说明。
//     *
//     * @param name   查询参数名称
//     * @param values 查询参数值
//     * @see #replaceQueryParam(String, Collection)
//     */
//    UriBuilder replaceQueryParam(String name, Object... values);
//
//    /**
//     * 带有集合的{@link #replaceQueryParam(String, Object...)}的变量。
//     * <p><strong>注：</strong> 请查看{@link #queryParam(String, Object...)}的Javadoc，
//     * 了解有关单个查询参数的处理和编码的更多说明。
//     *
//     * @param name   查询参数名称
//     * @param values 查询参数值
//     * @see #replaceQueryParam(String, Object...)
//     */
//    UriBuilder replaceQueryParam(String name, Collection<?> values);
//
//    /**
//     * 删除所有现有参数值后，设置查询参数值。
//     * <p><strong>注：</strong> 请查看 {@link #queryParam(String, Object...)} 的Javadoc，
//     * 了解有关单个查询参数的处理和编码的更多说明。
//     *
//     * @param params 查询参数名称
//     */
//    UriBuilder replaceQueryParams(MultiValueMap<String, String> params);
//
//    /**
//     * 设置URI片段。给定的片段可以包含URI模板变量，也可以是{@code null}以清除此生成器的片段。
//     *
//     * @param fragment URI fragment
//     */
//    UriBuilder fragment(String fragment);
//
//    /**
//     * 构建一个{@link URI}实例，并用数组中的值替换URI模板变量。
//     *
//     * @param uriVariables URI变量的映射
//     * @return URI
//     */
//    URI build(Object... uriVariables);
//
//    /**
//     * 构建一个{@link URI}实例，并用映射中的值替换URI模板变量。
//     *
//     * @param uriVariables URI变量的映射
//     * @return URI
//     */
//    URI build(Map<String, ?> uriVariables);
//}
