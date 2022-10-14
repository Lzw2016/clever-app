parser grammar SqlFuncParser;

options { tokenVocab=SqlFuncLexer; }

// java语言函数
javaFunc
    : IDENTIFIER ('.' IDENTIFIER)+ '(' javaParameterList* ')'
    ;

javaParameterList
    : javaParameter (',' javaParameter)*
    ;

javaParameter
    : NULL_LITERAL                              // null值
    | BOOL_LITERAL                              // bool值
    | DECIMAL_LITERAL                           // 整数数
    | FLOAT_LITERAL                             // 浮点数
    | STRING_LITERAL                            // 字符串值
    | javaVar                                   // java变量(链式变量取值)
    | javaFunc                                  // java函数
    ;

javaVar
    : IDENTIFIER ('.' IDENTIFIER)*
    ;

// sql语言函数
sqlFunc
    : IDENTIFIER '(' sqlParameterList* ')'
    ;

sqlParameterList
    : sqlParameter (',' sqlParameter)*
    ;

sqlParameter
    : NULL_LITERAL                              // null值
    | BOOL_LITERAL                              // bool值
    | DECIMAL_LITERAL                           // 整数数
    | FLOAT_LITERAL                             // 浮点数
    | STRING_LITERAL                            // 字符串值
    | sqlFunc                                   // sql函数
    | javaVar                                   // java变量(链式变量取值)
    | javaFunc                                  // java函数
    ;

/*
1.javaFunc(IDENTIFIER, NULL_LITERAL, BOOL_LITERAL, CHAR_LITERAL, DECIMAL_LITERAL, STRING_LITERAL, javaFunc)
2.sqlFunc(IDENTIFIER, NULL_LITERAL, BOOL_LITERAL, CHAR_LITERAL, DECIMAL_LITERAL, STRING_LITERAL, javaFunc, sqlFunc)
*/