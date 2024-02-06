package org.clever.js.api;

import java.util.Collection;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/16 09:59 <br/>
 *
 * @param <T> script引擎对象类型
 */
public interface ScriptObject<T> {
    /**
     * 获取原始script引擎对象
     */
    T originalValue();

    /**
     * 获取script对象所有成员名称
     */
    Collection<String> getMemberNames();

    /**
     * 获取script对象所有成员值
     */
    Collection<Object> getMembers();

    /**
     * 是否存在成员
     *
     * @param name 成员名称
     */
    boolean hasMember(String name);

    /**
     * 获取script对象成员值
     *
     * @param name 成员名称
     */
    Object getMember(String name);

    /**
     * 设置script对象成员值
     *
     * @param name  成员名称
     * @param value 成员属值
     */
    void setMember(String name, Object value);

    /**
     * 删除script对象成员属性
     *
     * @param name 成员名称
     */
    void delMember(String name);

    /**
     * 调用成员函数
     *
     * @param functionName 成员函数名称
     * @param args         参数
     */
    Object callMember(String functionName, Object... args);

    /**
     * 调用成员函数
     *
     * @param functionName 成员函数名称
     * @param args         参数
     */
    default void callMemberVoid(String functionName, Object... args) {
        callMember(functionName, args);
    }

    /**
     * 判断当前脚本对象能否执行
     */
    boolean canExecute();

    /**
     * 执行当前脚本对象
     *
     * @param args 参数
     */
    Object execute(Object... args);

    /**
     * 执行当前脚本对象
     *
     * @param args 参数
     */
    default void executeVoid(Object... args) {
        execute(args);
    }

    /**
     * script对象集合大小或者属性成员数量
     */
    int size();
}
