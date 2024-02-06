package org.clever.js.api.folder;

import java.util.List;

/**
 * 文件或文件夹对象
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/14 11:08 <br/>
 */
public interface Folder {
    /**
     * 统一使用的路径分隔符
     */
    String PATH_SEPARATE = "/";
    /**
     * 根路径字符串
     */
    String ROOT_PATH = "/";
    /**
     * 上级目录
     */
    String PARENT_PATH = "../";
    /**
     * 当前目录
     */
    String CURRENT_PATH = "./";
    /**
     * 当前位置
     */
    String CURRENT = ".";

    /**
     * 获取根路径
     */
    Folder getRoot();

    /**
     * 获取父路径
     * <pre>
     *  "/"           -> null
     *  "/a"          -> "/"
     *  "/a/file.js"  -> "/a"
     * </pre>
     */
    Folder getParent();

    /**
     * 返回当前路径
     * <pre>
     *  "/"           -> ""
     *  "/a"          -> "a"
     *  "/a/file.js"  -> "file.js"
     *  "/a/b/"       -> "b"
     * <pre>
     */
    String getName();

    /**
     * 返回逻辑绝对路径(结束字符不是路径分隔符，除了根路径)
     * <pre>
     *  "/"           -> "/"
     *  "/a"          -> "/a"
     *  "/a/file.js"  -> "/a/file.js"
     *  "/a/b/"       -> "/a/b"
     * <pre>
     */
    String getFullPath();

    /**
     * 获取物理绝对路径
     */
    String getAbsolutePath();

    /**
     * 当前路径是否存在
     */
    boolean exists();

    /**
     * 当前路径是否是文件
     * <pre>
     *  不存在    -> false
     *  文件夹    -> false
     *  是文件    -> true
     * </pre>
     */
    boolean isFile();

    /**
     * 当前路径是否是文件夹
     * <pre>
     *  不存在    -> false
     *  文件夹    -> true
     *  是文件    -> false
     * </pre>
     *
     * @return true: 文件；false: 文件夹
     */
    boolean isDir();

    /**
     * 得到当前文件夹下的文件内容<br/>
     * <pre>
     *  不存在    -> null
     *  文件夹    -> null
     *  是文件    -> FileContent
     * </pre>
     *
     * @param name 文件名称
     * @return 文件不存在返回null
     */
    String getFileContent(String name);

    /**
     * 得到当前文件的内容<br/>
     * <pre>
     *  不存在    -> null
     *  文件夹    -> null
     *  是文件    -> FileContent
     * </pre>
     *
     * @return 文件不存在返回null
     */
    String getFileContent();

    /**
     * 获取子路径列表
     * <pre>
     *  不存在    -> null
     *  文件夹    -> List<Path>
     *  是文件    -> null
     * </pre>
     */
    List<Folder> getChildren();

    /**
     * 连接路径，可能返回 null
     * <pre>
     *  /foo/ + bar          -->   /foo/bar
     *  /foo + bar           -->   /foo/bar
     *  /foo + /bar          -->   /bar
     *  /foo/a/ + ../bar     -->   foo/bar
     *  /foo/ + ../../bar    -->   null
     *  /foo/ + /bar         -->   /bar
     *  /foo/.. + /bar       -->   /bar
     *  /foo + bar/c.txt     -->   /foo/bar/c.txt
     *  /foo/c.txt + bar     -->   /foo/c.txt/bar
     *  /foo/bbb + /         -->   /
     * </pre>
     *
     * @param paths 子路径
     */
    Folder concat(String... paths);

    /**
     * 创建Path对象(拥有相同的基础路径)
     *
     * @param path 子路径
     */
    Folder create(String path);
}
