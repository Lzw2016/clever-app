package org.clever.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 业务编码表(biz_code)
 */
@Data
public class BizCode implements Serializable {
    /** 主键id */
    private Long id;
    /** 编码名称 */
    private String codeName;
    /** 编码规则表达式 */
    private String pattern;
    /** 序列值 */
    private Long sequence;
    /** 重置sequence值的表达式，使用Java日期格式化字符串 */
    private String resetPattern;
    /** 重置sequence值标识，此字段值变化后则需要重置 */
    private String resetFlag;
    /** 说明 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
