package org.clever.core.model.entity;

import java.io.Serializable;

/**
 * 实体类接口<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-5-12 9:26 <br/>
 */
public interface BaseEntity extends Serializable {
    /**
     * 自身关联实体类的fullPath属性分隔标识
     */
    char FULL_PATH_SPLIT = '-';

    /**
     * 树结构对象 根节点父级编号
     */
    Long ROOT_PARENT_ID = -1L;
}
