package org.clever.model.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * (test)
 */
@Data
public class Test implements Serializable {
    /**  */
    private Long id;
    /**  */
    private String a;
    /**  */
    private String b;
    /**  */
    private String c;
    /**  */
    private String d;
    /**  */
    private Long lockCount;
    /**  */
    private String description;
    /**  */
    private Date createAt;
    /**  */
    private Date updateAt;
}
