package org.clever.web.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.config.JavalinConfig;
import io.javalin.config.SizeUnit;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.bundled.CorsPluginConfig;
import io.javalin.util.ConcurrencyUtil;
import org.clever.core.AppContextHolder;
import org.clever.core.Assert;
import org.clever.core.ResourcePathUtils;
import org.clever.core.json.jackson.JacksonConfig;
import org.clever.core.mapper.JacksonMapper;
import org.clever.web.JavalinAppDataKey;
import org.clever.web.config.HttpConfig;
import org.clever.web.config.WebConfig;
import org.clever.web.config.WebSocketConfig;
import org.springframework.util.unit.DataSize;

import java.util.Optional;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/10/03 16:14 <br/>
 */
public abstract class ApplyWebConfig {
    /**
     * 获取资源的绝对路径
     *
     * @param rootPath 根路径
     * @param filePath 文件路径
     * @param location 位置类型
     */
    public static String getAbsolutePath(String rootPath, String filePath, Location location) {
        if (Location.EXTERNAL.equals(location)) {
            filePath = ResourcePathUtils.getAbsolutePath(rootPath, filePath);
        }
        return filePath;
    }

    /**
     * 应用当前配置到 JavalinConfig
     */
    public static void applyConfig(final String rootPath, final WebConfig webConfig, final JavalinConfig config) {
        Assert.notNull(config, "参数 config 不能为空");
        final JacksonConfig jackson = Optional.ofNullable(webConfig.getJackson()).orElseGet(() -> {
            webConfig.setJackson(new JacksonConfig());
            return webConfig.getJackson();
        });
        final HttpConfig http = Optional.ofNullable(webConfig.getHttp()).orElseGet(() -> {
            webConfig.setHttp(new HttpConfig());
            return webConfig.getHttp();
        });
        final WebSocketConfig websocket = Optional.ofNullable(webConfig.getWebsocket()).orElseGet(() -> {
            webConfig.setWebsocket(new WebSocketConfig());
            return webConfig.getWebsocket();
        });
        config.jetty.defaultHost = webConfig.getHost();
        config.jetty.defaultPort = webConfig.getPort();
        config.showJavalinBanner = webConfig.isShowJavalinBanner();
        config.useVirtualThreads = webConfig.isUseVirtualThreads();
        config.jetty.threadPool = ConcurrencyUtil.jettyThreadPool(
            webConfig.getThreadPoolName(),
            webConfig.getThreadPoolMin(),
            webConfig.getThreadPoolMax(),
            webConfig.isUseVirtualThreads()
        );
        config.startupWatcherEnabled = false;
        // jackson
        ObjectMapper webServerMapper = JacksonMapper.newObjectMapper();
        jackson.apply(webServerMapper);
        JsonMapper jsonMapper = new JavalinJackson(webServerMapper, webConfig.isUseVirtualThreads());
        config.jsonMapper(jsonMapper);
        config.appData(JavalinAppDataKey.OBJECT_MAPPER_KEY, webServerMapper);
        config.appData(JavalinAppDataKey.JSON_MAPPER_KEY, jsonMapper);
        AppContextHolder.registerBean("javalinJsonMapper", jsonMapper, true);
        AppContextHolder.registerBean("javalinObjectMapper", webServerMapper, true);
        // http
        config.router.contextPath = http.getContextPath();
        config.router.ignoreTrailingSlashes = http.isIgnoreTrailingSlashes();
        config.router.treatMultipleSlashesAsSingleSlash = http.isTreatMultipleSlashesAsSingleSlash();
        config.router.caseInsensitiveRoutes = http.isCaseInsensitiveRoutes();
        config.http.generateEtags = http.isGenerateEtags();
        config.http.prefer405over404 = http.isPrefer405over404();
        config.http.defaultContentType = http.getDefaultContentType();
        if (http.getMaxRequestSize() != null) {
            config.http.maxRequestSize = http.getMaxRequestSize().toBytes();
        }
        if (http.getAsyncTimeout() != null) {
            config.http.asyncTimeout = http.getAsyncTimeout().toMillis();
        }
        if (http.getMultipart() != null) {
            HttpConfig.Multipart multipart = http.getMultipart();
            String location = multipart.getLocation();
            DataSize maxFileSize = multipart.getMaxFileSize();
            DataSize maxTotalRequestSize = multipart.getMaxTotalRequestSize();
            DataSize maxInMemoryFileSize = multipart.getMaxInMemoryFileSize();
            if (multipart.getLocation() != null) {
                config.jetty.multipartConfig.cacheDirectory(ResourcePathUtils.getAbsolutePath(rootPath, location));
            }
            if (maxFileSize != null) {
                config.jetty.multipartConfig.maxFileSize(maxFileSize.toBytes(), SizeUnit.BYTES);
            }
            if (maxTotalRequestSize != null) {
                config.jetty.multipartConfig.maxTotalRequestSize(maxTotalRequestSize.toBytes(), SizeUnit.BYTES);
            }
            if (maxInMemoryFileSize != null) {
                config.jetty.multipartConfig.maxInMemoryFileSize((int) maxInMemoryFileSize.toBytes(), SizeUnit.BYTES);
            }
        }
        if (http.getSinglePageRoot() != null) {
            for (HttpConfig.SinglePageRoot singlePageRoot : http.getSinglePageRoot()) {
                config.spaRoot.addFile(
                    singlePageRoot.getHostedPath(),
                    getAbsolutePath(rootPath, singlePageRoot.getFilePath(), singlePageRoot.getLocation()),
                    singlePageRoot.getLocation()
                );
            }
        }
        if (http.getStaticFile() != null) {
            for (HttpConfig.StaticFile staticFile : http.getStaticFile()) {
                config.staticFiles.add(staticFileConfig -> {
                    staticFileConfig.hostedPath = staticFile.getHostedPath();
                    staticFileConfig.directory = getAbsolutePath(rootPath, staticFile.getDirectory(), staticFile.getLocation());
                    staticFileConfig.location = staticFile.getLocation();
                    staticFileConfig.precompress = staticFile.isPreCompress();
                    staticFileConfig.headers = staticFile.getHeaders();
                });
            }
        }
        if (http.isEnableWebjars()) {
            config.staticFiles.enableWebjars();
        }
        // websocket
        // TODO 自定义 Jetty WebSocketServletFactory
        // config.wsFactoryConfig(factory -> {
        // });
        // 注册一个 WebSocket 日志记录器
        // config.wsLogger(ws -> {});
        // bundledPlugins
        if (http.isEnforceSsl()) {
            config.bundledPlugins.enableSslRedirects();
        }
        if (webConfig.isEnableDevLogging()) {
            config.bundledPlugins.enableDevLogging();
        }
        if (http.isEnableCorsForAllOrigins()) {
            config.bundledPlugins.enableCors(corsPluginConfig -> corsPluginConfig.addRule(CorsPluginConfig.CorsRule::anyHost));
        } else if (http.getEnableCorsForOrigin() != null && !http.getEnableCorsForOrigin().isEmpty()) {
            config.bundledPlugins.enableCors(corsPluginConfig -> {
                for (String host : http.getEnableCorsForOrigin()) {
                    corsPluginConfig.addRule(corsRule -> corsRule.allowHost(host));
                }
            });
        }
        if (webConfig.isEnableHttpAllowedMethodsOnRoutes()) {
            config.bundledPlugins.enableHttpAllowedMethodsOnRoutes();
        }
        if (webConfig.isEnableRedirectToLowercasePaths()) {
            config.bundledPlugins.enableRedirectToLowercasePaths();
        }
    }
}
