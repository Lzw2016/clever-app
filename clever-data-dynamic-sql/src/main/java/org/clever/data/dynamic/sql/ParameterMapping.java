package org.clever.data.dynamic.sql;

import lombok.Data;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/10 16:02 <br/>
 */
@Data
public class ParameterMapping {
    /** 参数变量名 */
    private String property;
    /** 参数 javaType 属性 */
    private String javaType;
}
