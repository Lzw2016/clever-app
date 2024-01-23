package org.clever.core.tree;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/23 14:29 <br/>
 */
@Slf4j
public class TreeUtilsTest {
    @Test
    public void t01() {
        Node root = new Node("0", null);
        List<?> list = TreeUtils.buildTree(Collections.singletonList(root));
        log.info("list -> {}", list);
    }
}

@Data
class Node implements ITreeNode {
    private boolean build;
    private final String id;
    private final String parentId;
    private final List<ITreeNode> children = new ArrayList<>();

    public Node(String id, String parentId) {
        this.id = id;
        this.parentId = parentId;
    }

    @Override
    public Object getId() {
        return id;
    }

    @Override
    public Object getParentId() {
        return parentId;
    }

    @Override
    public boolean isBuild() {
        return build;
    }

    @Override
    public void setBuild(boolean isBuild) {
        build = isBuild;
    }

    @Override
    public List<? extends ITreeNode> getChildren() {
        return children;
    }

    @Override
    public void addChildren(ITreeNode node) {
        children.add(node);
    }
}
