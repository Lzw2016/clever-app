package org.clever.task.core.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 16:23 <br/>
 */
@Data
public class CartLineDataRes {
    private List<CartLineItem> job = new ArrayList<>();

    private List<CartLineItem> trigger = new ArrayList<>();

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class CartLineItem {
        private String time;
        private Integer count;
    }
}
