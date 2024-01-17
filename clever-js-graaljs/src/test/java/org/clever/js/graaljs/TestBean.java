package org.clever.js.graaljs;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/30 21:30 <br/>
 */
@Data
public class TestBean implements Serializable {
    private Integer a;
    private String b;
    private Boolean c;
    private Double d;
    private Date e;
}
