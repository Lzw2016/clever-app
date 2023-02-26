package org.clever.security.model.jackson2;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.clever.security.model.SecurityContext;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/12/19 13:44 <br/>
 */
public class SecurityJackson2Module extends SimpleModule {
    public static final SecurityJackson2Module INSTANCE = new SecurityJackson2Module();

    public SecurityJackson2Module() {
        super(SecurityJackson2Module.class.getName(), new Version(1, 0, 0, null, null, null));
    }

    @Override
    public void setupModule(SetupContext context) {
        context.setMixInAnnotations(SecurityContext.class, SecurityContextMixin.class);
    }
}
