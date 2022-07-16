package org.clever.dao;

/**
 * 当基础资源拒绝访问特定元素（如特定数据库表）的权限时引发异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:12 <br/>
 */
public class PermissionDeniedDataAccessException extends NonTransientDataAccessException {
    public PermissionDeniedDataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
