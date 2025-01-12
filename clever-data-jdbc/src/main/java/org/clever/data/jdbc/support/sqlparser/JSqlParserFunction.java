package org.clever.data.jdbc.support.sqlparser;

import net.sf.jsqlparser.JSQLParserException;

/**
 * 作者：lizw <br/>
 * 创建时间：2025/01/12 22:29 <br/>
 */
@FunctionalInterface
public interface JSqlParserFunction<T, R> {
    R parse(T t) throws JSQLParserException;
}
