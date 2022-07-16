package org.clever.format;

/**
 * 格式化接口，支持将T类型对象格式化为字符串，也可以将字符串解析为T类型对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:42 <br/>
 */
public interface Formatter<T> extends Printer<T>, Parser<T> {
}
