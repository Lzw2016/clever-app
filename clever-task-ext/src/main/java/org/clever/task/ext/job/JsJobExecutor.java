//package org.clever.task.ext.job;
//
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.clever.graaljs.core.GraalConstant;
//import org.clever.graaljs.core.ScriptEngineInstance;
//import org.clever.graaljs.core.internal.jackson.JacksonMapperSupport;
//import org.clever.task.core.JobExecutor;
//import org.clever.task.core.TaskStore;
//import org.clever.task.core.entity.*;
//import org.clever.task.core.exception.JobExecutorException;
//import org.graalvm.polyglot.Value;
//
//import java.util.Date;
//import java.util.LinkedHashMap;
//import java.util.Objects;
//
///**
// * 作者：lizw <br/>
// * 创建时间：2021/08/16 13:55 <br/>
// */
//@Slf4j
//public class JsJobExecutor implements JobExecutor {
//    private final ScriptEngineInstance scriptEngineInstance;
//
//    public JsJobExecutor(ScriptEngineInstance scriptEngineInstance) {
//        this.scriptEngineInstance = scriptEngineInstance;
//    }
//
//    @Override
//    public boolean support(int jobType) {
//        return Objects.equals(jobType, EnumConstant.JOB_TYPE_3);
//    }
//
//    @Override
//    public int order() {
//        return 0;
//    }
//
//    @Override
//    public void exec(Date dbNow, Job job, Scheduler scheduler, TaskStore taskStore) throws Exception {
//        final JsJob jsJob = taskStore.beginReadOnlyTX(status -> taskStore.getJsJob(scheduler.getNamespace(), job.getId()));
//        if (jsJob == null) {
//            throw new JobExecutorException(String.format("JsJob数据不存在，JobId=%s", job.getId()));
//        }
//        final FileResource fileResource = taskStore.beginReadOnlyTX(status -> taskStore.getFileResourceById(scheduler.getNamespace(), jsJob.getFileResourceId()));
//        if (fileResource == null) {
//            throw new JobExecutorException(String.format("FileResource数据不存在，JobId=%s | FileResourceId=%s", job.getId(), jsJob.getFileResourceId()));
//        }
//        final LinkedHashMap<?, ?> jobData = StringUtils.isBlank(job.getJobData())
//                ? new LinkedHashMap<>()
//                : JacksonMapperSupport.getHttpApiJacksonMapper().fromJson(job.getJobData(), LinkedHashMap.class);
//        scriptEngineInstance.wrapFunctionAndEval(fileResource.getContent(), scriptObject -> {
//            final Value bindings = scriptObject.getContext().getBindings(GraalConstant.Js_Language_Id);
//            final String ctxName = "jobData";
//            try {
//                bindings.putMember(ctxName, jobData);
//                scriptObject.executeVoid(jobData);
//            } finally {
//                bindings.removeMember(ctxName);
//            }
//        });
//        log.debug("JsJob执行完成，JobId=={} | FileResourceId=={}", job.getId(), jsJob.getFileResourceId());
//        if (Objects.equals(job.getIsUpdateData(), EnumConstant.JOB_IS_UPDATE_DATA_1)) {
//            job.setJobData(JacksonMapperSupport.getHttpApiJacksonMapper().toJson(jobData));
//        }
//    }
//}
