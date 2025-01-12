// Generated from /resources\SqlFuncParser.g4 by ANTLR 4.9.2
package org.clever.data.dynamic.sql.dialect.antlr;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SqlFuncParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 *            operations with no return type.
 */
public interface SqlFuncParserVisitor<T> extends ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by {@link SqlFuncParser#javaFunc}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitJavaFunc(SqlFuncParser.JavaFuncContext ctx);

    /**
     * Visit a parse tree produced by {@link SqlFuncParser#javaParameterList}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitJavaParameterList(SqlFuncParser.JavaParameterListContext ctx);

    /**
     * Visit a parse tree produced by {@link SqlFuncParser#javaParameter}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitJavaParameter(SqlFuncParser.JavaParameterContext ctx);

    /**
     * Visit a parse tree produced by {@link SqlFuncParser#javaVar}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitJavaVar(SqlFuncParser.JavaVarContext ctx);

    /**
     * Visit a parse tree produced by {@link SqlFuncParser#sqlFunc}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSqlFunc(SqlFuncParser.SqlFuncContext ctx);

    /**
     * Visit a parse tree produced by {@link SqlFuncParser#sqlParameterList}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSqlParameterList(SqlFuncParser.SqlParameterListContext ctx);

    /**
     * Visit a parse tree produced by {@link SqlFuncParser#sqlParameter}.
     *
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSqlParameter(SqlFuncParser.SqlParameterContext ctx);
}
