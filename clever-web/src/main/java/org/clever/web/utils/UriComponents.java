//package org.clever.web.utils;
//
//import org.clever.util.Assert;
//import org.clever.util.MultiValueMap;
//
//import java.io.Serializable;
//import java.net.URI;
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.function.UnaryOperator;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * 表示 URI 组件的不可变集合，将组件类型映射到 String 值。
// * 实际上类似于 {@link java.net.URI}，但具有更强大的编码选项和对 URI 模板变量的支持。
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/12/24 16:03 <br/>
// *
// * @see UriComponentsBuilder
// */
//public abstract class UriComponents implements Serializable {
//    /**
//     * 捕获 URI 模板变量名称
//     */
//    private static final Pattern NAMES_PATTERN = Pattern.compile("\\{([^/]+?)}");
//    private final String scheme;
//    private final String fragment;
//
//    protected UriComponents(String scheme, String fragment) {
//        this.scheme = scheme;
//        this.fragment = fragment;
//    }
//
//    // Component getters
//
//    /**
//     * 返回 scheme. 可以是 {@code null}.
//     */
//    public final String getScheme() {
//        return this.scheme;
//    }
//
//    /**
//     * 返回 fragment. 可以是 {@code null}.
//     */
//    public final String getFragment() {
//        return this.fragment;
//    }
//
//    /**
//     * 返回 scheme specific part. 可以是 {@code null}.
//     */
//    public abstract String getSchemeSpecificPart();
//
//    /**
//     * 返回 user info. 可以是 {@code null}.
//     */
//    public abstract String getUserInfo();
//
//    /**
//     * 返回 host. 可以是 {@code null}.
//     */
//    public abstract String getHost();
//
//    /**
//     * 返回 port. {@code -1} 如果没有设置端口.
//     */
//    public abstract int getPort();
//
//    /**
//     * 返回 path. 可以是 {@code null}.
//     */
//    public abstract String getPath();
//
//    /**
//     * 返回path segments列表. 如果没有设置路径则为空。
//     */
//    public abstract List<String> getPathSegments();
//
//    /**
//     * 返回 query. 可以是 {@code null}.
//     */
//    public abstract String getQuery();
//
//    /**
//     * 返回 query parameters. 如果没有设置查询则为空
//     */
//    public abstract MultiValueMap<String, String> getQueryParams();
//
//    /**
//     * 在扩展 URI 变量以编码生成的 URI 组件值之后调用它。
//     * <p>与 UriComponentsBuilder.encode() 相比，
//     * 此方法仅替换非 ASCII 和非法（在给定的 URI 组件类型内）字符，但不替换具有保留含义的字符。
//     * 对于大多数情况，UriComponentsBuilder.encode() 更有可能给出预期的结果。
//     *
//     * @see UriComponentsBuilder#encode()
//     */
//    public final UriComponents encode() {
//        return encode(StandardCharsets.UTF_8);
//    }
//
//    /**
//     * {@link #encode()} 的变体，其字符集不是“UTF-8”。
//     *
//     * @param charset 用于编码的字符集
//     * @see UriComponentsBuilder#encode(Charset)
//     */
//    public abstract UriComponents encode(Charset charset);
//
//    /**
//     * 用给定映射中的值替换所有 URI 模板变量。
//     * <p>给定的映射键代表变量名；相应的值表示变量值。变量的顺序并不重要。
//     *
//     * @param uriVariables URI 变量的映射
//     * @return 扩展的 URI 组件
//     */
//    public final UriComponents expand(Map<String, ?> uriVariables) {
//        Assert.notNull(uriVariables, "'uriVariables' must not be null");
//        return expandInternal(new MapTemplateVariables(uriVariables));
//    }
//
//    /**
//     * 用给定数组中的值替换所有 URI 模板变量。
//     * <p>给定的数组表示变量值。变量的顺序很重要。
//     *
//     * @param uriVariableValues URI 变量值
//     * @return 扩展的 URI 组件
//     */
//    public final UriComponents expand(Object... uriVariableValues) {
//        Assert.notNull(uriVariableValues, "'uriVariableValues' must not be null");
//        return expandInternal(new VarArgsTemplateVariables(uriVariableValues));
//    }
//
//    /**
//     * 用给定的 {@link UriTemplateVariables} 中的值替换所有 URI 模板变量
//     *
//     * @param uriVariables URI 模板值
//     * @return 扩展的 URI 组件
//     */
//    public final UriComponents expand(UriTemplateVariables uriVariables) {
//        Assert.notNull(uriVariables, "'uriVariables' must not be null");
//        return expandInternal(uriVariables);
//    }
//
//    /**
//     * 用给定的 {@link UriTemplateVariables} 中的值替换所有 URI 模板变量
//     *
//     * @param uriVariables URI 模板值
//     * @return 扩展的 URI 组件
//     */
//    abstract UriComponents expandInternal(UriTemplateVariables uriVariables);
//
//    /**
//     * 标准化路径删除序列，如“path/..”。请注意，规范化适用于完整路径，而不适用于单个路径段
//     *
//     * @see org.clever.util.StringUtils#cleanPath(String)
//     */
//    public abstract UriComponents normalize();
//
//    /**
//     * 连接所有 URI 组件以返回完全形成的 URI 字符串。
//     * <p>此方法相当于当前 URI 组件值的简单字符串串联，因此结果可能包含非法 URI 字符，
//     * 例如，如果 URI 变量尚未扩展或未通过 UriComponentsBuilder.encode() 或 encode() 应用编码.
//     */
//    public abstract String toUriString();
//
//    /**
//     * 从此实例创建一个 {@link URI}，如下所示：
//     * <p>如果当前实例已编码，则通过 toUriString() 形成完整的 URI 字符串，然后将其传递给保留百分比编码的单参数 URI 构造函数。
//     * <p>如果尚未编码，则将各个 URI 组件值传递给多参数 URI 构造函数，该构造函数引用不能出现在其各自 URI 组件中的非法字符。
//     */
//    public abstract URI toUri();
//
//    /**
//     * 到 {@link #toUriString()} 的简单传递。
//     */
//    @Override
//    public final String toString() {
//        return toUriString();
//    }
//
//    /**
//     * 设置给定 UriComponentsBuilder 的所有组件。
//     */
//    protected abstract void copyToUriComponentsBuilder(UriComponentsBuilder builder);
//
//    // Static expansion helpers
//
//    static String expandUriComponent(String source, UriTemplateVariables uriVariables) {
//        return expandUriComponent(source, uriVariables, null);
//    }
//
//    static String expandUriComponent(String source, UriTemplateVariables uriVariables, UnaryOperator<String> encoder) {
//        if (source == null) {
//            return null;
//        }
//        if (source.indexOf('{') == -1) {
//            return source;
//        }
//        if (source.indexOf(':') != -1) {
//            source = sanitizeSource(source);
//        }
//        Matcher matcher = NAMES_PATTERN.matcher(source);
//        StringBuffer sb = new StringBuffer();
//        while (matcher.find()) {
//            String match = matcher.group(1);
//            String varName = getVariableName(match);
//            Object varValue = uriVariables.getValue(varName);
//            if (UriTemplateVariables.SKIP_VALUE.equals(varValue)) {
//                continue;
//            }
//            String formatted = getVariableValueAsString(varValue);
//            formatted = encoder != null ? encoder.apply(formatted) : Matcher.quoteReplacement(formatted);
//            matcher.appendReplacement(sb, formatted);
//        }
//        matcher.appendTail(sb);
//        return sb.toString();
//    }
//
//    /**
//     * 移除嵌套的“{}”，例如使用正则表达式的 URI 变量
//     */
//    private static String sanitizeSource(String source) {
//        int level = 0;
//        int lastCharIndex = 0;
//        char[] chars = new char[source.length()];
//        for (int i = 0; i < source.length(); i++) {
//            char c = source.charAt(i);
//            if (c == '{') {
//                level++;
//            }
//            if (c == '}') {
//                level--;
//            }
//            if (level > 1 || (level == 1 && c == '}')) {
//                continue;
//            }
//            chars[lastCharIndex++] = c;
//        }
//        return new String(chars, 0, lastCharIndex);
//    }
//
//    private static String getVariableName(String match) {
//        int colonIdx = match.indexOf(':');
//        return (colonIdx != -1 ? match.substring(0, colonIdx) : match);
//    }
//
//    private static String getVariableValueAsString(Object variableValue) {
//        return (variableValue != null ? variableValue.toString() : "");
//    }
//
//    /**
//     * 定义 URI 模板变量的约定
//     *
//     * @see HierarchicalUriComponents#expand
//     */
//    public interface UriTemplateVariables {
//        /**
//         * 指示 URI 变量名称的值的常量应被忽略并保持原样。这对于部分扩展某些而非所有 URI 变量很有用。
//         */
//        Object SKIP_VALUE = UriTemplateVariables.class;
//
//        /**
//         * 获取给定 URI 变量名称的值。
//         * 如果值为 {@code null}，则展开一个空字符串。
//         * 如果值为 {@link #SKIP_VALUE}，则不扩展 URI 变量。
//         *
//         * @param name 变量名
//         * @return 变量值，可能是 {@code null} 或 {@link #SKIP_VALUE}
//         */
//        Object getValue(String name);
//    }
//
//    /**
//     * 地图支持的 URI 模板变量。
//     */
//    private static class MapTemplateVariables implements UriTemplateVariables {
//        private final Map<String, ?> uriVariables;
//
//        public MapTemplateVariables(Map<String, ?> uriVariables) {
//            this.uriVariables = uriVariables;
//        }
//
//        @Override
//        public Object getValue(String name) {
//            if (!this.uriVariables.containsKey(name)) {
//                throw new IllegalArgumentException("Map has no value for '" + name + "'");
//            }
//            return this.uriVariables.get(name);
//        }
//    }
//
//    /**
//     * 由可变参数数组支持的 URI 模板变量。
//     */
//    private static class VarArgsTemplateVariables implements UriTemplateVariables {
//        private final Iterator<Object> valueIterator;
//
//        public VarArgsTemplateVariables(Object... uriVariableValues) {
//            this.valueIterator = Arrays.asList(uriVariableValues).iterator();
//        }
//
//        @Override
//        public Object getValue(String name) {
//            if (!this.valueIterator.hasNext()) {
//                throw new IllegalArgumentException("Not enough variable values available to expand '" + name + "'");
//            }
//            return this.valueIterator.next();
//        }
//    }
//}
