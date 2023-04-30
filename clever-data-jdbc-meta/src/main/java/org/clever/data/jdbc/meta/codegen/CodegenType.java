package org.clever.data.jdbc.meta.codegen;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 12:09 <br/>
 */
public enum CodegenType {
    /** java 实体 */
    JAVA_ENTITY,
    /** groovy 实体 */
    GROOVY_ENTITY,
    /** kotlin 实体 */
    KOTLIN_ENTITY,

    /** java queryDSL */
    JAVA_QUERYDSL,
    /** groovy queryDSL */
    GROOVY_QUERYDSL,
    /** kotlin queryDSL */
    KOTLIN_QUERYDSL,

    /** 数据库文档 markdown 格式 */
    DB_DOC_MARKDOWN,
    /** 数据库文档 html 格式 */
    DB_DOC_HTML,
    /** 数据库文档 word 格式 */
    DB_DOC_WORD,
}
