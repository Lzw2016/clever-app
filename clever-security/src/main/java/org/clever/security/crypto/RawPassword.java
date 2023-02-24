package org.clever.security.crypto;

import org.clever.core.Conv;

import java.util.Objects;

/**
 * 密码明文处理,不做任何加密解密
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/02/24 16:42 <br/>
 */
public class RawPassword implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return Conv.asString(rawPassword, null);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return Objects.equals(Conv.asString(rawPassword, null), encodedPassword);
    }
}
