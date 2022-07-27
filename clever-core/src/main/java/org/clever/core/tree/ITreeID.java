package org.clever.core.tree;

import java.io.Serializable;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/03/18 19:11 <br/>
 */
public interface ITreeID extends Serializable {
    /**
     * 节点ID(必须非空)
     */
    Object getId();

    /**
     * 父节点ID(根节点的ParentId等于null 或 空字符串)
     */
    Object getParentId();

    /**
     * 当前节点是否是根节点
     *
     * @return true:是根节点 false:不是根节点 null:未知
     */
    default Boolean isRoot() {
        return null;
    }
}
