package org.clever.task.manage.model.request;

import lombok.Data;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 16:23 <br/>
 */
@Data
public class CartLineDataReq {
    /**
     * 命名空间
     */
    private String namespace;
    /**
     * 开始时间
     */
    private Date start;
    /**
     * 结束时间
     */
    private Date end;
    /**
     * 数据量限制
     */
    private Integer limit;
}
