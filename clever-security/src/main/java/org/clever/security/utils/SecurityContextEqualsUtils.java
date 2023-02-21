package org.clever.security.utils;

import org.clever.core.mapper.JacksonMapper;
import org.clever.security.model.SecurityContext;
import org.clever.security.model.UserInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/12/16 17:07 <br/>
 */
public class SecurityContextEqualsUtils {
    public static boolean equals(SecurityContext a, SecurityContext b) {
        if (a == b) {
            return true;
        }
        if ((a == null) != (b == null)) {
            return false;
        }
        return Objects.equals(toString(a), toString(b));
    }

    protected static String toString(SecurityContext securityContext) {
        if (securityContext == null) {
            return "";
        }
        UserInfo userInfo = securityContext.getUserInfo();
        List<String> roles = new ArrayList<>(securityContext.getRoles());
        List<String> permissions = new ArrayList<>(securityContext.getPermissions());
        StringBuilder sb = new StringBuilder();
        sb.append(userInfo.getUserId());
        sb.append(userInfo.getUserName());
        sb.append(JacksonMapper.getInstance().toJson(userInfo.getExt()));
        Collections.sort(roles.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        Collections.sort(permissions.stream().filter(Objects::nonNull).collect(Collectors.toList()));
        roles.forEach(sb::append);
        permissions.forEach(sb::append);
        return sb.toString();
    }
}
