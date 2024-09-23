//package org.clever.web.utils;
//
//import org.clever.util.CollectionUtils;
//import org.clever.util.LinkedMultiValueMap;
//import org.clever.util.MultiValueMap;
//import org.clever.util.StringUtils;
//
//import java.nio.charset.Charset;
//import java.nio.charset.StandardCharsets;
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//
///**
// * 基于RFC 3986的URI编码和解码实用方法。
// *
// * <p>有两种类型的编码方法：
// * <ul>
// * <li>{@code "encodeXyz"} -- 如RFC 3986中定义的，它们通过对非法字符（包括非美国ASCII字符）
// * 以及在给定URI组件类型中非法的字符进行百分比编码来编码特定URI组件（例如路径、查询）。
// * 这种方法在编码方面的效果与使用URI的多参数构造函数相当。
// * <li>{@code "encode"} 和 {@code "encodeUriVariables"} -- 这些可用于对URI变量值进行编码，
// * 方法是对URI中任何位置的所有非法字符或具有任何保留含义的字符进行百分比编码。
// * </ul>
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/12/24 16:01 <br/>
// *
// * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC 3986</a>
// */
//public abstract class UriUtils {
//    /**
//     * 使用给定的编码对给定的 URI 方案进行编码。
//     *
//     * @param scheme   要编码的scheme
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodeScheme(String scheme, String encoding) {
//        return encode(scheme, encoding, HierarchicalUriComponents.Type.SCHEME);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI scheme进行编码。
//     *
//     * @param scheme  要编码的scheme
//     * @param charset 要编码到的字符编码
//     */
//    public static String encodeScheme(String scheme, Charset charset) {
//        return encode(scheme, charset, HierarchicalUriComponents.Type.SCHEME);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI authority进行编码
//     *
//     * @param authority 被编码的authority
//     * @param encoding  要编码到的字符编码
//     */
//    public static String encodeAuthority(String authority, String encoding) {
//        return encode(authority, encoding, HierarchicalUriComponents.Type.AUTHORITY);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI authority进行编码。
//     *
//     * @param authority 被编码的authority
//     * @param charset   要编码到的字符编码
//     */
//    public static String encodeAuthority(String authority, Charset charset) {
//        return encode(authority, charset, HierarchicalUriComponents.Type.AUTHORITY);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI userInfo进行编码。
//     *
//     * @param userInfo 要编码的userInfo
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodeUserInfo(String userInfo, String encoding) {
//        return encode(userInfo, encoding, HierarchicalUriComponents.Type.USER_INFO);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI userInfo进行编码。
//     *
//     * @param userInfo 要编码的userInfo
//     * @param charset  要编码到的字符编码
//     */
//    public static String encodeUserInfo(String userInfo, Charset charset) {
//        return encode(userInfo, charset, HierarchicalUriComponents.Type.USER_INFO);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI host进行编码。
//     *
//     * @param host     要编码的host
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodeHost(String host, String encoding) {
//        return encode(host, encoding, HierarchicalUriComponents.Type.HOST_IPV4);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI host进行编码。
//     *
//     * @param host    要编码的host
//     * @param charset 要编码到的字符编码
//     */
//    public static String encodeHost(String host, Charset charset) {
//        return encode(host, charset, HierarchicalUriComponents.Type.HOST_IPV4);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI port进行编码。
//     *
//     * @param port     要编码的port
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodePort(String port, String encoding) {
//        return encode(port, encoding, HierarchicalUriComponents.Type.PORT);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI port进行编码。
//     *
//     * @param port    要编码的port
//     * @param charset 要编码到的字符编码
//     */
//    public static String encodePort(String port, Charset charset) {
//        return encode(port, charset, HierarchicalUriComponents.Type.PORT);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI path进行编码。
//     *
//     * @param path     要编码的path
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodePath(String path, String encoding) {
//        return encode(path, encoding, HierarchicalUriComponents.Type.PATH);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI path进行编码。
//     *
//     * @param path    要编码的path
//     * @param charset 要编码到的字符编码
//     */
//    public static String encodePath(String path, Charset charset) {
//        return encode(path, charset, HierarchicalUriComponents.Type.PATH);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI segment进行编码。
//     *
//     * @param segment  要编码的segment
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodePathSegment(String segment, String encoding) {
//        return encode(segment, encoding, HierarchicalUriComponents.Type.PATH_SEGMENT);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI segment进行编码。
//     *
//     * @param segment 要编码的segment
//     * @param charset 要编码到的字符编码
//     */
//    public static String encodePathSegment(String segment, Charset charset) {
//        return encode(segment, charset, HierarchicalUriComponents.Type.PATH_SEGMENT);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI query进行编码。
//     *
//     * @param query    要编码的 query
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodeQuery(String query, String encoding) {
//        return encode(query, encoding, HierarchicalUriComponents.Type.QUERY);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI query进行编码。
//     *
//     * @param query   要编码的 query
//     * @param charset 要编码到的字符编码
//     */
//    public static String encodeQuery(String query, Charset charset) {
//        return encode(query, charset, HierarchicalUriComponents.Type.QUERY);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI queryParam进行编码。
//     *
//     * @param queryParam 要编码的 queryParam
//     * @param encoding   要编码到的字符编码
//     */
//    public static String encodeQueryParam(String queryParam, String encoding) {
//        return encode(queryParam, encoding, HierarchicalUriComponents.Type.QUERY_PARAM);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI queryParam进行编码。
//     *
//     * @param queryParam 要编码的 queryParam
//     * @param charset    要编码到的字符编码
//     */
//    public static String encodeQueryParam(String queryParam, Charset charset) {
//        return encode(queryParam, charset, HierarchicalUriComponents.Type.QUERY_PARAM);
//    }
//
//    /**
//     * 使用 UTF-8 对来自给定 {@code MultiValueMap} 的查询参数进行编码。
//     * <p>当从已经编码的模板构建 URI 时，这可以与 {@link UriComponentsBuilder#queryParams(MultiValueMap)} 一起使用。
//     * <pre>{@code
//     * MultiValueMap<String, String> params = new LinkedMultiValueMap<>(2);
//     * // add to params...
//     * ServletUriComponentsBuilder.fromCurrentRequest()
//     *         .queryParams(UriUtils.encodeQueryParams(params))
//     *         .build(true)
//     *         .toUriString();
//     * }</pre>
//     *
//     * @param params 要编码的 params
//     * @return 带有编码名称和值的新 {@code MultiValueMap}
//     */
//    public static MultiValueMap<String, String> encodeQueryParams(MultiValueMap<String, String> params) {
//        Charset charset = StandardCharsets.UTF_8;
//        MultiValueMap<String, String> result = new LinkedMultiValueMap<>(params.size());
//        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
//            for (String value : entry.getValue()) {
//                result.add(encodeQueryParam(entry.getKey(), charset), encodeQueryParam(value, charset));
//            }
//        }
//        return result;
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI fragment进行编码。
//     *
//     * @param fragment 要编码的 fragment
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encodeFragment(String fragment, String encoding) {
//        return encode(fragment, encoding, HierarchicalUriComponents.Type.FRAGMENT);
//    }
//
//    /**
//     * 使用给定的编码对给定的 URI fragment进行编码。
//     *
//     * @param fragment 要编码的 fragment
//     * @param charset  要编码到的字符编码
//     */
//    public static String encodeFragment(String fragment, Charset charset) {
//        return encode(fragment, charset, HierarchicalUriComponents.Type.FRAGMENT);
//    }
//
//    /**
//     * {@link #encode(String, Charset)} 的变体，带有 String 字符集。
//     *
//     * @param source   要编码的字符串
//     * @param encoding 要编码到的字符编码
//     */
//    public static String encode(String source, String encoding) {
//        return encode(source, encoding, HierarchicalUriComponents.Type.URI);
//    }
//
//    /**
//     * 对 URI 内任意位置的所有非法字符或具有任何保留含义的字符进行编码，如<a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a>中所定义.
//     * 这有助于确保给定的 String 将按原样保留并且不会对 URI 的结构或含义产生任何影响。
//     *
//     * @param source  要编码的字符串
//     * @param charset 要编码到的字符编码
//     */
//    public static String encode(String source, Charset charset) {
//        return encode(source, charset, HierarchicalUriComponents.Type.URI);
//    }
//
//    /**
//     * 将 {@link #encode(String, Charset)} 应用于所有给定 URI 变量值的便捷方法。
//     *
//     * @param uriVariables 要编码的 URI 变量值
//     */
//    public static Map<String, String> encodeUriVariables(Map<String, ?> uriVariables) {
//        Map<String, String> result = CollectionUtils.newLinkedHashMap(uriVariables.size());
//        uriVariables.forEach((key, value) -> {
//            String stringValue = (value != null ? value.toString() : "");
//            result.put(key, encode(stringValue, StandardCharsets.UTF_8));
//        });
//        return result;
//    }
//
//    /**
//     * 将 {@link #encode(String, Charset)} 应用于所有给定 URI 变量值的便捷方法。
//     *
//     * @param uriVariables 要编码的 URI 变量值
//     */
//    public static Object[] encodeUriVariables(Object... uriVariables) {
//        return Arrays.stream(uriVariables).map(value -> {
//            String stringValue = (value != null ? value.toString() : "");
//            return encode(stringValue, StandardCharsets.UTF_8);
//        }).toArray();
//    }
//
//    private static String encode(String scheme, String encoding, HierarchicalUriComponents.Type type) {
//        return HierarchicalUriComponents.encodeUriComponent(scheme, encoding, type);
//    }
//
//    private static String encode(String scheme, Charset charset, HierarchicalUriComponents.Type type) {
//        return HierarchicalUriComponents.encodeUriComponent(scheme, charset, type);
//    }
//
//    /**
//     * 解码给定的编码 URI 组件。
//     * <p>有关解码规则，请参阅 {@link StringUtils#uriDecode(String, Charset)}。
//     *
//     * @param source   编码的字符串
//     * @param encoding 要使用的字符编码
//     * @return 解码值
//     * @throws IllegalArgumentException 当给定的源包含无效的编码序列时
//     * @see StringUtils#uriDecode(String, Charset)
//     * @see java.net.URLDecoder#decode(String, String)
//     */
//    public static String decode(String source, String encoding) {
//        return StringUtils.uriDecode(source, Charset.forName(encoding));
//    }
//
//    /**
//     * 解码给定的编码 URI 组件。
//     * <p>有关解码规则，请参阅 {@link StringUtils#uriDecode(String, Charset)}。
//     *
//     * @param source  编码的字符串
//     * @param charset 要使用的字符编码
//     * @return 解码值
//     * @throws IllegalArgumentException 当给定的源包含无效的编码序列时
//     * @see StringUtils#uriDecode(String, Charset)
//     * @see java.net.URLDecoder#decode(String, String)
//     */
//    public static String decode(String source, Charset charset) {
//        return StringUtils.uriDecode(source, charset);
//    }
//
//    /**
//     * 从给定的 URI 路径中提取文件扩展名。
//     *
//     * @param path URI 路径（例如“products/index.html”）
//     * @return 提取的文件扩展名（例如“html”）
//     */
//    public static String extractFileExtension(String path) {
//        int end = path.indexOf('?');
//        int fragmentIndex = path.indexOf('#');
//        if (fragmentIndex != -1 && (end == -1 || fragmentIndex < end)) {
//            end = fragmentIndex;
//        }
//        if (end == -1) {
//            end = path.length();
//        }
//        int begin = path.lastIndexOf('/', end) + 1;
//        int paramIndex = path.indexOf(';', begin);
//        end = (paramIndex != -1 && paramIndex < end ? paramIndex : end);
//        int extIndex = path.lastIndexOf('.', end);
//        if (extIndex != -1 && extIndex > begin) {
//            return path.substring(extIndex + 1, end);
//        }
//        return null;
//    }
//}
