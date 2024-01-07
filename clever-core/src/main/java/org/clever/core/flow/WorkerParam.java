package org.clever.core.flow;

import lombok.EqualsAndHashCode;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务参数
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2024/01/07 20:38 <br/>
 */
@EqualsAndHashCode
public class WorkerParam {
    private final ConcurrentMap<String, Object> params = new ConcurrentHashMap<>();

//    set
//    get
}
