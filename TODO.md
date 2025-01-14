### 总体

打包体积瘦身(保证在25MB内)

- [OK] org.antlr:antlr4(只依赖“antlr4-runtime”即可)
- [OK] gradle编译时(--continuous)会出现class文件回退问题,不能用于“dev”或“生产”环境[已解决]

升级核心依赖包版本(最新稳定版)

- kotlin
- groovy
- javalin
- jmh
- querydsl
- poi
- p6spy
- schemacrawler

支持JDK

- [OK] JDK8
- [  ] JDK11
- [OK] JDK17
- [  ] JDK21

### clever-core

- [OK] jackson配置
- [OK] 增加了多线程任务编排功能
- [  ]

### clever-groovy

- [  ] 支持动态加载 groovy 代码并执行
- [  ] 支持直接执行 groovy dsl

已暂停

### clever-web

- [OK] ApplyConfigFilter (应用web配置,如: ContentType、CharacterEncoding 等等)
- [OK] EchoFilter (请求日志记录)
- [OK] ExceptionHandlerFilter (异常处理)
- [OK] GlobalRequestParamsFilter (获取全局请求参数: QueryBySort、QueryByPage)
- [OK] CorsFilter (跨域支持)
- [  ] CaptureHandlerFilter (http请求响应数据打印) - 耗性能先不做?
- [OK] MvcHandlerMethodFilter (解析获取MVC的HandlerMethod)
- [OK] StaticResourceFilter (静态资源映射)
- [OK] MvcFilter (实现MVC拦截器逻辑 before-invoke-after-finally)
- [OK] MVC支持注解：Transactional、CookieValue、RequestBody、RequestHeader、RequestParam、RequestPart、ValueConstants
- [  ]

### clever-security

- [OK] 全局异常处理
- [OK] 跨域问题
- [OK] 静态资源问题
- [OK] MVC拦截问题
- [OK] MVC参数绑定
- [OK] MVC数据验证@Validated
- [OK] MVC处理时默认开启(多个)数据源事务[配置化]
- [OK] MVC自定义事务@Transactional
- [  ] WebSocket支持(配置在mvc下,验证:new对象后ClassLoader对象是否会被GC回收)
- [  ] 精简代码 & 简化逻辑 & 整合相同功能代码 & 重构代码 -> 删除 ReadOnlyHttpHeaders HttpHeaders
- [OK] 事务提交或者回滚异常后不会向上抛出异常
- [OK] 全局的404默认返回处理
- [  ]

### clever-data-commons

- [OK] 数据源抽象类型
- [OK] JDBC事务支持
- [OK] 统一数据访问层异常处理

### clever-data-dynamic-sql

- [OK] 支持类似mybatis的动态SQL模版
- [OK] 支持自定义函数 `join` 和 `to_date`

### clever-data-jdbc

```
数据库原生的锁支持
MySQL(锁名称为字符串，支持超时):
    get_lock
    release_lock
    release_all_locks
    is_free_lock
    is_used_lock
PostgreSQL(锁名称为数字，不支持超时，但可以使用hashtext函数将字符串转数字和pg_sleep函数实现超时功能):    
    pg_advisory_lock
    pg_advisory_unlock
    pg_advisory_unlock_all
    pg_advisory_lock_shared
    pg_advisory_unlock_shared
    pg_advisory_xact_lock
    pg_advisory_xact_lock_shared
    pg_try_advisory_lock
    pg_try_advisory_lock_shared
    pg_try_advisory_xact_lock
    pg_try_advisory_xact_lock_shared
Oracle(锁名称为字符串，支持超时):
    dbms_lock.allocate_unique
    dbms_lock.request
    dbms_lock.release
```

- [OK] 需要支持 `#{varName,javaType=int|long|decimal|char|string|date|bool}` 语法
- [OK] join函数支持 `join(java_arr)` 和 `join(java_arr, javaType)` 两种调用形式
- [OK] 利用数据库原生的锁支持实现 nativeLock 相关功能
- [  ] 提供 JdbcMetrics 信息查询接口，查询收集的 sql 执行性能信息
- [OK] p6spy 支持忽略指定的sql(全匹配和包含匹配)
- [OK] p6spy 忽略的sql使用debug级别的日志
- [  ] 更细粒度的sql查询拦截，能实现字段无感加解密、字典自动翻译
- [  ] 支持全局sql重写，能实现全局的数据权限，自定义字段填充

### clever-data-jdbc-meta

```
类型映射
https://www.dbvisitor.net/docs/guides/types/type-handlers
https://www.dbvisitor.net/docs/guides/types/java-jdbc
```

- [OK] 读取MySQL数据库的元数据
- [OK] 读取PostgreSQL数据库的元数据
- [OK] 生成数据库文档, 支持: markdown、html、word文档
- [OK] Entity、QueryDS代码生成, 支持: java、groovy、kotlin语言
- [OK] 支持数据库DDL语句生成, 支持: 表、字段、主键、索引、序列、存储过程、函数
- [OK] 数据库结构同步
- [OK] 数据库数据库同步, 支持查询SQL
- [  ]

已暂停

### clever-data-redis

- [OK] 支持多个 redis 数据源
- [OK] 支持：单节点、哨兵、群集, 三种模式
- [OK] 封装了 redis 的常用操作

### clever-data-rabbitmq

已暂停

### clever-task

- [OK] 支持多集群
- [OK] 支持动态配置任务以及触发器
- [OK] 优化lock实现, 使用数据库原生lock(删除 task_scheduler_lock 表)
- [NO] 使用时间轮算法改良定时任务调度(应用场景不合适)
- [NO] 定时任务支持中断(暂不实现，应用场景较少，后续有需要在考虑)
- [OK] 数据完整性校验、一致性校验
- [OK] 支持控制任务执行节点：指定节点优先、固定节点白名单、固定节点黑名单
- [OK] 支持负载均衡策略：抢占、随机、轮询、一致性HASH
- [OK] 支持任务类型：java调用任务、Http任务、js脚本任务、shell脚本任务
- [OK] 定时任务调度平台API接口
- [OK] 定时任务调度平台Web页面
- [OK] 拆分管理控制台代码(区分定时任务核心代码与管理API代码)
- [OK] 内置日志数据清理任务
- [OK] 新建统计数据表，定时收集统计信息
- [OK] 保存http、java、js、shell任务的执行日志，方便控制台查询
- [OK] 对于 java、js 任务需要支持 JobContext
- [OK] 任何一个节点都能给其它节点发送指令，用于在web控制台操作调度器和执行定时任务
- [OK] 数据校验需要检查最后一条数据时间，如果其它节点已经运行了，就不运行了
- [OK] 修改调度器的暂停逻辑，暂停状态时依然可以执行 “调度器节点注册&更新调度器运行信息”、“集群节点心跳保持”、“数据完整性校验&一致性校验” “调度器指令”、“清理日志数据”、“收集任务执行报表”
- [OK] 当触发器已经完结时，要设置下次触发时间为 null
- [OK] 支持配置启动后调度器的状态

### clever-task-ext

未开始

### clever-js-api

```
Folder                  加载CommonJS模块的代码资源加载器(一般对应文件系统)
Module                  CommonJS模块
ModuleCache             CommonJS模块缓存
CompileModule           把js代码编译成js对象
Require                 内置的 require 对象,实现了CommonJS模块的 require 功能
Console                 内置的 console 对象
Console                 内置的 print 对象
LoggerFactory           内置的 LoggerFactory 对象
ScriptEngineContext     脚本引擎上下文
ScriptEngineInstance    脚本引擎实例
ScriptObject            js对象的java包装类
EngineInstancePool      js引擎池,在多线程环境中使用

自定义注入的全局对象
GlobalConstant.CUSTOM_REGISTER_GLOBAL_VARS
```

- [OK] 抽象了 js 引擎的使用

### clever-js-graaljs

```
// graaljs最新使用方式(更推荐使用)
dependency("org.graalvm.polyglot:polyglot:23.1.2")
dependency("org.graalvm.polyglot:js:23.1.2")
dependency("org.graalvm.polyglot:js-community:23.1.2")
dependency("org.graalvm.polyglot:inspect:23.1.2")
dependency("org.graalvm.polyglot:inspect-community:23.1.2")
dependency("org.graalvm.polyglot:profiler:23.1.2")
dependency("org.graalvm.polyglot:profiler-community:23.1.2")
dependency("org.graalvm.polyglot:tools:23.1.2")
dependency("org.graalvm.polyglot:tools-community:23.1.2")
```

- [OK] 基本实现 clever-js-api
- [  ] 需要解决 js 与 java 相互交互的问题(需要写大量的 targetTypeMapping 实现)
- [  ] 需要升级 graaljs 最新版本

### clever-js-nashorn

- [OK] 基本实现 clever-js-api
  性能最差、与java语言交互能力太弱 不推荐使用，暂停维护

### clever-js-v8

- [OK] 基本实现 clever-js-api
- [  ] 需要解决内存泄漏问题
- [  ] 需要解决 js 与 java 相互交互的问题

### clever-js-shim

### 第三方库
