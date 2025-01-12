// Generated from /resources\SqlFuncParser.g4 by ANTLR 4.9.2
package org.clever.data.dynamic.sql.dialect.antlr;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SqlFuncParser}.
 */
public interface SqlFuncParserListener extends ParseTreeListener {
    /**
     * Enter a parse tree produced by {@link SqlFuncParser#javaFunc}.
     *
     * @param ctx the parse tree
     */
    void enterJavaFunc(SqlFuncParser.JavaFuncContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#javaFunc}.
     *
     * @param ctx the parse tree
     */
    void exitJavaFunc(SqlFuncParser.JavaFuncContext ctx);

    /**
     * Enter a parse tree produced by {@link SqlFuncParser#javaParameterList}.
     *
     * @param ctx the parse tree
     */
    void enterJavaParameterList(SqlFuncParser.JavaParameterListContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#javaParameterList}.
     *
     * @param ctx the parse tree
     */
    void exitJavaParameterList(SqlFuncParser.JavaParameterListContext ctx);

    /**
     * Enter a parse tree produced by {@link SqlFuncParser#javaParameter}.
     *
     * @param ctx the parse tree
     */
    void enterJavaParameter(SqlFuncParser.JavaParameterContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#javaParameter}.
     *
     * @param ctx the parse tree
     */
    void exitJavaParameter(SqlFuncParser.JavaParameterContext ctx);

    /**
     * Enter a parse tree produced by {@link SqlFuncParser#javaVar}.
     *
     * @param ctx the parse tree
     */
    void enterJavaVar(SqlFuncParser.JavaVarContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#javaVar}.
     *
     * @param ctx the parse tree
     */
    void exitJavaVar(SqlFuncParser.JavaVarContext ctx);

    /**
     * Enter a parse tree produced by {@link SqlFuncParser#sqlFunc}.
     *
     * @param ctx the parse tree
     */
    void enterSqlFunc(SqlFuncParser.SqlFuncContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#sqlFunc}.
     *
     * @param ctx the parse tree
     */
    void exitSqlFunc(SqlFuncParser.SqlFuncContext ctx);

    /**
     * Enter a parse tree produced by {@link SqlFuncParser#sqlParameterList}.
     *
     * @param ctx the parse tree
     */
    void enterSqlParameterList(SqlFuncParser.SqlParameterListContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#sqlParameterList}.
     *
     * @param ctx the parse tree
     */
    void exitSqlParameterList(SqlFuncParser.SqlParameterListContext ctx);

    /**
     * Enter a parse tree produced by {@link SqlFuncParser#sqlParameter}.
     *
     * @param ctx the parse tree
     */
    void enterSqlParameter(SqlFuncParser.SqlParameterContext ctx);

    /**
     * Exit a parse tree produced by {@link SqlFuncParser#sqlParameter}.
     *
     * @param ctx the parse tree
     */
    void exitSqlParameter(SqlFuncParser.SqlParameterContext ctx);
}
