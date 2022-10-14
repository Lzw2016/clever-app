package org.clever.data.dynamic.sql.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/11/10 11:02 <br/>
 */
@Data
public class EntityMixin {
    private String a;
    private Integer b;
    private Double c;
    private BigDecimal d;
    private Boolean e;
    private List<Long> f;
    private EntityA g;
    private EntityB h;
    private List<EntityA> i;
    private Set<EntityB> j;
}

