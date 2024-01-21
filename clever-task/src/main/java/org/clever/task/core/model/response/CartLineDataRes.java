package org.clever.task.core.model.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/12/17 16:23 <br/>
 */
@Data
public class CartLineDataRes {
    private List<CartLineItem> job = new ArrayList<>();

    @Data
    public static class CartLineItem {
        /** 时间 */
        private String time;
        /** job 运行总次数 */
        private Integer jobCount;
        /** job 运行错误次数 */
        private Integer jobErrCount;
        /** job 触发总次数 */
        private Integer triggerCount;

        public CartLineItem() {
        }

        public CartLineItem(String time, Integer jobCount, Integer jobErrCount, Integer triggerCount) {
            this.time = time;
            this.jobCount = jobCount;
            this.jobErrCount = jobErrCount;
            this.triggerCount = triggerCount;
        }
    }
}
