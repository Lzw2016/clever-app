package org.clever.boot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 16:54 <br/>
 */
@Slf4j
public class BeanUtilsTest {
    @Test
    public void t01() {
        BeanA beanA = new BeanA('a', true, (byte) 2, (short) 4, 6, 8, 10.1F, 12.2D, "abc", new Date(), new BigDecimal("123.456"));
        BeanB beanB = new BeanB();
        BeanUtils.copyProperties(beanA, beanB);
        log.info("--> {}", beanB);
    }
}

@AllArgsConstructor
@NoArgsConstructor
@Data
class BeanA {
    private char /*         */ f1;
    private boolean /*      */ f2;
    private byte /*         */ f3;
    private short /*        */ f4;
    private int /*          */ f5;
    private long /*         */ f6;
    private float /*        */ f7;
    private double /*       */ f8;
    private String /*       */ f9;
    private Date /*         */ f10;
    private BigDecimal /*   */ f11;
}

@AllArgsConstructor
@NoArgsConstructor
@Data
class BeanB {
    private Character /*    */ f1;
    private Boolean /*      */ f2;
    private Byte /*         */ f3;
    private Short /*        */ f4;
    private Integer /*      */ f5;
    private Long /*         */ f6;
    private Float /*        */ f7;
    private Double /*       */ f8;
    private String /*       */ f9;
    private Date /*         */ f10;
    private BigDecimal /*   */ f11;
}
