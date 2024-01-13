package org.clever.data.jdbc.entity;

import lombok.Data;

import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/10 14:32 <br/>
 */
@Data
public class EntityData {
    private Long id;
    private String a;
    private String b;
    private String c;
    private String d;
    private Long lock_count;
    private String description;
    private Date createAt;
    private Date updateAt;
}
