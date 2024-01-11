package org.clever.core.tree;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.tuples.TupleOne;
import org.clever.util.Assert;

import java.util.*;

/**
 * 构建对象树结构的工具类<br/>
 * <p/>
 * 作者：LiZW <br/>
 * 创建时间：2016-5-8 22:05 <br/>
 */
@Slf4j
public class TreeUtils {
    /**
     * 构建树结构，可能有多棵树<br/>
     *
     * @param nodes 所有要构建树的节点
     * @return 构建的所有树的根节点
     */
    public static <T extends ITreeNode> List<T> buildTree(Collection<T> nodes) {
        log.debug("开始构建树结构...");
        final long startTime = System.currentTimeMillis();
        // 需要构建树的节点，还未构建到树中的节点
        List<T> allTreeNodeList = getCanBuildTreeNodes(nodes);
        // 清除构建状态
        clearBuild(allTreeNodeList);
        // 查找所有根节点
        List<T> rootNodeList = findRootNode(allTreeNodeList);
        // 刷新还未构建到树中的节点，减少循环次数
        List<T> noBuildTreeNodeList = refreshNoBuildNodes(allTreeNodeList);
        // 循环根节点，构建多棵树
        buildTree(rootNodeList, noBuildTreeNodeList);
        // 刷新还未构建到树中的节点，减少循环次数
        noBuildTreeNodeList = refreshNoBuildNodes(noBuildTreeNodeList);
        final long endTime = System.currentTimeMillis();
        // 校验构建是否正确
        if (noBuildTreeNodeList.isEmpty()) {
            log.debug("树构建成功！耗时：{}ms | 数据量：{}", (endTime - startTime), nodes.size());
        } else {
            log.error("树构建失败！耗时：{}ms | [{}]", (endTime - startTime), nodesToString(noBuildTreeNodeList));
        }
        return rootNodeList;
    }

    private static <T extends ITreeNode> String nodesToString(Collection<T> nodes) {
        StringBuilder sb = new StringBuilder();
        for (ITreeNode treeNode : nodes) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(treeNode.getId());
        }
        return sb.toString();
    }

    /**
     * 刷新还未构建到树中的节点<br/>
     *
     * @param noBuildTreeNodeList 还未构建到树中的节点集合
     * @return 刷新后的还未构建到树中的节点集合
     */
    private static <T extends ITreeNode> List<T> refreshNoBuildNodes(List<T> noBuildTreeNodeList) {
        List<T> newNoBuildTreeNodeList = new ArrayList<>();
        for (T node : noBuildTreeNodeList) {
            if (!node.isBuild()) {
                newNoBuildTreeNodeList.add(node);
            }
        }
        return newNoBuildTreeNodeList;
    }

    /**
     * 生成树(一层一层的查找子节点)<br/>
     *
     * @param parentNodeList      父节点集合
     * @param noBuildTreeNodeList 所有未被添加到父节点下的节点
     */
    private static <T extends ITreeNode> void buildTree(List<T> parentNodeList, List<T> noBuildTreeNodeList) {
        while (true) {
            // 下一次遍历的父节点
            List<T> nextParentNodeList = new ArrayList<>();
            for (T childNode : noBuildTreeNodeList) {
                for (T parentNode : parentNodeList) {
                    if (!childNode.isBuild() && Objects.equals(childNode.getParentId(), parentNode.getId())) {
                        // 设置已经被添加到父节点下了
                        childNode.setBuild(true);
                        parentNode.addChildren(childNode);
                        // 下一次遍历的父节点-增加
                        nextParentNodeList.add(childNode);
                    }
                }
            }
            // 没有找到下一级节点
            if (nextParentNodeList.isEmpty()) {
                break;
            }
            // 父节点集合
            parentNodeList = nextParentNodeList;
            // 踢除已经构建好的节点
            List<T> nextNoBuildTreeNodeList = refreshNoBuildNodes(noBuildTreeNodeList);
            // 消除死循环
            if (!noBuildTreeNodeList.isEmpty()) {
                Assert.isTrue(
                    nextNoBuildTreeNodeList.size() < noBuildTreeNodeList.size(),
                    "可能出现死循环，ITreeNode.setBuild函数实现错误"
                );
            }
            noBuildTreeNodeList = nextNoBuildTreeNodeList;
            // 没有未构建的节点了
            if (noBuildTreeNodeList.isEmpty()) {
                break;
            }
        }
    }

    /**
     * 过滤节点对象，排除不能构建树的节点，不能构建树的节点满足以下条件：<br/>
     * 1.节点对象为null (node == null)<br/>
     * 2.节点ID为null (node.getId() == null)<br/>
     *
     * @param nodes 所有要构建树的节点
     * @return 所有可以构建树的节点，即节点数据验证通过的节点
     */
    private static <T extends ITreeNode> List<T> getCanBuildTreeNodes(Collection<T> nodes) {
        List<T> treeNodeList = new ArrayList<>();
        nodes.forEach(node -> {
            if (node != null && node.getId() != null) {
                treeNodeList.add(node);
            }
        });
        return treeNodeList;
    }

    /**
     * 清除节点的构建状态，以用于重新构建树<br/>
     *
     * @param noBuildTreeNodeList 所有要构建树的节点
     */
    private static <T extends ITreeNode> void clearBuild(List<T> noBuildTreeNodeList) {
        for (T node : noBuildTreeNodeList) {
            node.setBuild(false);
        }
    }

    /**
     * 在节点中查找所有根节点，根节点满足以下条件：<br/>
     * 1.节点的父节点ID等于 null 或 空字符串<br/>
     * 2.在节点集合中找不到某个节点的父节点，那么这个节点就是根节点<br/>
     *
     * @param noBuildTreeNodeList 所有要构建树的节点
     * @return 所有根节点
     */
    private static <T extends ITreeNode> List<T> findRootNode(List<T> noBuildTreeNodeList) {
        // 所有根节点
        List<T> rootNodeList = new ArrayList<>();
        for (T root : noBuildTreeNodeList) {
            // 节点的父节点ID等于-1
            if (root.getParentId() == null || StringUtils.isBlank(String.valueOf(root.getParentId()))) {
                rootNodeList.add(root);
                root.setBuild(true);
                continue;
            }
            Boolean isRoot = root.isRoot();
            if (Objects.equals(isRoot, true)) {
                rootNodeList.add(root);
                root.setBuild(true);
                continue;
            }
            if (Objects.equals(isRoot, false)) {
                continue;
            }
            // 在节点集合中找不到某个节点的父节点，那么这个节点就是根节点
            if (noBuildTreeNodeList.stream().noneMatch(children -> !root.equals(children) && Objects.equals(root.getParentId(), children.getId()))) {
                rootNodeList.add(root);
                root.setBuild(true);
            }
        }
        return rootNodeList;
    }

    /**
     * 查找指定节点
     *
     * @param treeList 已经构建好的树
     * @param id       查找的节点ID
     * @param <T>      节点数据类型
     * @return 如未找到返回null
     */
    @SuppressWarnings("unchecked")
    public static <T extends ITreeNode> T findNode(List<T> treeList, Object id) {
        List<T> currentLevel = treeList;
        List<T> nextLevel;
        T match = null;
        while (currentLevel != null && !currentLevel.isEmpty()) {
            nextLevel = new ArrayList<>();
            for (T treeNode : currentLevel) {
                // id 一致找到了
                if (Objects.equals(id, treeNode.getId())) {
                    match = treeNode;
                    break;
                }
                // 加入下一层节点
                if (treeNode.getChildren() != null && !treeNode.getChildren().isEmpty()) {
                    nextLevel.addAll((List<T>) treeNode.getChildren());
                }
            }
            if (match != null) {
                break;
            }
            currentLevel = nextLevel;
        }
        return match;
    }

    /**
     * 查找指定节点
     *
     * @param node 已经构建好的树节点
     * @param id   查找的节点ID
     * @param <T>  节点数据类型
     * @return 如未找到返回null
     */
    public static <T extends ITreeNode> T findNode(T node, Object id) {
        return findNode(Collections.singletonList(node), id);
    }

    /**
     * 判断树中是否存在指定的节点
     *
     * @param treeList 已经构建好的树
     * @param id       查找的节点ID
     * @param <T>      节点数据类型
     * @return 如未找到返回null
     */
    public static <T extends ITreeNode> boolean existsNode(List<T> treeList, Object id) {
        return findNode(treeList, id) != null;
    }

    /**
     * 判断树中是否存在指定的节点
     *
     * @param node 已经构建好的树节点
     * @param id   查找的节点ID
     * @param <T>  节点数据类型
     * @return 如未找到返回null
     */
    public static <T extends ITreeNode> boolean existsNode(T node, Object id) {
        return findNode(Collections.singletonList(node), id) != null;
    }

    /**
     * 判断当前节点是否是叶子节点
     */
    public static <T extends ITreeNode> boolean isLeaf(T node) {
        Assert.notNull(node, "参数 node 不能为 null");
        return node.getChildren() == null || node.getChildren().isEmpty();
    }

    /**
     * 平铺树
     *
     * @param treeList 已经构建好的树
     * @param <T>      节点数据类型
     * @return 包含所有节点的集合
     */
    @SuppressWarnings("unchecked")
    public static <T extends ITreeNode> List<T> flattenTree(List<T> treeList) {
        List<T> currentLevel = treeList;
        List<T> nextLevel;
        List<T> flattenNode = new ArrayList<>();
        while (currentLevel != null && !currentLevel.isEmpty()) {
            flattenNode.addAll(currentLevel);
            nextLevel = new ArrayList<>();
            for (T treeNode : currentLevel) {
                if (!isLeaf(treeNode)) {
                    nextLevel.addAll((List<T>) treeNode.getChildren());
                }
            }
            currentLevel = nextLevel;
        }
        return flattenNode;
    }

    /**
     * 平铺树节点
     *
     * @param node 已经构建好的树的一个节点
     * @param <T>  节点数据类型
     * @return 包含所有节点的集合
     */
    public static <T extends ITreeNode> List<T> flattenNode(T node) {
        return flattenTree(Collections.singletonList(node));
    }

    /**
     * 获取指定节点的父节点
     *
     * @param treeList 已经构建好的树
     * @param id       查找的节点ID
     * @return 指定节点的父节点，不存在返回null
     */
    public static <T extends ITreeNode> T getParent(List<T> treeList, Object id) {
        List<T> nodes = flattenTree(treeList);
        T node = nodes.stream().filter(n -> Objects.equals(n.getId(), id)).findFirst().orElse(null);
        if (node == null || Objects.equals(node.isRoot(), true)) {
            return null;
        }
        return nodes.stream().filter(n -> Objects.equals(n.getId(), node.getParentId())).findFirst().orElse(null);
    }

    /**
     * 获取指定节点的父节点
     *
     * @param treeList 已经构建好的树
     * @param node     指定的节点
     * @return 指定节点的父节点，不存在返回null
     */
    public static <T extends ITreeNode> T getParent(List<T> treeList, T node) {
        return getParent(treeList, node.getId());
    }

    /**
     * 获取指定节点的所有父节点(按照层级结构排序，直接父节点在集合的第一个位置)
     *
     * @param treeList 已经构建好的树
     * @param id       查找的节点ID
     * @return 指定节点的所有父节点，不存在返回空集合
     */
    public static <T extends ITreeNode> List<T> getParents(List<T> treeList, Object id) {
        final List<T> parents = new ArrayList<>();
        final List<T> nodes = flattenTree(treeList);
        final T node = nodes.stream().filter(n -> Objects.equals(n.getId(), id)).findFirst().orElse(null);
        if (node == null || Objects.equals(node.isRoot(), true)) {
            return parents;
        }
        final TupleOne<Object> parentId = TupleOne.creat(node.getParentId());
        final Set<Object> parentIds = new HashSet<>();
        while (true) {
            T parentNode = nodes.stream().filter(n -> Objects.equals(n.getId(), parentId.getValue1())).findFirst().orElse(null);
            // 父节点必须存在
            if (parentNode == null) {
                break;
            }
            // 防止死循环
            if (!parentIds.add(parentNode.getId())) {
                break;
            }
            parents.add(parentNode);
            parentId.setValue1(parentNode.getParentId());
            // 父节点是根节点
            if (Objects.equals(parentNode.isRoot(), true)) {
                break;
            }
        }
        return parents;
    }

    /**
     * 获取指定节点的所有父节点(按照层级结构排序，直接父节点在集合的第一个位置)
     *
     * @param treeList 已经构建好的树
     * @param node     指定的节点
     * @return 指定节点的所有父节点，不存在返回空集合
     */
    public static <T extends ITreeNode> List<T> getParents(List<T> treeList, T node) {
        return getParents(treeList, node.getId());
    }

    /**
     * 把指定的节点当作一棵树的根节点，获取这棵树的所有的叶子节点
     *
     * @param nodes 指定的节点集合
     */
    @SuppressWarnings("unchecked")
    public static <T extends ITreeNode> List<T> getLeafs(List<T> nodes) {
        List<T> currentLevel = nodes;
        List<T> nextLevel;
        List<T> leafsNode = new ArrayList<>();
        while (currentLevel != null && !currentLevel.isEmpty()) {
            nextLevel = new ArrayList<>();
            for (T treeNode : currentLevel) {
                if (isLeaf(treeNode)) {
                    leafsNode.add(treeNode);
                } else {
                    nextLevel.addAll((List<T>) treeNode.getChildren());
                }
            }
            currentLevel = nextLevel;
        }
        return leafsNode;
    }

    /**
     * 把指定的节点当作一棵树的根节点，获取这棵树的所有的叶子节点
     *
     * @param node 指定的节点
     */
    public static <T extends ITreeNode> List<T> getLeafs(T node) {
        return getLeafs(Collections.singletonList(node));
    }
}
