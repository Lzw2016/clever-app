package org.clever.core.model.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 响应基础类
 * <p>
 * 作者：lzw <br/>
 * 创建时间：2017-09-01 23:23 <br/>
 */
@Data
public class BaseResponse implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
