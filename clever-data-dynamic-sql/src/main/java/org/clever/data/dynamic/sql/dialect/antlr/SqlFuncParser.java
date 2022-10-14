// Generated from/resources\SqlFuncParser.g4 by ANTLR 4.9.2
package org.clever.data.dynamic.sql.dialect.antlr;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNDeserializer;
import org.antlr.v4.runtime.atn.ParserATNSimulator;
import org.antlr.v4.runtime.atn.PredictionContextCache;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class SqlFuncParser extends Parser {
    static {
        RuntimeMetaData.checkVersion("4.9.2", RuntimeMetaData.VERSION);
    }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
            new PredictionContextCache();
    public static final int
            LPAREN = 1, RPAREN = 2, COMMA = 3, DOT = 4, IDENTIFIER = 5, NULL_LITERAL = 6, BOOL_LITERAL = 7,
            DECIMAL_LITERAL = 8, FLOAT_LITERAL = 9, STRING_LITERAL = 10, WS = 11;
    public static final int
            RULE_javaFunc = 0, RULE_javaParameterList = 1, RULE_javaParameter = 2,
            RULE_javaVar = 3, RULE_sqlFunc = 4, RULE_sqlParameterList = 5, RULE_sqlParameter = 6;

    private static String[] makeRuleNames() {
        return new String[]{
                "javaFunc", "javaParameterList", "javaParameter", "javaVar", "sqlFunc",
                "sqlParameterList", "sqlParameter"
        };
    }

    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[]{
                null, "'('", "')'", "','", "'.'", null, "'null'"
        };
    }

    private static final String[] _LITERAL_NAMES = makeLiteralNames();

    private static String[] makeSymbolicNames() {
        return new String[]{
                null, "LPAREN", "RPAREN", "COMMA", "DOT", "IDENTIFIER", "NULL_LITERAL",
                "BOOL_LITERAL", "DECIMAL_LITERAL", "FLOAT_LITERAL", "STRING_LITERAL",
                "WS"
        };
    }

    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;

    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }

    @Override
    public String getGrammarFileName() {
        return "SqlFuncParser.g4";
    }

    @Override
    public String[] getRuleNames() {
        return ruleNames;
    }

    @Override
    public String getSerializedATN() {
        return _serializedATN;
    }

    @Override
    public ATN getATN() {
        return _ATN;
    }

    public SqlFuncParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
    }

    public static class JavaFuncContext extends ParserRuleContext {
        public List<TerminalNode> IDENTIFIER() {
            return getTokens(SqlFuncParser.IDENTIFIER);
        }

        public TerminalNode IDENTIFIER(int i) {
            return getToken(SqlFuncParser.IDENTIFIER, i);
        }

        public TerminalNode LPAREN() {
            return getToken(SqlFuncParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(SqlFuncParser.RPAREN, 0);
        }

        public List<TerminalNode> DOT() {
            return getTokens(SqlFuncParser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(SqlFuncParser.DOT, i);
        }

        public List<JavaParameterListContext> javaParameterList() {
            return getRuleContexts(JavaParameterListContext.class);
        }

        public JavaParameterListContext javaParameterList(int i) {
            return getRuleContext(JavaParameterListContext.class, i);
        }

        public JavaFuncContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_javaFunc;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterJavaFunc(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitJavaFunc(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitJavaFunc(this);
            else return visitor.visitChildren(this);
        }
    }

    public final JavaFuncContext javaFunc() throws RecognitionException {
        JavaFuncContext _localctx = new JavaFuncContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_javaFunc);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(14);
                match(IDENTIFIER);
                setState(17);
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                        {
                            setState(15);
                            match(DOT);
                            setState(16);
                            match(IDENTIFIER);
                        }
                    }
                    setState(19);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while (_la == DOT);
                setState(21);
                match(LPAREN);
                setState(25);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IDENTIFIER) | (1L << NULL_LITERAL) | (1L << BOOL_LITERAL) | (1L << DECIMAL_LITERAL) | (1L << FLOAT_LITERAL) | (1L << STRING_LITERAL))) != 0)) {
                    {
                        {
                            setState(22);
                            javaParameterList();
                        }
                    }
                    setState(27);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(28);
                match(RPAREN);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class JavaParameterListContext extends ParserRuleContext {
        public List<JavaParameterContext> javaParameter() {
            return getRuleContexts(JavaParameterContext.class);
        }

        public JavaParameterContext javaParameter(int i) {
            return getRuleContext(JavaParameterContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(SqlFuncParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(SqlFuncParser.COMMA, i);
        }

        public JavaParameterListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_javaParameterList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterJavaParameterList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitJavaParameterList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitJavaParameterList(this);
            else return visitor.visitChildren(this);
        }
    }

    public final JavaParameterListContext javaParameterList() throws RecognitionException {
        JavaParameterListContext _localctx = new JavaParameterListContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_javaParameterList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(30);
                javaParameter();
                setState(35);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == COMMA) {
                    {
                        {
                            setState(31);
                            match(COMMA);
                            setState(32);
                            javaParameter();
                        }
                    }
                    setState(37);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class JavaParameterContext extends ParserRuleContext {
        public TerminalNode NULL_LITERAL() {
            return getToken(SqlFuncParser.NULL_LITERAL, 0);
        }

        public TerminalNode BOOL_LITERAL() {
            return getToken(SqlFuncParser.BOOL_LITERAL, 0);
        }

        public TerminalNode DECIMAL_LITERAL() {
            return getToken(SqlFuncParser.DECIMAL_LITERAL, 0);
        }

        public TerminalNode FLOAT_LITERAL() {
            return getToken(SqlFuncParser.FLOAT_LITERAL, 0);
        }

        public TerminalNode STRING_LITERAL() {
            return getToken(SqlFuncParser.STRING_LITERAL, 0);
        }

        public JavaVarContext javaVar() {
            return getRuleContext(JavaVarContext.class, 0);
        }

        public JavaFuncContext javaFunc() {
            return getRuleContext(JavaFuncContext.class, 0);
        }

        public JavaParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_javaParameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterJavaParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitJavaParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitJavaParameter(this);
            else return visitor.visitChildren(this);
        }
    }

    public final JavaParameterContext javaParameter() throws RecognitionException {
        JavaParameterContext _localctx = new JavaParameterContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_javaParameter);
        try {
            setState(45);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 3, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(38);
                    match(NULL_LITERAL);
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(39);
                    match(BOOL_LITERAL);
                }
                break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(40);
                    match(DECIMAL_LITERAL);
                }
                break;
                case 4:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(41);
                    match(FLOAT_LITERAL);
                }
                break;
                case 5:
                    enterOuterAlt(_localctx, 5);
                {
                    setState(42);
                    match(STRING_LITERAL);
                }
                break;
                case 6:
                    enterOuterAlt(_localctx, 6);
                {
                    setState(43);
                    javaVar();
                }
                break;
                case 7:
                    enterOuterAlt(_localctx, 7);
                {
                    setState(44);
                    javaFunc();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class JavaVarContext extends ParserRuleContext {
        public List<TerminalNode> IDENTIFIER() {
            return getTokens(SqlFuncParser.IDENTIFIER);
        }

        public TerminalNode IDENTIFIER(int i) {
            return getToken(SqlFuncParser.IDENTIFIER, i);
        }

        public List<TerminalNode> DOT() {
            return getTokens(SqlFuncParser.DOT);
        }

        public TerminalNode DOT(int i) {
            return getToken(SqlFuncParser.DOT, i);
        }

        public JavaVarContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_javaVar;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterJavaVar(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitJavaVar(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitJavaVar(this);
            else return visitor.visitChildren(this);
        }
    }

    public final JavaVarContext javaVar() throws RecognitionException {
        JavaVarContext _localctx = new JavaVarContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_javaVar);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(47);
                match(IDENTIFIER);
                setState(52);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == DOT) {
                    {
                        {
                            setState(48);
                            match(DOT);
                            setState(49);
                            match(IDENTIFIER);
                        }
                    }
                    setState(54);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class SqlFuncContext extends ParserRuleContext {
        public TerminalNode IDENTIFIER() {
            return getToken(SqlFuncParser.IDENTIFIER, 0);
        }

        public TerminalNode LPAREN() {
            return getToken(SqlFuncParser.LPAREN, 0);
        }

        public TerminalNode RPAREN() {
            return getToken(SqlFuncParser.RPAREN, 0);
        }

        public List<SqlParameterListContext> sqlParameterList() {
            return getRuleContexts(SqlParameterListContext.class);
        }

        public SqlParameterListContext sqlParameterList(int i) {
            return getRuleContext(SqlParameterListContext.class, i);
        }

        public SqlFuncContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_sqlFunc;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterSqlFunc(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitSqlFunc(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitSqlFunc(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SqlFuncContext sqlFunc() throws RecognitionException {
        SqlFuncContext _localctx = new SqlFuncContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_sqlFunc);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(55);
                match(IDENTIFIER);
                setState(56);
                match(LPAREN);
                setState(60);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << IDENTIFIER) | (1L << NULL_LITERAL) | (1L << BOOL_LITERAL) | (1L << DECIMAL_LITERAL) | (1L << FLOAT_LITERAL) | (1L << STRING_LITERAL))) != 0)) {
                    {
                        {
                            setState(57);
                            sqlParameterList();
                        }
                    }
                    setState(62);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(63);
                match(RPAREN);
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class SqlParameterListContext extends ParserRuleContext {
        public List<SqlParameterContext> sqlParameter() {
            return getRuleContexts(SqlParameterContext.class);
        }

        public SqlParameterContext sqlParameter(int i) {
            return getRuleContext(SqlParameterContext.class, i);
        }

        public List<TerminalNode> COMMA() {
            return getTokens(SqlFuncParser.COMMA);
        }

        public TerminalNode COMMA(int i) {
            return getToken(SqlFuncParser.COMMA, i);
        }

        public SqlParameterListContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_sqlParameterList;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterSqlParameterList(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitSqlParameterList(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitSqlParameterList(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SqlParameterListContext sqlParameterList() throws RecognitionException {
        SqlParameterListContext _localctx = new SqlParameterListContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_sqlParameterList);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
                setState(65);
                sqlParameter();
                setState(70);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la == COMMA) {
                    {
                        {
                            setState(66);
                            match(COMMA);
                            setState(67);
                            sqlParameter();
                        }
                    }
                    setState(72);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static class SqlParameterContext extends ParserRuleContext {
        public TerminalNode NULL_LITERAL() {
            return getToken(SqlFuncParser.NULL_LITERAL, 0);
        }

        public TerminalNode BOOL_LITERAL() {
            return getToken(SqlFuncParser.BOOL_LITERAL, 0);
        }

        public TerminalNode DECIMAL_LITERAL() {
            return getToken(SqlFuncParser.DECIMAL_LITERAL, 0);
        }

        public TerminalNode FLOAT_LITERAL() {
            return getToken(SqlFuncParser.FLOAT_LITERAL, 0);
        }

        public TerminalNode STRING_LITERAL() {
            return getToken(SqlFuncParser.STRING_LITERAL, 0);
        }

        public SqlFuncContext sqlFunc() {
            return getRuleContext(SqlFuncContext.class, 0);
        }

        public JavaVarContext javaVar() {
            return getRuleContext(JavaVarContext.class, 0);
        }

        public JavaFuncContext javaFunc() {
            return getRuleContext(JavaFuncContext.class, 0);
        }

        public SqlParameterContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }

        @Override
        public int getRuleIndex() {
            return RULE_sqlParameter;
        }

        @Override
        public void enterRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).enterSqlParameter(this);
        }

        @Override
        public void exitRule(ParseTreeListener listener) {
            if (listener instanceof SqlFuncParserListener) ((SqlFuncParserListener) listener).exitSqlParameter(this);
        }

        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if (visitor instanceof SqlFuncParserVisitor) return ((SqlFuncParserVisitor<? extends T>) visitor).visitSqlParameter(this);
            else return visitor.visitChildren(this);
        }
    }

    public final SqlParameterContext sqlParameter() throws RecognitionException {
        SqlParameterContext _localctx = new SqlParameterContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_sqlParameter);
        try {
            setState(81);
            _errHandler.sync(this);
            switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
                case 1:
                    enterOuterAlt(_localctx, 1);
                {
                    setState(73);
                    match(NULL_LITERAL);
                }
                break;
                case 2:
                    enterOuterAlt(_localctx, 2);
                {
                    setState(74);
                    match(BOOL_LITERAL);
                }
                break;
                case 3:
                    enterOuterAlt(_localctx, 3);
                {
                    setState(75);
                    match(DECIMAL_LITERAL);
                }
                break;
                case 4:
                    enterOuterAlt(_localctx, 4);
                {
                    setState(76);
                    match(FLOAT_LITERAL);
                }
                break;
                case 5:
                    enterOuterAlt(_localctx, 5);
                {
                    setState(77);
                    match(STRING_LITERAL);
                }
                break;
                case 6:
                    enterOuterAlt(_localctx, 6);
                {
                    setState(78);
                    sqlFunc();
                }
                break;
                case 7:
                    enterOuterAlt(_localctx, 7);
                {
                    setState(79);
                    javaVar();
                }
                break;
                case 8:
                    enterOuterAlt(_localctx, 8);
                {
                    setState(80);
                    javaFunc();
                }
                break;
            }
        } catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        } finally {
            exitRule();
        }
        return _localctx;
    }

    public static final String _serializedATN =
            "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\rV\4\2\t\2\4\3\t" +
                    "\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\6\2\24\n\2\r\2" +
                    "\16\2\25\3\2\3\2\7\2\32\n\2\f\2\16\2\35\13\2\3\2\3\2\3\3\3\3\3\3\7\3$" +
                    "\n\3\f\3\16\3\'\13\3\3\4\3\4\3\4\3\4\3\4\3\4\3\4\5\4\60\n\4\3\5\3\5\3" +
                    "\5\7\5\65\n\5\f\5\16\58\13\5\3\6\3\6\3\6\7\6=\n\6\f\6\16\6@\13\6\3\6\3" +
                    "\6\3\7\3\7\3\7\7\7G\n\7\f\7\16\7J\13\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b" +
                    "\5\bT\n\b\3\b\2\2\t\2\4\6\b\n\f\16\2\2\2a\2\20\3\2\2\2\4 \3\2\2\2\6/\3" +
                    "\2\2\2\b\61\3\2\2\2\n9\3\2\2\2\fC\3\2\2\2\16S\3\2\2\2\20\23\7\7\2\2\21" +
                    "\22\7\6\2\2\22\24\7\7\2\2\23\21\3\2\2\2\24\25\3\2\2\2\25\23\3\2\2\2\25" +
                    "\26\3\2\2\2\26\27\3\2\2\2\27\33\7\3\2\2\30\32\5\4\3\2\31\30\3\2\2\2\32" +
                    "\35\3\2\2\2\33\31\3\2\2\2\33\34\3\2\2\2\34\36\3\2\2\2\35\33\3\2\2\2\36" +
                    "\37\7\4\2\2\37\3\3\2\2\2 %\5\6\4\2!\"\7\5\2\2\"$\5\6\4\2#!\3\2\2\2$\'" +
                    "\3\2\2\2%#\3\2\2\2%&\3\2\2\2&\5\3\2\2\2\'%\3\2\2\2(\60\7\b\2\2)\60\7\t" +
                    "\2\2*\60\7\n\2\2+\60\7\13\2\2,\60\7\f\2\2-\60\5\b\5\2.\60\5\2\2\2/(\3" +
                    "\2\2\2/)\3\2\2\2/*\3\2\2\2/+\3\2\2\2/,\3\2\2\2/-\3\2\2\2/.\3\2\2\2\60" +
                    "\7\3\2\2\2\61\66\7\7\2\2\62\63\7\6\2\2\63\65\7\7\2\2\64\62\3\2\2\2\65" +
                    "8\3\2\2\2\66\64\3\2\2\2\66\67\3\2\2\2\67\t\3\2\2\28\66\3\2\2\29:\7\7\2" +
                    "\2:>\7\3\2\2;=\5\f\7\2<;\3\2\2\2=@\3\2\2\2><\3\2\2\2>?\3\2\2\2?A\3\2\2" +
                    "\2@>\3\2\2\2AB\7\4\2\2B\13\3\2\2\2CH\5\16\b\2DE\7\5\2\2EG\5\16\b\2FD\3" +
                    "\2\2\2GJ\3\2\2\2HF\3\2\2\2HI\3\2\2\2I\r\3\2\2\2JH\3\2\2\2KT\7\b\2\2LT" +
                    "\7\t\2\2MT\7\n\2\2NT\7\13\2\2OT\7\f\2\2PT\5\n\6\2QT\5\b\5\2RT\5\2\2\2" +
                    "SK\3\2\2\2SL\3\2\2\2SM\3\2\2\2SN\3\2\2\2SO\3\2\2\2SP\3\2\2\2SQ\3\2\2\2" +
                    "SR\3\2\2\2T\17\3\2\2\2\n\25\33%/\66>HS";
    public static final ATN _ATN =
            new ATNDeserializer().deserialize(_serializedATN.toCharArray());

    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}