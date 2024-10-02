package org.clever.web;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.core.ResourcePathUtils;
import org.clever.core.StrFormatter;
import org.clever.web.config.MvcConfig;
import org.clever.web.filter.MvcFilter;
import org.clever.web.filter.MvcHandlerMethodFilter;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/02/24 15:18 <br/>
 */
@Slf4j
public class MvcBootstrap {
    public static MvcBootstrap create(String rootPath, MvcConfig mvcConfig) {
        MvcFilter mvcFilter = new MvcFilter(rootPath, mvcConfig);
        return new MvcBootstrap(mvcFilter);
    }

    public static MvcBootstrap create(String rootPath, Environment environment) {
        MvcConfig mvcConfig = Binder.get(environment).bind(MvcConfig.PREFIX, MvcConfig.class).orElseGet(MvcConfig::new);
        MvcConfig.TransactionalConfig defTransactional = Optional.ofNullable(mvcConfig.getDefTransactional()).orElse(new MvcConfig.TransactionalConfig());
        mvcConfig.setDefTransactional(defTransactional);
        MvcConfig.HotReload hotReload = Optional.ofNullable(mvcConfig.getHotReload()).orElse(new MvcConfig.HotReload());
        mvcConfig.setHotReload(hotReload);
        Map<String, String> locationMap = ResourcePathUtils.getAbsolutePath(rootPath, hotReload.getLocations());
        AppContextHolder.registerBean("mvcConfig", mvcConfig, true);
        List<String> logs = new ArrayList<>();
        logs.add("mvc: ");
        logs.add("  enable                    : " + mvcConfig.isEnable());
        logs.add("  path                      : " + mvcConfig.getPath());
        logs.add("  httpMethod                : " + StringUtils.join(mvcConfig.getHttpMethod(), " | "));
        logs.add("  allowPackages             : " + StringUtils.join(mvcConfig.getAllowPackages(), " | "));
        logs.add("  packageMapping            : " + mvcConfig.getPackageMapping());
        logs.add("  transactionalDefDatasource: " + StringUtils.join(mvcConfig.getTransactionalDefDatasource(), " | "));
        logs.add("  defTransactional: ");
        logs.add("    datasource              : " + StringUtils.join(defTransactional.getDatasource(), " | "));
        logs.add("    propagation             : " + defTransactional.getPropagation());
        logs.add("    isolation               : " + defTransactional.getIsolation());
        logs.add("    timeout                 : " + defTransactional.getTimeout() + "s");
        logs.add("    readOnly                : " + defTransactional.isReadOnly());
        logs.add("  hotReload: ");
        logs.add("    enable                  : " + hotReload.isEnable());
        logs.add("    watchFile               : " + ResourcePathUtils.getAbsolutePath(rootPath, hotReload.getWatchFile()));
        logs.add("    interval                : " + StrFormatter.toPlainString(hotReload.getInterval()));
        logs.add("    excludePackages         : " + StringUtils.join(hotReload.getExcludePackages(), " | "));
        logs.add("    excludeClasses          : " + StringUtils.join(hotReload.getExcludeClasses(), " | "));
        logs.add("    locations               : " + StringUtils.join(hotReload.getLocations().stream().map(locationMap::get).toArray(), " | "));
        if (mvcConfig.isEnable()) {
            BannerUtils.printConfig(log, "mvc配置", logs.toArray(new String[0]));
        }
        return create(rootPath, mvcConfig);
    }

    @Getter
    private final MvcFilter mvcFilter;
    @Getter
    private final MvcHandlerMethodFilter mvcHandlerMethodFilter;

    public MvcBootstrap(MvcFilter mvcFilter) {
        this.mvcFilter = mvcFilter;
        this.mvcHandlerMethodFilter = mvcFilter.createMvcHandlerMethodFilter();
    }
}
