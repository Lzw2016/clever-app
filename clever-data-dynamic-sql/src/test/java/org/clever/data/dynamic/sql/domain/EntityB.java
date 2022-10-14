package org.clever.data.dynamic.sql.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/10 11:00 <br/>
 */
@Data
public class EntityB {
    private String a;
    private Integer b;
    private Double c;
    private BigDecimal d;
    private Boolean e;
    private List<String> f;
    private EntityA g;
}
