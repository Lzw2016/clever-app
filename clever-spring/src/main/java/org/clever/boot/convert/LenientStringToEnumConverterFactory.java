package org.clever.boot.convert;

/**
 * 把 {@link String} 转换成 {@link java.lang.Enum} 具有宽松的转换规则. 明确地:
 * <ul>
 * <li>使用不区分大小写的搜索</li>
 * <li>不考虑 {@code '_'}, {@code '$'} 或其他特殊字符</li>
 * <li>允许映射 {@code "false"} and {@code "true"} 到枚举 {@code ON} 和 {@code OFF}</li>
 * </ul>
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:56 <br/>
 */
final class LenientStringToEnumConverterFactory extends LenientObjectToEnumConverterFactory<String> {
}
