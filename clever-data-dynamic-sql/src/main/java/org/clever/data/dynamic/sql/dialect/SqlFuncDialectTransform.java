package org.clever.data.dynamic.sql.dialect;

import lombok.Getter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.clever.data.dynamic.sql.dialect.antlr.SqlFuncLexer;
import org.clever.data.dynamic.sql.dialect.antlr.SqlFuncParser;
import org.clever.data.dynamic.sql.dialect.antlr.SqlFuncParserBaseListener;
import org.clever.data.dynamic.sql.dialect.exception.ParseSqlFuncException;
import org.clever.data.dynamic.sql.dialect.utils.SqlFuncTransformUtils;
import org.clever.data.dynamic.sql.ognl.OgnlCache;
import org.clever.data.dynamic.sql.utils.SqlParameterNameStrategy;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Stack;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/10 17:02 <br/>
 */
public class SqlFuncDialectTransform extends SqlFuncParserBaseListener {
    private final Object ognlRoot;

    /**
     * 是否有解析错误
     */
    @Getter
    private boolean hasError = false;
    /**
     * 最外层sql函数
     */
    private SqlFuncNode rootSqlFuncNode;
    /**
     * 解析sql函数的栈容器
     */
    private final Stack<SqlFuncNode> sqlFuncNodeStack = new Stack<>();

    /**
     * 转换后的sql函数代码
     */
    public String toSql(DbType dbType) {
        if (hasError) {
            throw new ParseSqlFuncException("解析SQL函数失败");
        }
        SqlFuncNode target = SqlFuncTransformUtils.transform(rootSqlFuncNode, dbType);
        return SqlFuncTransformUtils.toSql(target);
    }

    /**
     * sql变量
     */
    public LinkedHashMap<String, Object> getSqlVariable(DbType dbType) {
        if (hasError) {
            throw new ParseSqlFuncException("解析SQL函数失败");
        }
        SqlFuncNode target = SqlFuncTransformUtils.transform(rootSqlFuncNode, dbType);
        return SqlFuncTransformUtils.getSqlVariable(target);
    }

    /**
     * @param sqlFuc   数据库函数(原始函数)
     * @param ognlRoot ognl表达式的root对象
     */
    public SqlFuncDialectTransform(String sqlFuc, Object ognlRoot) {
        this.ognlRoot = ognlRoot;
        CharStream charStream = CharStreams.fromString(sqlFuc);
        SqlFuncLexer lexer = new SqlFuncLexer(charStream);
        CommonTokenStream tokenStream = new CommonTokenStream(lexer);
        SqlFuncParser parser = new SqlFuncParser(tokenStream);
        ParseTree tree = parser.sqlFunc();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
    }

    @Override
    public void enterSqlFunc(SqlFuncParser.SqlFuncContext ctx) {
        if (hasError) {
            return;
        }
        String sqlFuncName = ctx.IDENTIFIER().getSymbol().getText();
        // 上级函数存在 & 上级函数的最后一个参数是SqlFuncNode
        if (!sqlFuncNodeStack.isEmpty()) {
            SqlFuncNode parentSqlFuncNode = sqlFuncNodeStack.peek();
            if (parentSqlFuncNode != null && !parentSqlFuncNode.getParams().isEmpty()) {
                SqlFuncNodeParam param = parentSqlFuncNode.getParams().get(parentSqlFuncNode.getParams().size() - 1);
                if (SqlFuncNodeParamEnum.SQL_FUNC.equals(param.getType())
                        && param.getFunc() != null
                        && Objects.equals(sqlFuncName, param.getFunc().getFuncName())) {
                    sqlFuncNodeStack.push(param.getFunc());
                } else {
                    throw new ParseSqlFuncException("解析SQL函数异常：sqlFuncName=" + sqlFuncName + " | param=" + param);
                }
                return;
            }
        }
        // 当前SQL函数入栈
        SqlFuncNode sqlFuncNode = new SqlFuncNode(sqlFuncName);
        sqlFuncNodeStack.push(sqlFuncNode);
        if (rootSqlFuncNode == null) {
            rootSqlFuncNode = sqlFuncNode;
        }
    }

    @Override
    public void exitSqlFunc(SqlFuncParser.SqlFuncContext ctx) {
        if (hasError) {
            return;
        }
        sqlFuncNodeStack.pop();
    }

    @Override
    public void enterSqlParameter(SqlFuncParser.SqlParameterContext ctx) {
        if (hasError) {
            return;
        }
        if (sqlFuncNodeStack.isEmpty()) {
            throw new ParseSqlFuncException("解析SQL函数失败");
        }
        SqlFuncNode sqlFuncNode = sqlFuncNodeStack.peek();
        SqlFuncParser.SqlFuncContext sqlFuncContext = ctx.sqlFunc();
        SqlFuncParser.JavaVarContext javaVarContext = ctx.javaVar();
        SqlFuncParser.JavaFuncContext javaFuncContext = ctx.javaFunc();
        SqlFuncNodeParam param;
        if (sqlFuncContext != null) {
            // sql函数
            String funcName = sqlFuncContext.IDENTIFIER().getSymbol().getText();
            SqlFuncNode sqlFunc = new SqlFuncNode(funcName);
            param = new SqlFuncNodeParam(sqlFunc);
        } else if (javaVarContext != null) {
            // java变量(链式变量取值)
            String literal = javaVarContext.getText();
            Object value = OgnlCache.getValue(literal, ognlRoot);
            String name = SqlParameterNameStrategy.rename(literal);
            SqlFuncParam sqlFuncParam = new SqlFuncParam(true, literal, name, value);
            param = new SqlFuncNodeParam(sqlFuncParam);
        } else if (javaFuncContext != null) {
            // java函数
            String literal = javaFuncContext.getText();
            Object value = OgnlCache.getValue(literal, ognlRoot);
            String name = SqlParameterNameStrategy.rename(literal);
            SqlFuncParam sqlFuncParam = new SqlFuncParam(true, literal, name, value);
            param = new SqlFuncNodeParam(sqlFuncParam);
        } else {
            // null值 | bool值 | 整数数 | 浮点数 | 字符串值
            String literal = ctx.getText();
            SqlFuncParam sqlFuncParam = new SqlFuncParam(false, literal, null, null);
            param = new SqlFuncNodeParam(sqlFuncParam);
        }
        sqlFuncNode.getParams().add(param);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        hasError = true;
        // throw new ParseSqlFuncException("解析SQL函数失败");
    }
}
