package org.clever.data.jdbc.support.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 自增长id表(auto_increment_id)
 */
@Data
public class AutoIncrementId implements Serializable {
    /** 主键id */
    private Long id;
    /** 序列名称 */
    private String sequenceName;
    /** 当前值 */
    private Long currentValue;
    /** 说明 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
