package org.clever.security.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import lombok.Data;
import org.clever.core.mapper.BeanMapper;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户信息(从数据库或其它服务加载)
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2020/11/29 19:27 <br/>
 */
@Data
public class UserInfo implements Serializable {
    /**
     * 用户id
     */
    private String userId;
    /**
     * 用户姓名/昵称
     */
    private String userName;
    /**
     * 用户扩展信息
     */
    private final Map<String, Object> ext = new LinkedHashMap<>();

    public UserInfo() {
    }

    public UserInfo copy() {
        UserInfo userInfo = new UserInfo();
        BeanMapper.copyTo(this, userInfo);
        userInfo.getExt().putAll(this.getExt());
        return userInfo;
    }

    @JsonAnyGetter
    public Map<String, Object> getExt() {
        return ext;
    }
}
