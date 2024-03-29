package org.clever.core.tree;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 树节点包装器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2021/03/18 19:08 <br/>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class TreeNodeWrapper<T extends ITreeID> implements ITreeNode {
    /**
     * 是否被添加到父节点下
     */
    private boolean isBuild = false;
    /**
     * 子节点
     */
    private List<ITreeNode> children;

    /**
     * 绑定到节点的对象
     */
    @JsonUnwrapped
    private T node;

    public TreeNodeWrapper() {
    }

    public TreeNodeWrapper(T node) {
        this.node = node;
    }

    @Override
    public Object getId() {
        return node.getId();
    }

    @Override
    public Object getParentId() {
        return node.getParentId();
    }

    @Override
    public boolean isBuild() {
        return isBuild;
    }

    @Override
    public void setBuild(boolean isBuild) {
        this.isBuild = isBuild;
    }

    @Override
    public List<? extends ITreeNode> getChildren() {
        return children;
    }

    @Override
    public void addChildren(ITreeNode node) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(node);
    }

    /**
     * 增加子节点<br>
     */
    public TreeNodeWrapper<?> addChildren(TreeNodeWrapper<?> node) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(node);
        return this;
    }

    /**
     * 增加子节点集合<br>
     */
    public TreeNodeWrapper<?> addChildren(Collection<? extends TreeNodeWrapper<?>> nodes) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.addAll(nodes);
        return this;
    }
}
