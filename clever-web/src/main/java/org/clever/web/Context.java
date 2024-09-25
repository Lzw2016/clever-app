package org.clever.web;

import io.javalin.config.JavalinConfig;
import io.javalin.config.Key;
import io.javalin.http.*;
import io.javalin.http.servlet.JavalinServletRequest;
import io.javalin.http.util.AsyncTaskConfig;
import io.javalin.http.util.CookieStore;
import io.javalin.http.util.ETagGenerator;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.ContextPlugin;
import io.javalin.security.BasicAuthCredentials;
import io.javalin.security.RouteRole;
import io.javalin.util.function.ThrowingRunnable;
import io.javalin.validation.BodyValidator;
import io.javalin.validation.Validator;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kotlin.io.ByteStreamsKt;
import kotlin.jvm.functions.Function1;
import kotlin.reflect.KClass;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.clever.core.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/09/25 09:52 <br/>
 */
public class Context implements io.javalin.http.Context {
    private final HttpServletRequest req;
    private final HttpServletResponse res;
    private final JavalinConfig config;
    private volatile InputStream resultStream = null;
    private volatile Supplier<? extends CompletableFuture<?>> userFutureSupplier = null;

    public Context(HttpServletRequest req, HttpServletResponse res, JavalinConfig config) {
        Assert.notNull(req, "参数 req 不能为 null");
        Assert.notNull(res, "参数 res 不能为 null");
        Assert.notNull(config, "参数 config 不能为 null");
        this.req = new JavalinServletRequest(req);
        this.res = res;
        this.config = config;
    }

    // --------------------------------------------------------------------------------------------
    // 请求
    // --------------------------------------------------------------------------------------------

    /**
     * request body as string
     */
    @NotNull
    @Override
    public String body() {
        return io.javalin.http.Context.super.body();
    }

    /**
     * request body as array of bytes
     */
    @Override
    public byte @NotNull [] bodyAsBytes() {
        return io.javalin.http.Context.super.bodyAsBytes();
    }

    /**
     * request body as specified class (deserialized from JSON)
     */
    @Override
    public <T> T bodyAsClass(@NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.bodyAsClass(clazz);
    }

    /**
     * request body as specified class (deserialized from JSON)
     */
    @Override
    public <T> T bodyAsClass(@NotNull Type type) {
        return io.javalin.http.Context.super.bodyAsClass(type);
    }

    /**
     * request body as specified class (memory optimized version of above)
     */
    @Override
    public <T> T bodyStreamAsClass(@NotNull Type type) {
        return io.javalin.http.Context.super.bodyStreamAsClass(type);
    }

    /**
     * request body as validator typed as specified class
     */
    @NotNull
    @Override
    public <T> BodyValidator<T> bodyValidator(@NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.bodyValidator(clazz);
    }

    /**
     * the underlying input stream of the request
     */
    @NotNull
    @Override
    public InputStream bodyInputStream() {
        return io.javalin.http.Context.super.bodyInputStream();
    }

    /**
     * uploaded file by name
     */
    @Nullable
    @Override
    public UploadedFile uploadedFile(@NotNull String fileName) {
        return io.javalin.http.Context.super.uploadedFile(fileName);
    }

    /**
     * all uploaded files by name
     */
    @NotNull
    @Override
    public List<UploadedFile> uploadedFiles(@NotNull String fileName) {
        return io.javalin.http.Context.super.uploadedFiles(fileName);
    }

    /**
     * all uploaded files as list
     */
    @NotNull
    @Override
    public List<UploadedFile> uploadedFiles() {
        return io.javalin.http.Context.super.uploadedFiles();
    }

    /**
     * all uploaded files as a "names by files" map
     */
    @NotNull
    @Override
    public Map<String, List<UploadedFile>> uploadedFileMap() {
        return io.javalin.http.Context.super.uploadedFileMap();
    }

    /**
     * form parameter by name, as string
     */
    @Nullable
    @Override
    public String formParam(@NotNull String key) {
        return io.javalin.http.Context.super.formParam(key);
    }

    /**
     * form parameter by name, as validator typed as specified class
     */
    @NotNull
    @Override
    public <T> Validator<T> formParamAsClass(@NotNull String key, @NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.formParamAsClass(key, clazz);
    }

    /**
     * list of form parameters by name
     */
    @NotNull
    @Override
    public List<String> formParams(@NotNull String key) {
        return io.javalin.http.Context.super.formParams(key);
    }

    /**
     * list of form parameters by name, as validator typed as specified class
     */
    @NotNull
    @Override
    public <T> Validator<List<T>> formParamsAsClass(@NotNull String key, @NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.formParamsAsClass(key, clazz);
    }

    /**
     * map of all form parameters
     */
    @NotNull
    @Override
    public Map<String, List<String>> formParamMap() {
        return io.javalin.http.Context.super.formParamMap();
    }

    /**
     * path parameter by name as string
     */
    @NotNull
    @Override
    public String pathParam(@NotNull String s) {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * path parameter as validator typed as specified class
     */
    @NotNull
    @Override
    public <T> Validator<T> pathParamAsClass(@NotNull String key, @NotNull Class<T> clazz) {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * map of all path parameters
     */
    @NotNull
    @Override
    public Map<String, String> pathParamMap() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * basic auth credentials (or null if not set)
     */
    @Nullable
    @Override
    public BasicAuthCredentials basicAuthCredentials() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * set an attribute on the request
     */
    @Override
    public void attribute(@NotNull String key, @Nullable Object value) {
        io.javalin.http.Context.super.attribute(key, value);
    }

    /**
     * get an attribute on the request
     */
    @Nullable
    @Override
    public <T> T attribute(@NotNull String key) {
        return io.javalin.http.Context.super.attribute(key);
    }

    /**
     * get an attribute or compute it based on the context if absent
     */
    @Nullable
    @Override
    public <T> T attributeOrCompute(@NotNull String key, @NotNull Function1<? super io.javalin.http.Context, ? extends T> callback) {
        return io.javalin.http.Context.super.attributeOrCompute(key, callback);
    }

    /**
     * map of all attributes on the request
     */
    @NotNull
    @Override
    public Map<String, Object> attributeMap() {
        return io.javalin.http.Context.super.attributeMap();
    }

    /**
     * content length of the request body
     */
    @Override
    public int contentLength() {
        return io.javalin.http.Context.super.contentLength();
    }

    /**
     * request content type
     */
    @Nullable
    @Override
    public String contentType() {
        return io.javalin.http.Context.super.contentType();
    }

    /**
     * request cookie by name
     */
    @Nullable
    @Override
    public String cookie(@NotNull String name) {
        return io.javalin.http.Context.super.cookie(name);
    }

    /**
     * map of all request cookies
     */
    @NotNull
    @Override
    public Map<String, String> cookieMap() {
        return io.javalin.http.Context.super.cookieMap();
    }

    /**
     * request header by name (can be used with Header.HEADERNAME)
     */
    @Nullable
    @Override
    public String header(@NotNull String header) {
        return io.javalin.http.Context.super.header(header);
    }

    /**
     * request header by name, as validator typed as specified class
     */
    @NotNull
    @Override
    public <T> Validator<T> headerAsClass(@NotNull String header, @NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.headerAsClass(header, clazz);
    }

    /**
     * map of all request headers
     */
    @NotNull
    @Override
    public Map<String, String> headerMap() {
        return io.javalin.http.Context.super.headerMap();
    }

    /**
     * host as string
     */
    @Nullable
    @Override
    public String host() {
        return io.javalin.http.Context.super.host();
    }

    /**
     * ip as string
     */
    @NotNull
    @Override
    public String ip() {
        return io.javalin.http.Context.super.ip();
    }

    /**
     * true if the request is multipart
     */
    @Override
    public boolean isMultipart() {
        return io.javalin.http.Context.super.isMultipart();
    }

    /**
     * true if the request is multipart/formdata
     */
    @Override
    public boolean isMultipartFormData() {
        return io.javalin.http.Context.super.isMultipartFormData();
    }

    /**
     * request methods (GET, POST, etc)
     */
    @NotNull
    @Override
    public HandlerType method() {
        return io.javalin.http.Context.super.method();
    }

    /**
     * handler type of the current handler (BEFORE, AFTER, GET, etc.)
     */
    @NotNull
    @Override
    public HandlerType handlerType() {
        return this.method();
    }

    /**
     * request path
     */
    @NotNull
    @Override
    public String path() {
        return io.javalin.http.Context.super.path();
    }

    /**
     * request port
     */
    @Override
    public int port() {
        return io.javalin.http.Context.super.port();
    }

    /**
     * request protocol
     */
    @NotNull
    @Override
    public String protocol() {
        return io.javalin.http.Context.super.protocol();
    }

    /**
     * query param by name as string
     */
    @Nullable
    @Override
    public String queryParam(@NotNull String key) {
        return io.javalin.http.Context.super.queryParam(key);
    }

    /**
     * query param by name, as validator typed as specified class
     */
    @NotNull
    @Override
    public <T> Validator<T> queryParamAsClass(@NotNull String key, @NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.queryParamAsClass(key, clazz);
    }

    /**
     * query param list by name as string
     */
    @NotNull
    @Override
    public List<String> queryParams(@NotNull String key) {
        return io.javalin.http.Context.super.queryParams(key);
    }

    /**
     * query param list by name, as validator typed as list of specified class
     */
    @NotNull
    @Override
    public <T> Validator<List<T>> queryParamsAsClass(@NotNull String key, @NotNull Class<T> clazz) {
        return io.javalin.http.Context.super.queryParamsAsClass(key, clazz);
    }

    /**
     * map of all query parameters
     */
    @NotNull
    @Override
    public Map<String, List<String>> queryParamMap() {
        return io.javalin.http.Context.super.queryParamMap();
    }

    /**
     * full query string
     */
    @Nullable
    @Override
    public String queryString() {
        return io.javalin.http.Context.super.queryString();
    }

    /**
     * request scheme
     */
    @NotNull
    @Override
    public String scheme() {
        return io.javalin.http.Context.super.scheme();
    }

    /**
     * set a session attribute
     */
    @Override
    public void sessionAttribute(@NotNull String key, @Nullable Object value) {
        io.javalin.http.Context.super.sessionAttribute(key, value);
    }

    /**
     * get a session attribute
     */
    @Nullable
    @Override
    public <T> T sessionAttribute(@NotNull String key) {
        return io.javalin.http.Context.super.sessionAttribute(key);
    }

    /**
     * get a session attribute, and set value to null
     */
    @Nullable
    @Override
    public <T> T consumeSessionAttribute(@NotNull String key) {
        return io.javalin.http.Context.super.consumeSessionAttribute(key);
    }

    /**
     * set a session attribute, and cache the value as a request attribute
     */
    @Override
    public void cachedSessionAttribute(@NotNull String key, @Nullable Object value) {
        io.javalin.http.Context.super.cachedSessionAttribute(key, value);
    }

    /**
     * get a session attribute, and cache the value as a request attribute
     */
    @Nullable
    @Override
    public <T> T cachedSessionAttribute(@NotNull String key) {
        return io.javalin.http.Context.super.cachedSessionAttribute(key);
    }

    /**
     * same as above, but compute and set if value is absent
     */
    @Nullable
    @Override
    public <T> T cachedSessionAttributeOrCompute(@NotNull String key, @NotNull Function1<? super io.javalin.http.Context, ? extends T> callback) {
        return io.javalin.http.Context.super.cachedSessionAttributeOrCompute(key, callback);
    }

    /**
     * map of all session attributes
     */
    @NotNull
    @Override
    public Map<String, Object> sessionAttributeMap() {
        return io.javalin.http.Context.super.sessionAttributeMap();
    }

    /**
     * request url
     */
    @NotNull
    @Override
    public String url() {
        return io.javalin.http.Context.super.url();
    }

    /**
     * request url + query string
     */
    @NotNull
    @Override
    public String fullUrl() {
        return io.javalin.http.Context.super.fullUrl();
    }

    /**
     * request context path
     */
    @NotNull
    @Override
    public String contextPath() {
        return io.javalin.http.Context.super.contextPath();
    }

    /**
     * request user agent
     */
    @Nullable
    @Override
    public String userAgent() {
        return io.javalin.http.Context.super.userAgent();
    }

    /**
     * get the underlying HttpServletRequest
     */
    @NotNull
    @Override
    public HttpServletRequest req() {
        return req;
    }

    @Override
    public boolean isJson() {
        return io.javalin.http.Context.super.isJson();
    }

    @Override
    public boolean isFormUrlencoded() {
        return io.javalin.http.Context.super.isFormUrlencoded();
    }

    @Nullable
    @Override
    public String characterEncoding() {
        return io.javalin.http.Context.super.characterEncoding();
    }

    // --------------------------------------------------------------------------------------------
    // 响应
    // --------------------------------------------------------------------------------------------

    /**
     * set result stream to specified string (overwrites any previously set result)
     */
    @NotNull
    @Override
    public Context result(@NotNull String string) {
        io.javalin.http.Context.super.result(string);
        return this;
    }

    /**
     * set result stream to specified byte array (overwrites any previously set result)
     */
    @NotNull
    @Override
    public Context result(byte @NotNull [] bytes) {
        io.javalin.http.Context.super.result(bytes);
        return this;
    }

    /**
     * set result stream to specified input stream (overwrites any previously set result)
     */
    @NotNull
    @Override
    public Context result(@NotNull InputStream inputStream) {
        if (resultStream != null) {
            IOUtils.closeQuietly(resultStream);
        }
        resultStream = inputStream;
        return this;
    }

    /**
     * set the result to be a future, see async section (overwrites any previously set result)
     */
    @Override
    public void future(@NotNull Supplier<? extends CompletableFuture<?>> future) {
        if (userFutureSupplier != null) {
            throw new IllegalStateException("Cannot override future from the same handler");
        }
        userFutureSupplier = future;
    }

    /**
     * write content immediately as seekable stream (useful for audio and video)
     */
    @Override
    public void writeSeekableStream(@NotNull InputStream inputStream, @NotNull String contentType) {
        io.javalin.http.Context.super.writeSeekableStream(inputStream, contentType);
    }

    /**
     * write content immediately as seekable stream (useful for audio and video)
     */
    @Override
    public void writeSeekableStream(@NotNull InputStream inputStream, @NotNull String contentType, long totalBytes) {
        io.javalin.http.Context.super.writeSeekableStream(inputStream, contentType, totalBytes);
    }

    @Override
    public void writeJsonStream(@NotNull Stream<?> stream) {
        this.jsonMapper().writeToOutputStream(stream, this.contentType(ContentType.APPLICATION_JSON).outputStream());
    }

    /**
     * get current result stream as string (if possible), and reset result stream
     */
    @Nullable
    @Override
    public String result() {
        return io.javalin.http.Context.super.result();
    }

    /**
     * get current result stream
     */
    @Nullable
    @Override
    public InputStream resultInputStream() {
        return resultStream;
    }

    /**
     * set the response content type
     */
    @NotNull
    @Override
    public Context contentType(@NotNull String contentType) {
        io.javalin.http.Context.super.contentType(contentType);
        return this;
    }

    /**
     * set the response content type
     */
    @NotNull
    @Override
    public Context contentType(@NotNull ContentType contentType) {
        io.javalin.http.Context.super.contentType(contentType);
        return this;
    }

    /**
     * set response header by name (can be used with Header.HEADERNAME)
     */
    @NotNull
    @Override
    public Context header(@NotNull String name, @NotNull String value) {
        io.javalin.http.Context.super.header(name, value);
        return this;
    }

    /**
     * redirect to the given path with the given status code
     */
    @Override
    public void redirect(@NotNull String location, @NotNull HttpStatus status) {
        header(Header.LOCATION, location).status(status).result("Redirected");
    }

    /**
     * redirect to the given path with the given status code
     */
    @Override
    public void redirect(@NotNull String location) {
        io.javalin.http.Context.super.redirect(location);
    }

    /**
     * set the response status code
     */
    @NotNull
    @Override
    public Context status(int status) {
        io.javalin.http.Context.super.status(status);
        return this;
    }

    /**
     * set the response status code
     */
    @NotNull
    @Override
    public Context status(@NotNull HttpStatus status) {
        io.javalin.http.Context.super.status(status);
        return this;
    }

    /**
     * get the response status code
     */
    @NotNull
    @Override
    public HttpStatus status() {
        return io.javalin.http.Context.super.status();
    }

    /**
     * get the response status code
     */
    @Override
    public int statusCode() {
        return io.javalin.http.Context.super.statusCode();
    }

    /**
     * set response cookie by name, with value and max-age (optional).
     */
    @NotNull
    @Override
    public Context cookie(@NotNull String name, @NotNull String value, int maxAge) {
        io.javalin.http.Context.super.cookie(name, value, maxAge);
        return this;
    }

    /**
     * set response cookie by name, with value and max-age (optional).
     */
    @NotNull
    @Override
    public Context cookie(@NotNull String name, @NotNull String value) {
        io.javalin.http.Context.super.cookie(name, value);
        return this;
    }

    /**
     * set cookie using javalin Cookie class
     */
    @NotNull
    @Override
    public Context cookie(@NotNull Cookie cookie) {
        io.javalin.http.Context.super.cookie(cookie);
        return this;
    }

    /**
     * removes cookie by name and path (optional)
     */
    @NotNull
    @Override
    public Context removeCookie(@NotNull String name, @Nullable String path) {
        io.javalin.http.Context.super.removeCookie(name, path);
        return this;
    }

    /**
     * removes cookie by name and path (optional)
     */
    @NotNull
    @Override
    public Context removeCookie(@NotNull String name) {
        io.javalin.http.Context.super.removeCookie(name);
        return this;
    }

    /**
     * calls result(jsonString), and also sets content type to json
     */
    @NotNull
    @Override
    public Context json(@NotNull Object obj, @NotNull Type type) {
        io.javalin.http.Context.super.json(obj, type);
        return this;
    }

    /**
     * calls result(jsonString), and also sets content type to json
     */
    @NotNull
    @Override
    public Context json(@NotNull Object obj) {
        io.javalin.http.Context.super.json(obj);
        return this;
    }

    /**
     * calls result(jsonStream), and also sets content type to json
     */
    @NotNull
    @Override
    public Context jsonStream(@NotNull Object obj, @NotNull Type type) {
        io.javalin.http.Context.super.jsonStream(obj, type);
        return this;
    }

    /**
     * calls result(jsonStream), and also sets content type to json
     */
    @NotNull
    @Override
    public Context jsonStream(@NotNull Object obj) {
        io.javalin.http.Context.super.jsonStream(obj);
        return this;
    }

    /**
     * calls result(string), and also sets content type to html
     */
    @NotNull
    @Override
    public Context html(@NotNull String html) {
        io.javalin.http.Context.super.html(html);
        return this;
    }

    /**
     * calls html(renderedTemplate)
     */
    @NotNull
    @Override
    public Context render(@NotNull String filePath, @NotNull Map<String, ?> model) {
        io.javalin.http.Context.super.render(filePath, model);
        return this;
    }

    /**
     * calls html(renderedTemplate)
     */
    @NotNull
    @Override
    public Context render(@NotNull String filePath) {
        io.javalin.http.Context.super.render(filePath);
        return this;
    }

    /**
     * get the underlying HttpServletResponse
     */
    @NotNull
    @Override
    public HttpServletResponse res() {
        return res;
    }

    @NotNull
    @Override
    @SneakyThrows
    public ServletOutputStream outputStream() {
        return res.getOutputStream();
    }

    @NotNull
    @Override
    public Charset responseCharset() {
        return io.javalin.http.Context.super.responseCharset();
    }

    @NotNull
    @Override
    public Context removeHeader(@NotNull String name) {
        io.javalin.http.Context.super.removeHeader(name);
        return this;
    }

    // --------------------------------------------------------------------------------------------
    // 其他方法
    // --------------------------------------------------------------------------------------------

    /**
     * get data from the Javalin instance (see app data section below)
     */
    @Override
    public <T> T appData(@NotNull Key<T> key) {
        return config.pvt.appDataManager.get(key);
    }

    /**
     * Get configured JsonMapper
     */
    @NotNull
    @Override
    public JsonMapper jsonMapper() {
        return config.pvt.appDataManager.get(JavalinAppDataKey.JSON_MAPPER_KEY);
    }

    /**
     * see cookie store section below
     */
    @NotNull
    @Override
    public CookieStore cookieStore() {
        return io.javalin.http.Context.super.cookieStore();
    }

    /**
     * get the path that was used to match this request (ex, "/hello/{name}")
     */
    @NotNull
    @Override
    public String matchedPath() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * get the path of the endpoint handler that was used to match this request
     */
    @NotNull
    @Override
    public String endpointHandlerPath() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * skip all remaining handlers for this request
     */
    @NotNull
    @Override
    public Context skipRemainingHandlers() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * get context plugin by class, see plugin section below
     */
    @Override
    public <T> T with(@NotNull Class<? extends ContextPlugin<?, T>> aClass) {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * get context plugin by class, see plugin section below
     */
    @Override
    public <T> T with(@NotNull KClass<? extends ContextPlugin<?, T>> clazz) {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * lifts request out of Jetty's ThreadPool, and moves it to Javalin's AsyncThreadPool
     */
    @Override
    public void async(@NotNull ThrowingRunnable<Exception> task) {
        io.javalin.http.Context.super.async(task);
    }

    /**
     * same as above, but with additional config
     */
    @Override
    public void async(@NotNull Consumer<AsyncTaskConfig> config, @NotNull ThrowingRunnable<Exception> task) {
        io.javalin.http.Context.super.async(config, task);
    }

    @Override
    public boolean strictContentTypes() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    @NotNull
    @Override
    public Context minSizeForCompression(int minSizeForCompression) {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    @NotNull
    @Override
    public Set<RouteRole> routeRoles() {
        throw new UnsupportedOperationException("当前操作只能在 javalin 框架环境中使用");
    }

    /**
     * 把 {@link Context#resultInputStream()} 流写入响应流中
     */
    public static void flushResultStream(Context ctx) throws IOException {
        if (ctx.res().isCommitted()) {
            return;
        }
        try (final InputStream resultStream = ctx.resultInputStream()) {
            if (resultStream == null) {
                return;
            }
            boolean etagWritten = ETagGenerator.INSTANCE.tryWriteEtagAndClose(
                ctx.config.http.generateEtags, ctx, resultStream
            );
            if (!etagWritten) {
                ByteStreamsKt.copyTo(resultStream, ctx.outputStream(), 4096);
            }
        }
    }
}
