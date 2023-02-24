import lombok.extern.slf4j.Slf4j;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.core.env.Environment;
import org.clever.security.config.SecurityConfig;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/24 10:59 <br/>
 */
@Slf4j
public class SecurityBootstrap {
    public static SecurityBootstrap create(Environment environment) {
        SecurityConfig securityConfig = Binder.get(environment).bind(SecurityConfig.PREFIX, SecurityConfig.class).orElseGet(SecurityConfig::new);
        AppContextHolder.registerBean("securityConfig", securityConfig, true);
        List<String> logs = new ArrayList<>();


        BannerUtils.printConfig(log, "mvc配置", logs.toArray(new String[0]));
        return null;
    }

    private final SecurityConfig securityConfig;

    public SecurityBootstrap(SecurityConfig securityConfig) {
        Assert.notNull(securityConfig, "参数 securityConfig 不能为 null");
        this.securityConfig = securityConfig;
    }
}
