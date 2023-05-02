package org.clever.task.core.job;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.http.HttpUtils;
import org.clever.core.mapper.JacksonMapper;
import org.clever.task.core.JobExecutor;
import org.clever.task.core.TaskStore;
import org.clever.task.core.exception.JobExecutorException;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.HttpJobModel;
import org.clever.task.core.model.entity.TaskHttpJob;
import org.clever.task.core.model.entity.TaskJob;
import org.clever.task.core.model.entity.TaskScheduler;

import java.util.Date;
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
    public int order() {
        return 0;
    }

    @Override
    public void exec(Date dbNow, TaskJob job, TaskScheduler scheduler, TaskStore taskStore) throws Exception {
        TaskHttpJob httpJob = taskStore.beginReadOnlyTX(status -> taskStore.getHttpJob(scheduler.getNamespace(), job.getId()));
        if (httpJob == null) {
            throw new JobExecutorException(String.format("HttpJob数据不存在，JobId=%s", job.getId()));
        }
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
        try (Response response = HttpUtils.execute(HttpUtils.getInner().getOkHttpClient(), builder.build())) {
            String body = null;
            try (ResponseBody responseBody = response.body()) {
                if (responseBody != null) {
                    body = responseBody.string();
                }
            }
            if (StringUtils.isBlank(httpJob.getSuccessCheck())) {
                int status = response.code();
                if (status < 200 || status >= 300) {
                    throw new JobExecutorException(String.format("Http任务执行失败，response_body=%s", body));
                }
            } else {
                // httpJob.getSuccessCheck() TODO 执行js校验逻辑
            }
        }
    }
}
