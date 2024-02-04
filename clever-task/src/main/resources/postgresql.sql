/* ====================================================================================================================
    task_scheduler -- 调度器
==================================================================================================================== */
create table task_scheduler
(
    id                      int8                                not null,
    namespace               varchar(63)                         not null,
    instance_name           varchar(127)                        not null,
    last_heartbeat_time     timestamp                           not null,
    heartbeat_interval      int8            default 3000        not null,
    config                  text                                not null,
    runtime_info            text,
    description             varchar(511),
    create_at               timestamp       default now()       not null,
    update_at               timestamp,
    primary key (id)
);
comment on table task_scheduler is '调度器';
comment on column task_scheduler.id is '主键id';
comment on column task_scheduler.namespace is '命名空间(同一个namespace的不同调度器属于同一个集群)';
comment on column task_scheduler.instance_name is '调度器实例名称';
comment on column task_scheduler.last_heartbeat_time is '最后心跳时间';
comment on column task_scheduler.heartbeat_interval is '心跳频率(单位：毫秒)';
comment on column task_scheduler.config is '调度器配置，线程池大小、负载权重、最大并发任务数...';
comment on column task_scheduler.runtime_info is '调度器运行时信息';
comment on column task_scheduler.description is '描述';
comment on column task_scheduler.create_at is '创建时间';
comment on column task_scheduler.update_at is '更新时间';
create index idx_task_scheduler_instance_name on task_scheduler (instance_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_job -- 定时任务
==================================================================================================================== */
create table task_job
(
    id int8                                                     not null,
    namespace               varchar(63)                         not null,
    name                    varchar(127)                        not null,
    type                    int2                                not null,
    max_reentry             int2            default 0           not null,
    allow_concurrent        int2            default 1           not null,
    max_retry_count         int4            default 0           not null,
    route_strategy          int2            default 0           not null,
    first_scheduler         varchar(2047),
    whitelist_scheduler     varchar(2047),
    blacklist_scheduler     varchar(2047),
    load_balance            int2            default 1           not null,
    is_update_data          int2            default 1           not null,
    job_data                text,
    run_count               int8            default 0           not null,
    disable                 int2            default 0           not null,
    description             varchar(511),
    create_at               timestamp       default now()       not null,
    update_at               timestamp,
    primary key (id)
);
comment on table task_job is '定时任务';
comment on column task_job.id is '主键id';
comment on column task_job.namespace is '命名空间';
comment on column task_job.name is '任务名称';
comment on column task_job.type is '任务类型，1：http调用，2：java调用，3：js脚本，4：shell脚本';
comment on column task_job.max_reentry is '最大重入执行数量(对于单个节点当前任务未执行完成就触发了下一次执行导致任务重入执行)，小于等于0：表示禁止重入执行';
comment on column task_job.allow_concurrent is '是否允许多节点并发执行，使用悲观锁实现，不建议禁止，0：禁止，1：允许';
comment on column task_job.max_retry_count is '执行失败时的最大重试次数';
comment on column task_job.route_strategy is '路由策略，0：不启用，1：指定节点优先，2：固定节点白名单，3：固定节点黑名单';
comment on column task_job.first_scheduler is '路由策略，1-指定节点优先，调度器名称集合';
comment on column task_job.whitelist_scheduler is '路由策略，2-固定节点白名单，调度器名称集合';
comment on column task_job.blacklist_scheduler is '路由策略，3-固定节点黑名单，调度器名称集合';
comment on column task_job.load_balance is '负载均衡策略，1：抢占，2：随机，3：轮询，4：一致性HASH';
comment on column task_job.is_update_data is '是否更新任务数据，0：不更新，1：更新';
comment on column task_job.job_data is '任务数据(json格式)';
comment on column task_job.run_count is '运行次数';
comment on column task_job.disable is '是否禁用：0-启用，1-禁用';
comment on column task_job.description is '描述';
comment on column task_job.create_at is '创建时间';
comment on column task_job.update_at is '更新时间';
create index idx_task_job_create_at on task_job (create_at);
create index idx_task_job_name on task_job (name);
create index idx_task_job_update_at on task_job (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_http_job -- Http任务
==================================================================================================================== */
create table task_http_job
(
    id                      int8                                not null,
    namespace               varchar(63)                         not null,
    job_id                  int8                                not null,
    request_method          varchar(15)                         not null,
    request_url             varchar(511)                        not null,
    request_data            text,
    create_at               timestamp       default now()       not null,
    update_at               timestamp,
    primary key (id)
);
comment on table task_http_job is 'Http任务';
comment on column task_http_job.id is '主键id';
comment on column task_http_job.namespace is '命名空间';
comment on column task_http_job.job_id is '任务ID';
comment on column task_http_job.request_method is 'http请求method，ALL GET HEAD POST PUT DELETE CONNECT OPTIONS TRACE PATCH';
comment on column task_http_job.request_url is 'Http请求地址';
comment on column task_http_job.request_data is 'Http请求数据json格式，包含：params、headers、body';
comment on column task_http_job.create_at is '创建时间';
comment on column task_http_job.update_at is '更新时间';
create index idx_task_http_job_create_at on task_http_job (create_at);
create index idx_task_http_job_job_id on task_http_job (job_id);
create index idx_task_http_job_request_url on task_http_job (request_url);
create index idx_task_http_job_update_at on task_http_job (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_java_job -- java调用任务
==================================================================================================================== */
create table task_java_job
(
    id                      int8                                not null,
    namespace               varchar(63)                         not null,
    job_id                  int8                                not null,
    is_static               int2            default 1           not null,
    class_name              varchar(255)                        not null,
    class_method            varchar(63)                         not null,
    create_at               timestamp       default now()       not null,
    update_at               timestamp,
    primary key (id)
);
comment on table task_java_job is 'js脚本任务';
comment on column task_java_job.id is '主键id';
comment on column task_java_job.namespace is '命名空间';
comment on column task_java_job.job_id is '任务ID';
comment on column task_java_job.is_static is '是否是静态方法(函数)，0：非静态，1：静态';
comment on column task_java_job.class_name is 'java class全路径';
comment on column task_java_job.class_method is 'java class method';
comment on column task_java_job.create_at is '创建时间';
comment on column task_java_job.update_at is '更新时间';
create index idx_task_java_job_class_method on task_java_job (class_method);
create index idx_task_java_job_class_name on task_java_job (class_name);
create index idx_task_java_job_create_at on task_java_job (create_at);
create index idx_task_java_job_job_id on task_java_job (job_id);
create index idx_task_java_job_update_at on task_java_job (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_js_job -- js脚本任务
==================================================================================================================== */
create table task_js_job
(
    id                  int8                                not null,
    namespace           varchar(63)                         not null,
    job_id              int8                                not null,
    content             text,
    read_only           int2            default 0           not null,
    create_at           timestamp       default now()       not null,
    update_at           timestamp,
    primary key (id)
);
comment on table task_js_job is 'js脚本任务';
comment on column task_js_job.id is '主键id';
comment on column task_js_job.namespace is '命名空间';
comment on column task_js_job.job_id is '任务ID';
comment on column task_js_job.content is '文件内容';
comment on column task_js_job.read_only is '读写权限：0-可读可写，1-只读';
comment on column task_js_job.create_at is '创建时间';
comment on column task_js_job.update_at is '更新时间';
create index idx_task_js_job_create_at on task_js_job (create_at);
create index idx_task_js_job_job_id on task_js_job (job_id);
create index idx_task_js_job_update_at on task_js_job (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_shell_job -- shell脚本任务
==================================================================================================================== */
create table task_shell_job
(
    id                  int8                                not null,
    namespace           varchar(63)                         not null,
    job_id              int8                                not null,
    shell_type          varchar(15)                         not null,
    shell_charset       varchar(15),
    shell_timeout       int4            default 600         not null,
    content             text,
    read_only           int2            default 0           not null,
    create_at           timestamp       default now()       not null,
    update_at           timestamp,
    primary key (id)
);
comment on table task_shell_job is 'js脚本任务';
comment on column task_shell_job.id is '主键id';
comment on column task_shell_job.namespace is '命名空间';
comment on column task_shell_job.job_id is '任务ID';
comment on column task_shell_job.shell_type is 'shell脚本类型：bash|sh|ash|powershell|cmd|python|node|deno|php';
comment on column task_shell_job.shell_charset is '执行终端的字符集编码，如：“UTF-8”';
comment on column task_shell_job.shell_timeout is '执行超时时间，单位：秒，默认：“10分钟”';
comment on column task_shell_job.content is '文件内容';
comment on column task_shell_job.read_only is '读写权限：0-可读可写，1-只读';
comment on column task_shell_job.create_at is '创建时间';
comment on column task_shell_job.update_at is '更新时间';
create index idx_task_shell_job_create_at on task_shell_job (create_at);
create index idx_task_shell_job_job_id on task_shell_job (job_id);
create index idx_task_shell_job_update_at on task_shell_job (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_job_trigger -- 任务触发器
==================================================================================================================== */
create table task_job_trigger
(
    id                  int8                                not null,
    namespace           varchar(63)                         not null,
    job_id              int8                                not null,
    name                varchar(127)                        not null,
    start_time          timestamp                           not null,
    end_time            timestamp,
    last_fire_time      timestamp,
    next_fire_time      timestamp,
    misfire_strategy    int2            default 2           not null,
    allow_concurrent    int2            default 0           not null,
    type                int2                                not null,
    cron                varchar(511),
    fixed_interval      int8,
    fire_count          int8            default 0           not null,
    disable             int2            default 0           not null,
    description         varchar(511),
    create_at           timestamp       default now()       not null,
    update_at           timestamp,
    primary key (id)
);
comment on table task_job_trigger is '任务触发器';
comment on column task_job_trigger.id is '主键id';
comment on column task_job_trigger.namespace is '命名空间';
comment on column task_job_trigger.job_id is '任务ID';
comment on column task_job_trigger.name is '触发器名称';
comment on column task_job_trigger.start_time is '触发开始时间';
comment on column task_job_trigger.end_time is '触发结束时间';
comment on column task_job_trigger.last_fire_time is '上一次触发时间';
comment on column task_job_trigger.next_fire_time is '下一次触发时间';
comment on column task_job_trigger.misfire_strategy is '错过触发策略，1：忽略，2：立即补偿触发一次';
comment on column task_job_trigger.allow_concurrent is '是否允许多节点并行触发，使用悲观锁实现，不建议允许，0：禁止，1：允许';
comment on column task_job_trigger.type is '触发类型，1：cron触发，2：固定间隔触发';
comment on column task_job_trigger.cron is 'cron表达式';
comment on column task_job_trigger.fixed_interval is '固定间隔触发，间隔时间(单位：秒)';
comment on column task_job_trigger.fire_count is '触发次数';
comment on column task_job_trigger.disable is '是否禁用：0-启用，1-禁用';
comment on column task_job_trigger.description is '描述';
comment on column task_job_trigger.create_at is '创建时间';
comment on column task_job_trigger.update_at is '更新时间';
create index idx_task_job_trigger_create_at on task_job_trigger (create_at);
create index idx_task_job_trigger_job_id on task_job_trigger (job_id);
create index idx_task_job_trigger_last_fire_time on task_job_trigger (last_fire_time);
create index idx_task_job_trigger_name on task_job_trigger (name);
create index idx_task_job_trigger_next_fire_time on task_job_trigger (next_fire_time);
create index idx_task_job_trigger_update_at on task_job_trigger (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_scheduler_cmd -- 调度器指令
==================================================================================================================== */
create table task_scheduler_cmd
(
    id                  int8                                not null,
    namespace           varchar(63)                         not null,
    instance_name       varchar(127),
    cmd_info            varchar(2047)                       not null,
    state               int2            default 0           not null,
    create_at           timestamp       default now()       not null,
    update_at           timestamp,
    primary key (id)
);
comment on table task_scheduler_cmd is '调度器指令';
comment on column task_scheduler_cmd.id is '主键id';
comment on column task_scheduler_cmd.namespace is '命名空间';
comment on column task_scheduler_cmd.instance_name is '指定的调度器实例名称，为空表示不指定';
comment on column task_scheduler_cmd.cmd_info is '指令信息';
comment on column task_scheduler_cmd.state is '指令执行状态，0：未执行，1：执行中，2：执行完成';
comment on column task_scheduler_cmd.create_at is '创建时间';
comment on column task_scheduler_cmd.update_at is '更新时间';
create index idx_task_scheduler_cmd_create_at on task_scheduler_cmd (create_at);
create index idx_task_scheduler_cmd_update_at on task_scheduler_cmd (update_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_scheduler_log -- 调度器事件日志
==================================================================================================================== */
create table task_scheduler_log
(
    id                  int8                                not null,
    namespace           varchar(63)                         not null,
    instance_name       varchar(127)                        not null,
    event_name          varchar(63)                         not null,
    log_data            text,
    create_at           timestamp       default now()       not null,
    primary key (id)
);
comment on table task_scheduler_log is '调度器事件日志';
comment on column task_scheduler_log.id is '编号';
comment on column task_scheduler_log.namespace is '命名空间';
comment on column task_scheduler_log.instance_name is '调度器实例名称';
comment on column task_scheduler_log.event_name is '事件名称';
comment on column task_scheduler_log.log_data is '事件日志数据';
comment on column task_scheduler_log.create_at is '创建时间';
create index idx_task_scheduler_log_create_at on task_scheduler_log (create_at);
create index idx_task_scheduler_log_instance_name on task_scheduler_log (instance_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_job_trigger_log -- 任务触发器日志
==================================================================================================================== */
create table task_job_trigger_log
(
    id                  int8                            not null,
    namespace           varchar(63)                     not null,
    instance_name       varchar(127)                    not null,
    job_trigger_id      int8,
    job_id              int8                            not null,
    trigger_name        varchar(127)                    not null,
    fire_time           timestamp                       not null,
    is_manual           int2                            not null,
    trigger_time        int4                            not null,
    last_fire_time      timestamp,
    next_fire_time      timestamp,
    fire_count          int8                            not null,
    mis_fired           int2                            not null,
    trigger_msg         varchar(511),
    create_at           timestamp   default now()       not null,
    primary key (id)
);
comment on table task_job_trigger_log is '任务触发器日志';
comment on column task_job_trigger_log.id is '主键id';
comment on column task_job_trigger_log.namespace is '命名空间';
comment on column task_job_trigger_log.instance_name is '调度器实例名称';
comment on column task_job_trigger_log.job_trigger_id is '任务触发器ID';
comment on column task_job_trigger_log.job_id is '任务ID';
comment on column task_job_trigger_log.trigger_name is '触发器名称';
comment on column task_job_trigger_log.fire_time is '触发时间';
comment on column task_job_trigger_log.is_manual is '是否是手动触发，0：系统自动触发，1：用户手动触发';
comment on column task_job_trigger_log.trigger_time is '触发耗时(单位：毫秒)';
comment on column task_job_trigger_log.last_fire_time is '上一次触发时间';
comment on column task_job_trigger_log.next_fire_time is '下一次触发时间';
comment on column task_job_trigger_log.fire_count is '触发次数';
comment on column task_job_trigger_log.mis_fired is '是否错过了触发，0：否，1：是';
comment on column task_job_trigger_log.trigger_msg is '触发器消息';
comment on column task_job_trigger_log.create_at is '创建时间';
create index idx_task_job_trigger_log_create_at on task_job_trigger_log (create_at);
create index idx_task_job_trigger_log_fire_time on task_job_trigger_log (fire_time);
create index idx_task_job_trigger_log_instance_name on task_job_trigger_log (instance_name);
create index idx_task_job_trigger_log_job_id on task_job_trigger_log (job_id);
create index idx_task_job_trigger_log_last_fire_time on task_job_trigger_log (last_fire_time);
create index idx_task_job_trigger_log_next_fire_time on task_job_trigger_log (next_fire_time);
create index idx_task_job_trigger_log_trigger_name on task_job_trigger_log (trigger_name);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_job_log -- 任务执行日志
==================================================================================================================== */
create table task_job_log
(
    id                  int8                            not null,
    namespace           varchar(63)                     not null,
    instance_name       varchar(127)                    not null,
    job_trigger_log_id  int8                            not null,
    job_trigger_id      int8,
    job_id              int8                            not null,
    fire_time           timestamp                       not null,
    start_time          timestamp                       not null,
    end_time            timestamp,
    run_time            int4,
    status              int2,
    retry_count         int4                            not null,
    exception_info      varchar(2047),
    run_count           int8                            not null,
    before_job_data     text,
    after_job_data      text,
    primary key (id)
);
comment on table task_job_log is '任务执行日志';
comment on column task_job_log.id is '主键id';
comment on column task_job_log.namespace is '命名空间';
comment on column task_job_log.instance_name is '调度器实例名称';
comment on column task_job_log.job_trigger_log_id is '对应的触发器日志ID';
comment on column task_job_log.job_trigger_id is '任务触发器ID';
comment on column task_job_log.job_id is '任务ID';
comment on column task_job_log.fire_time is '触发时间';
comment on column task_job_log.start_time is '开始执行时间';
comment on column task_job_log.end_time is '执行结束时间';
comment on column task_job_log.run_time is '执行耗时(单位：毫秒)';
comment on column task_job_log.status is '任务执行结果，0：成功，1：失败，2：取消';
comment on column task_job_log.retry_count is '重试次数';
comment on column task_job_log.exception_info is '异常信息';
comment on column task_job_log.run_count is '执行次数';
comment on column task_job_log.before_job_data is '执行前的任务数据';
comment on column task_job_log.after_job_data is '执行后的任务数据';
create index idx_task_job_log_end_time on task_job_log (end_time);
create index idx_task_job_log_fire_time on task_job_log (fire_time);
create index idx_task_job_log_instance_name on task_job_log (instance_name);
create index idx_task_job_log_job_id on task_job_log (job_id);
create index idx_task_job_log_job_trigger_id on task_job_log (job_trigger_id);
create index idx_task_job_log_job_trigger_log_id on task_job_log (job_trigger_log_id);
create index idx_task_job_log_start_time on task_job_log (start_time);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_job_console_log -- 任务控制台日志
==================================================================================================================== */
create table task_job_console_log
(
    id                  int8                            not null,
    namespace           varchar(63)                     not null,
    instance_name       varchar(127)                    not null,
    job_id              int8                            not null,
    job_log_id          int8                            not null,
    line_num            int4                            not null,
    log                 text,
    create_at           timestamp   default now()       not null,
    primary key (id)
);
comment on table task_job_console_log is '任务控制台日志';
comment on column task_job_console_log.id is '主键id';
comment on column task_job_console_log.namespace is '命名空间';
comment on column task_job_console_log.instance_name is '调度器实例名称';
comment on column task_job_console_log.job_id is '任务ID';
comment on column task_job_console_log.job_log_id is '任务执行日志ID';
comment on column task_job_console_log.line_num is '日志行号';
comment on column task_job_console_log.log is '日志内容';
comment on column task_job_console_log.create_at is '创建时间';
create index idx_task_job_console_log_instance_name on task_job_console_log (instance_name);
create index idx_task_job_console_log_job_id on task_job_console_log (job_id);
create index idx_task_job_console_log_job_log_id on task_job_console_log (job_log_id);
create index idx_task_job_console_log_create_at on task_job_console_log (create_at);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/


/* ====================================================================================================================
    task_report -- 任务执行报表
==================================================================================================================== */
create table task_report
(
    id                  int8                            not null,
    namespace           varchar(63)                     not null,
    report_time         varchar(31)                     not null,
    job_count           int8                            not null,
    job_err_count       int8                            not null,
    trigger_count       int8                            not null,
    misfire_count       int8                            not null,
    primary key (id)
);
comment on table task_report is '任务执行报表';
comment on column task_report.id is '主键id';
comment on column task_report.namespace is '命名空间';
comment on column task_report.report_time is '报表时间';
comment on column task_report.job_count is 'job 运行总次数';
comment on column task_report.job_err_count is 'job 运行错误次数';
comment on column task_report.trigger_count is '触发总次数';
comment on column task_report.misfire_count is '错过触发次数';
create index idx_task_report_task_report on task_report (report_time);
/*------------------------------------------------------------------------------------------------------------------------

--------------------------------------------------------------------------------------------------------------------------*/
