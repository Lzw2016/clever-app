package org.clever.task.core.job;

import kotlin.Pair;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.Conv;
import org.clever.core.SystemClock;
import org.clever.core.http.HttpUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.HttpJobModel;
import org.clever.task.core.model.entity.TaskHttpJob;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/12 12:24 <br/>
 */
@Slf4j
public class HttpJobExecutor implements JobExecutor {
    private static final String LINE = "\n";
    private static final String SEPARATE = ":";
    private static final ThreadLocal<JobContext> JOB_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();
    private static final OkHttpClient OKHTTP_CLIENT;

    static {
        long readTimeout = 30L;
        long connectTimeout = 5L;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                final JobContext context = JOB_CONTEXT_THREAD_LOCAL.get();
                // 构造请求对象
                Request request = chain.request().newBuilder()
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
                    .build();
                // 请求数据日志
                final long start = SystemClock.now();
                StringBuilder logs = new StringBuilder();
                logs.append(String.format("---> 请求 [%1$s] %2$s", request.method(), request.url())).append(LINE);
                int maxWidth = logHeaders(request.headers(), logs);
                if (request.body() != null) {
                    logs.append(StringUtils.rightPad("body" + SEPARATE, maxWidth)).append(Conv.asString(request.body())).append(LINE);
                }
                logs.append(LINE);
                context.info(logs.toString());
                logs.setLength(0);
                // 执行http请求
                Response response = chain.proceed(request);
                final long end = SystemClock.now();
                // 响应数据日志
                logs = new StringBuilder();
                logs.append(String.format("<--- 响应 [%1$d] %2$s (%3$dms)", response.code(), response.request().url(), (end - start))).append(LINE);
                maxWidth = logHeaders(response.headers(), logs);
                if (response.body() != null) {
                    logs.append(StringUtils.rightPad("body" + SEPARATE, maxWidth)).append(response.body().string()).append(LINE);
                }
                context.info(logs.toString());
                logs.setLength(0);
                return response;
            })
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .connectTimeout(connectTimeout, TimeUnit.SECONDS);
        OKHTTP_CLIENT = builder.build();
    }

    private static int logHeaders(okhttp3.Headers headers, StringBuilder logs) {
        Iterator<Pair<String, String>> iterator = headers.iterator();
        int maxWidth = 0;
        while (iterator.hasNext()) {
            Pair<String, String> pair = iterator.next();
            maxWidth = Math.max(maxWidth, pair.getFirst().length());
        }
        maxWidth = maxWidth + 2;
        iterator = headers.iterator();
        while (iterator.hasNext()) {
            Pair<String, String> pair = iterator.next();
            logs.append(StringUtils.rightPad(pair.getFirst() + SEPARATE, maxWidth)).append(pair.getSecond()).append(LINE);
        }
        return maxWidth;
    }

    @Override
    public boolean support(int jobType) {
        return Objects.equals(jobType, EnumConstant.JOB_TYPE_1);
    }

    @Override
    public void exec(final JobContext context) throws Exception {
        final TaskJob job = context.getJob();
        final TaskStore taskStore = context.getTaskStore();
        final TaskScheduler scheduler = context.getScheduler();
        TaskHttpJob httpJob = taskStore.beginReadOnlyTX(status -> taskStore.getHttpJob(scheduler.getNamespace(), job.getId()));
        if (httpJob == null) {
            throw new JobExecutorException(String.format("HttpJob数据不存在，JobId=%s", job.getId()));
        }
        context.setInnerData(JobContext.INNER_HTTP_JOB_KEY, httpJob);
        HttpJobModel.HttpRequestData requestData;
        if (StringUtils.isBlank(httpJob.getRequestData())) {
            requestData = new HttpJobModel.HttpRequestData();
        } else {
            requestData = JacksonMapper.getInstance().fromJson(httpJob.getRequestData(), HttpJobModel.HttpRequestData.class);
        }
        Request.Builder builder = HttpUtils.createRequestBuilder(httpJob.getRequestUrl(), requestData.getHeaders(), requestData.getParams());
        RequestBody requestBody = null;
        if (requestData.getBody() != null && !requestData.getBody().isEmpty()) {
            String jsonBody = JacksonMapper.getInstance().toJson(requestData.getBody());
            requestBody = RequestBody.create(jsonBody, MediaType.parse(HttpUtils.MediaType_Json));
        }
        builder.method(httpJob.getRequestMethod(), requestBody);
        JOB_CONTEXT_THREAD_LOCAL.set(context);
        try (Response response = HttpUtils.execute(OKHTTP_CLIENT, builder.build())) {
            if (StringUtils.isBlank(httpJob.getSuccessCheck())) {
                int status = response.code();
                if (status < 200 || status >= 300) {
                    throw new JobExecutorException(String.format("Http任务执行失败，status=%s", status));
                }
            } else {
                // TODO 执行js校验逻辑
                // httpJob.getSuccessCheck()
            }
        } finally {
            JOB_CONTEXT_THREAD_LOCAL.remove();
        }
    }
}
