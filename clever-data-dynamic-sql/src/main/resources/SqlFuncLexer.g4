lexer grammar SqlFuncLexer;

LPAREN:             '(';
RPAREN:             ')';
COMMA:              ',';
DOT:                '.';
//AT:                 '@';
//LBRACE:             '{';
//RBRACE:             '}';
//COMMA:              ',';

IDENTIFIER:         [a-zA-Z0-9$_]+;                         // 标识符规则(函数名、变量名)
NULL_LITERAL:       'null';                                 // null值规则
BOOL_LITERAL:       'true' | 'false';                       // bool值规则
DECIMAL_LITERAL:    '0' | [1-9] Digits?;                    // 整数数规则
FLOAT_LITERAL:      '-'? Digits ('.' Digits)?;              // 浮点数规则
STRING_LITERAL:     ('"' (~["\\\r\n])* '"');                // 字符串规则

fragment Digits:    [0-9]+;                                 // 数字

WS:                 [ \t\r\n\u000C]+ -> channel(HIDDEN);    // 忽略空白字符
