package org.clever.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 自增长id表(sys_lock)
 */
@Data
public class SysLock implements Serializable {
    /** 主键id */
    private Long id;
    /** 锁名称 */
    private String lockName;
    /** 锁次数 */
    private Long lockCount;
    /** 说明 */
    private String description;
    /** 创建时间 */
    private Date createAt;
    /** 更新时间 */
    private Date updateAt;
}
