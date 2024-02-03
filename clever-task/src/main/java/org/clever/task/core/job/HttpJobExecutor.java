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

/**
 * 作者：lizw <br/>
 * 创建时间：2021/08/12 12:24 <br/>
 */
@Slf4j
public class HttpJobExecutor implements JobExecutor {
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
        String jsonBody = null;
        if (requestData.getBody() != null && !requestData.getBody().isEmpty()) {
            jsonBody = JacksonMapper.getInstance().toJson(requestData.getBody());
            requestBody = RequestBody.create(jsonBody, MediaType.parse(HttpUtils.MediaType_Json));
        }
        builder.method(httpJob.getRequestMethod(), requestBody);
        Request request = builder.build();
        StringBuilder logs = new StringBuilder("请求数据: \n");
        logs.append(String.format("---> 请求 [%1$s] %2$s", request.method(), request.url())).append("\n");
        int maxWidth = logHeaders(request.headers(), logs);
        logs.append(StringUtils.rightPad("body:", maxWidth)).append(Conv.asString(jsonBody)).append("\n");
        context.info(logs.toString());
        logs.setLength(0);
        final long start = SystemClock.now();
        try (Response response = HttpUtils.execute(HttpUtils.getInner().getOkHttpClient(), request)) {
            String body = null;
            try (ResponseBody responseBody = response.body()) {
                if (responseBody != null) {
                    body = responseBody.string();
                }
            }
            final long end = SystemClock.now();
            logs = new StringBuilder("响应数据: \n");
            logs.append(String.format("<--- 响应 [%1$d] %2$s (%3$dms)", response.code(), response.request().url(), (end - start))).append("\n");
            maxWidth = logHeaders(response.headers(), logs);
            logs.append(StringUtils.rightPad("body:", maxWidth)).append(Conv.asString(body)).append("\n");
            context.info(logs.toString());
            logs.setLength(0);
            if (StringUtils.isBlank(httpJob.getSuccessCheck())) {
                int status = response.code();
                if (status < 200 || status >= 300) {
                    throw new JobExecutorException(String.format("Http任务执行失败，response_body=%s", body));
                }
            } else {
                // TODO 执行js校验逻辑
                // httpJob.getSuccessCheck()
            }
        }
    }

    private static int logHeaders(okhttp3.Headers headers, StringBuilder logs) {
        Iterator<Pair<String, String>> iterator = headers.iterator();
        int maxWidth = 8;
        while (iterator.hasNext()) {
            Pair<String, String> pair = iterator.next();
            maxWidth = Math.max(maxWidth, pair.getFirst().length());
        }
        iterator = headers.iterator();
        while (iterator.hasNext()) {
            Pair<String, String> pair = iterator.next();
            String name = pair.getFirst() + ":";
            logs.append(StringUtils.rightPad(name, maxWidth)).append(pair.getSecond()).append("\n");
        }
        return maxWidth;
    }
}
