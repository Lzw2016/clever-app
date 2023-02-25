package org.clever.security.impl.model;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/01/21 14:48 <br/>
 */
public interface EnumConstant {
    /**
     * 是否启用，0:禁用
     */
    int ENABLED_0 = 0;
    /**
     * 是否启用，1:启用
     */
    int ENABLED_1 = 1;

    /**
     * JWT-Token是否禁用，0:未禁用
     */
    int DISABLE_0 = 0;
    /**
     * JWT-Token是否禁用，1:已禁用
     */
    int DISABLE_1 = 1;

    /**
     * 用户登录类型 1: 职员登录
     */
    int LOGIN_USER_TYPE_1 = 1;

    /**
     * 登录目标类型 1: PC
     */
    int AGENT_TYPE_1 = 1;
    /**
     * 登录目标类型 2: PDA
     */
    int AGENT_TYPE_2 = 1;

    /**
     * 验证类型 1: 密码
     */
    int AUTH_TYPE_1 = 1;
    /**
     * 验证类型 2: 指纹
     */
    int AUTH_TYPE_2 = 2;
    /**
     * 验证类型 3: 终端IP
     */
    int AUTH_TYPE_3 = 3;

    /**
     * 资源类型 1: PC菜单
     */
    int RESOURCE_TYPE_1 = 1;
    /**
     * 资源类型 2: PC按钮
     */
    int RESOURCE_TYPE_2 = 2;
    /**
     * 资源类型 3: PDA菜单
     */
    int RESOURCE_TYPE_3 = 3;
    /**
     * 资源类型 4: PDA按钮
     */
    int RESOURCE_TYPE_4 = 4;
    /**
     * 资源类型 5: 岗位
     */
    int RESOURCE_TYPE_5 = 5;
    /**
     * 资源类型 6: 操作授权
     */
    int RESOURCE_TYPE_6 = 6;

    /**
     * token禁用原因，0:使用RefreshToken
     */
    int JWT_TOKEN_DISABLE_REASON_0 = 0;
    /**
     * token禁用原因，1:管理员手动禁用
     */
    int JWT_TOKEN_DISABLE_REASON_1 = 1;
    /**
     * token禁用原因，2:并发登录被挤下线
     */
    int JWT_TOKEN_DISABLE_REASON_2 = 2;
    /**
     * token禁用原因，3:用户主动登出
     */
    int JWT_TOKEN_DISABLE_REASON_3 = 3;
    /**
     * 刷新Token状态，0:无效(已使用)
     */
    int JWT_TOKEN_REFRESH_TOKEN_STATE_0 = 0;
    /**
     * 刷新Token状态，1:有效(未使用)
     */
    int JWT_TOKEN_REFRESH_TOKEN_STATE_1 = 1;

    /**
     * 登录状态，0:登录失败
     */
    int USER_LOGIN_LOG_LOGIN_STATE_0 = 0;
    /**
     * 登录状态，1:登录成功
     */
    int USER_LOGIN_LOG_LOGIN_STATE_1 = 1;

    /**
     * 数据删除标志，0:未删除
     */
    int LOGIN_FAILED_COUNT_DELETE_FLAG_0 = 0;
    /**
     * 数据删除标志，1:已删除
     */
    int LOGIN_FAILED_COUNT_DELETE_FLAG_1 = 1;

    /**
     * request Attribute SysUser
     */
    String REQUEST_ATTR_USER = "sys_user";
    /**
     * JWT 扩展数据 user_id
     */
    String JWT_EXT_USER_ID = "user_id";
}
