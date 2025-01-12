package org.clever.core.retrofit2;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.clever.core.mapper.JacksonMapper;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2017/10/18 17:20 <br/>
 */
@Slf4j
public class ClientFactory {
    private static final OkHttpClient OK_HTTP_CLIENT;

    static {
        long timeout = 60L;
        OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                Request request = chain.request().newBuilder()
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
                    .build();
                long start = System.currentTimeMillis();
                log.info(String.format("---> 请求 [%1$s] %2$s ", request.method(), request.url()));
                Response response = chain.proceed(request);
                long end = System.currentTimeMillis();
                log.info(String.format("<--- 响应 [%1$d] %2$s (%3$dms)", response.code(), response.request().url(), (end - start)));
                return response;
            })
            .readTimeout(timeout, TimeUnit.SECONDS)
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .build();
    }

    /**
     * 获取客户端代理
     *
     * @param baseUrl 请求地址
     * @param clazz   客户端接口类
     * @param <T>     客户端类型
     * @return 客户端代理
     */
    public static <T> T getClient(String baseUrl, Class<T> clazz) {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OK_HTTP_CLIENT)
            .addConverterFactory(JacksonConverterFactory.create(JacksonMapper.getInstance().getMapper()))
            .build();
        return retrofit.create(clazz);
    }
}
