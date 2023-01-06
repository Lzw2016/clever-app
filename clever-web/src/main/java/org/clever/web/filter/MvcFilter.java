package org.clever.web.filter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.core.AppContextHolder;
import org.clever.core.BannerUtils;
import org.clever.core.ResourcePathUtils;
import org.clever.core.env.Environment;
import org.clever.util.Assert;
import org.clever.web.FilterRegistrar;
import org.clever.web.config.MvcConfig;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/01/06 13:29 <br/>
 */
@Slf4j
public class MvcFilter implements FilterRegistrar.FilterFuc {
    public static MvcFilter create(String rootPath, MvcConfig mvcConfig) {
        return new MvcFilter(rootPath, mvcConfig);
    }

    public static MvcFilter create(String rootPath, Environment environment) {
        MvcConfig mvcConfig = Binder.get(environment).bind(MvcConfig.PREFIX, MvcConfig.class).orElseGet(MvcConfig::new);
        fixedLocations(rootPath, mvcConfig);
        MvcConfig.HotReload hotReload = Optional.of(mvcConfig.getHotReload()).orElse(new MvcConfig.HotReload());
        mvcConfig.setHotReload(hotReload);
        Map<String, String> locationMap = fixedLocations(rootPath, mvcConfig);
        AppContextHolder.registerBean("mvcConfig", mvcConfig, true);
        List<String> logs = new ArrayList<>();
        logs.add("mvc: ");
        logs.add("  path             : " + mvcConfig.getPath());
        logs.add("  httpMethod       : " + StringUtils.join(mvcConfig.getHttpMethod(), " | "));
        logs.add("  allowPackages    : " + StringUtils.join(mvcConfig.getAllowPackages(), " | "));
        logs.add("  hotReload: ");
        logs.add("    enable         : " + hotReload.isEnable());
        logs.add("    interval       : " + hotReload.getInterval().toMillis() + "ms");
        logs.add("    excludePackages: " + StringUtils.join(hotReload.getExcludePackages(), " | "));
        logs.add("    locations      : " + StringUtils.join(hotReload.getLocations().stream().map(locationMap::get).toArray(), " | "));
        BannerUtils.printConfig(log, "mvc配置", logs.toArray(new String[0]));
        return create(rootPath, mvcConfig);
    }

    private static Map<String, String> fixedLocations(String rootPath, MvcConfig mvcConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为 null");
        Map<String, String> locationMap = new LinkedHashMap<>();
        MvcConfig.HotReload hotReload = mvcConfig.getHotReload();
        if (hotReload != null && hotReload.getLocations() != null) {
            for (String location : hotReload.getLocations()) {
                locationMap.put(location, ResourcePathUtils.getAbsolutePath(rootPath, location));
            }
        }
        return locationMap;
    }

    // 保存 JavalinConfig.inner.appAttributes
//    private Map<String, Object> appAttributes = Collections.emptyMap();
    private final MvcConfig mvcConfig;
    private final Map<String, String> locationMap;

    public MvcFilter(String rootPath, MvcConfig mvcConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(mvcConfig, "参数 mvcConfig 不能为 null");
        locationMap = Collections.unmodifiableMap(fixedLocations(rootPath, mvcConfig));
        this.mvcConfig = mvcConfig;
    }

    @Override
    public void doFilter(FilterRegistrar.Context ctx) throws IOException, ServletException {


        // 1.获取 HandlerMethod
        // 2.解析 HandlerMethod args
        // 3.获取 HandlerContext
        // 4.执行 HandlerInterceptor
        // 5.响应


//        Context context = new Context(ctx.req, ctx.res, appAttributes);
//        // 参考 io.javalin.http.util.ContextUtil#update
//        // String requestUri = StringUtils.removeStart(ctx.req.getRequestURI(), ctx.req.getContextPath());
//        ReflectionsUtils.setFieldValue(ctx, "matchedPath", pathSpec);
//        // ReflectionsUtils.setFieldValue(ctx, "pathParamMap", Collections.emptyMap());
//        ReflectionsUtils.setFieldValue(ctx, "handlerType", HandlerType.Companion.fromServletRequest(ctx.req));
//        ReflectionsUtils.setFieldValue(ctx, "endpointHandlerPath", pathSpec);
        ctx.next();
    }
}
