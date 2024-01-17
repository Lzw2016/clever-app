package org.clever.js.api.require;

/**
 * CommonJS 接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/07/14 11:12 <br/>
 *
 * @param <T> script引擎对象类型
 */
@FunctionalInterface
public interface Require<T> {

    /**
     * CommonJS中的require函数
     *
     * @param id 模块ID(模块路径)
     */
    T require(final String id) throws Exception;
}

//require():                              加载外部模块
//(x)require.resolve()：                  将模块名解析到一个绝对路径
//(x)require.main：                       指向主模块
//(*)require.cache：                      指向所有缓存的模块
//(x)require.extensions：                 根据文件的后缀名，调用不同的执行函数
