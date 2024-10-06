# clever-app

一个轻量级的Java Web框架，强调简洁、透明、高效。基于SpringBoot，只增强而不改变，为您带来纯粹的开发体验。

## 特点

#### 兼容SpringBoot

采用SpringBoot技术构建，无缝融入Spring生态以及IDE支持。可以直接使用SpringBoot生态组件，体验一致的便捷开发。

#### 启动时间短且稳定

在SpringBoot项目中，随着项目规模的扩大和依赖项的增多，启动时间往往会显著增加，我参与的许多项目启动时间都超过了1分钟，有的甚至达到了3分钟。
然而，我们采用了一种不同的方法：函数式编程模式，这使得系统启动时间大幅缩短，仅需3到10秒。
这种速度的提升不受项目规模或依赖数量的影响。

以下是我的测试结果：

- 在12代i7处理器上，启动时间仅需3秒。
- 在8代i5处理器上，启动时间也仅为9秒。

这种快速启动的能力，为大型项目的开发和部署带来了显著的优势。

#### 代码即改即生效

提供了强大的类热重载功能，这意味着您在开发过程中无需重启服务即可实时查看代码更改的效果。
只需通过IDE进行增量编译，改动的代码即刻生效，完全不需要依赖像`jrebel`这样的工具。
更令人兴奋的是，内置的热重载技术在生产环境中同样稳定可靠。在项目开发过程中，从代码修改到生效的时间通常在3秒以内。
这样的效率提升，让您的开发流程更加流畅，减少了等待时间，提高了生产力。

#### 支持Kotlin

推荐使用Kotlin的函数式编程模式，它能够简化您的业务开发流程，让您摆脱传统的Controller/Service/Dao模式。
采用Kotlin，您可以享受到类似Go语言的简洁和高效。
此外，也完全支持Java，以及标准的SpringBoot开发模式，确保您能够根据自己的喜好和项目需求灵活选择。

#### 可观测性优先

- **启动日志**：应用启动时，关键配置信息一目了然，确保配置环境准确无误。
- **数据库日志**：内置ORM自动记录SQL执行、事务操作及慢查询，让数据库交互清晰可见。
- **请求日志**：详细记录用户请求和处理时间，慢接口无处藏身。

#### 多数据源支持

提供灵活的多数据源支持，轻松连接和管理JDBC、Redis、MongoDB、RabbitMQ等多种数据源。
可以在运行时自由切换和动态添加数据源，无需依赖AOP或AbstractRoutingDataSource等复杂机制。
直接操作底层API，让数据源管理变得直观而简单。

#### 内置定时任务

- **单节点定时任务**：一个轻量级的线程池调度器，不依赖数据库，直接在`application.yml`文件中配置任务。
- **分布式定时任务**：一个仅依赖数据库的分布式任务调度服务，专为集群环境设计，支持通过命名空间实现集群的隔离。

两种方案，满足不同规模的应用需求，让定时任务管理既简单又强大。

#### 内置JS引擎

内置多种JavaScript引擎，支持灵活的业务逻辑动态调整：

- **Nashorn**：适用于JDK8及以前的版本，JDK内置的JavaScript引擎，与Java程序无缝集成，但性能相对较低。
- **GraalJS**：适用于JDK8及以后的版本，由GraalVM提供支持，与Java程序交互流畅，性能优异。
- **V8**：Google开源的高性能JavaScript引擎，虽然与Java程序的交互稍显复杂，但性能卓越。

根据您的需求，轻松选择最适合的JavaScript引擎，实现生产环境运行时高效灵活的业务逻辑调整。

#### 支持JDK17

随着SpringBoot3的推出，我们推荐使用JDK 17及以上版本以获得最佳性能。
选择JDK 21，您将享受到虚拟线程等创新特性，进一步提升您的开发体验和性能。
切换到`springboot3.x`分支以适配这些先进的Java版本。

## 开始使用

#### Maven使用

```xml
<project>
    <!-- 增加 maven 私服 -->
    <repositories>
        <repository>
            <id>gitea</id>
            <url>http://all.msvc.top:30005/api/packages/clever/maven</url>
        </repository>
    </repositories>
    <!-- 指定版本 -->
    <properties>
        <cleverVersion>3.3.4.0</cleverVersion>
    </properties>
    <!-- 按需引入依赖 -->
    <dependencies>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-spring</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-core</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-data-jdbc</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-data-redis</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-web</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-security</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-task</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
        <dependency>
            <groupId>org.clever</groupId>
            <artifactId>clever-task-ext</artifactId>
            <version>${cleverVersion}</version>
        </dependency>
    </dependencies>
</project>
```

#### Gradle使用(kotlin)

```kotlin
repositories {
    // 增加 maven 私服
    maven(url = "http://all.msvc.top:30005/api/packages/clever/maven/") {
        isAllowInsecureProtocol = true
    }
}
// 指定版本
val cleverVersion = "3.3.4.0"
dependencies {
    // 按需引入依赖
    api("org.clever:clever-spring:${cleverVersion}")
    api("org.clever:clever-core:${cleverVersion}")
    api("org.clever:clever-data-jdbc:${cleverVersion}")
    api("org.clever:clever-data-redis:${cleverVersion}")
    api("org.clever:clever-web:${cleverVersion}")
    api("org.clever:clever-security:${cleverVersion}")
    api("org.clever:clever-task:${cleverVersion}")
    api("org.clever:clever-task-ext:${cleverVersion}")
}
```

#### Gradle使用(groovy)

```groovy
repositories {
    // 增加 maven 私服
    maven {
        url "http://all.msvc.top:30005/api/packages/clever/maven/"
        allowInsecureProtocol = true
    }
}
// 指定版本
ext.cleverVersion = "3.3.4.0"
dependencies {
    // 按需引入依赖
    api("org.clever:clever-spring:${cleverVersion}")
    api("org.clever:clever-core:${cleverVersion}")
    api("org.clever:clever-data-jdbc:${cleverVersion}")
    api("org.clever:clever-data-redis:${cleverVersion}")
    api("org.clever:clever-web:${cleverVersion}")
    api("org.clever:clever-security:${cleverVersion}")
    api("org.clever:clever-task:${cleverVersion}")
    api("org.clever:clever-task-ext:${cleverVersion}")
}
```

示例项目: [clever-examples](https://github.com/Lzw2016/clever-examples)

## 模块说明

| 模块名                     | 完成进度 | 说明                                                                                                      |
|:------------------------|:-----|:--------------------------------------------------------------------------------------------------------|
| clever-spring           | 已完成  | 对spring的轻度封装，以适应底层框架需要                                                                                  |
| clever-core             | 已完成  | 封装常用的工具类: 序列化反序列化、HTTP、编码解码、ID生成、Bean转换验证、Excel、异常、验证码等等                                                |
| clever-data-dynamic-sql | 已完成  | 完全兼容mybatis语法的sql模版，实现动态sql能力                                                                           |
| clever-data-commons     | 已完成  | 数据访问层公共能力封装                                                                                             |
| clever-data-jdbc        | 已完成  | 参考spring-jdbc实现对JDBC操作的轻度封装，同时支持mybatis、query-dsl用法                                                     |
| clever-data-jdbc-meta   | 已完成  | 实现读取数据库元数据功能，支持读取数据库：表结构、索引、主键、序列、函数、存储过程。并且支持数据库数据同步和表结构同步                                             |
| clever-data-redis       | 已完成  | 基于spring-data-redis实现对Redis操作的轻度封装                                                                      |
| clever-data-mongodb     | 未开始  | 实现对mongodb操作的轻度封装                                                                                       |
| clever-data-rabbitmq    | 进行中  | 实现对rabbitmq操作的轻度封装                                                                                      |
| clever-data-rocketmq    | 未开始  | 实现对rocketmq操作的轻度封装                                                                                      |
| clever-data-kafka       | 未开始  | 实现对kafka操作的轻度封装                                                                                         |
| clever-web              | 已完成  | 基于javalin封装servlet web服务器的实现,参考spring mvc部分注解的实现,支持web mvc动态调用java class(包含kotlin或groovy等JVM语言,需要编译器支持) |
| clever-security         | 已完成  | 实现一套RBAC模型的权限认证功能                                                                                       |
| clever-js-api           | 已完成  | 定义JavaScript引擎API                                                                                       |
| clever-js-graaljs       | 已完成  | graaljs引擎实现(推荐)                                                                                         |
| clever-js-nashorn       | 已完成  | nashorn引擎实现                                                                                             |
| clever-js-v8            | 进行中  | v8引擎实现                                                                                                  |
| clever-js-shim          | 进行中  | 用于桥接Java与JS组件                                                                                           |
| clever-openapi          | 未开始  | 自动扫描生成Open API接口文档(类似swagger文档)                                                                         |

#### 系统日志配置

```yaml
logging:
    config: classpath:logback-spring.xml
    file:
        name: '${clever.name:server}'
        path: './logs/${logging.file.name}'
    level:
        root: info
        org.springframework.jdbc.datasource.DataSourceTransactionManager: info
        org.springframework.jdbc.datasource.DataSourceUtils: info
        org.springframework.core.HotReloadClassLoader: debug
        org.springframework.security: debug
        com.zaxxer.hikari.HikariConfig: debug
```

系统日志配置与SpringBoot配置一致。

#### 基础配置

```yaml
app:
    # 用于设置应用的根路径，其它路径配置项的相对路径就是这个配置
    root-path: './'

# 单节点定时任务
startup-task:
    pool-size: 8
    timed-task:
        - name: '测试定时任务'
          enable: true
          interval: 120s
          clazz: 'org.clever.app.task.TaskTest'
          method: 'job01'
    cmd-task:
        - name: '持续编译源码'
          enable: false
          async: true
          work-dir: './'
          cmd:
              windows: 'gradlew.bat classes --continuous --watch-fs --build-cache --configuration-cache --configuration-cache-problems=warn'
              linux: './gradlew     classes --continuous --watch-fs --build-cache --configuration-cache --configuration-cache-problems=warn'
              macos: './gradlew     classes --continuous --watch-fs --build-cache --configuration-cache --configuration-cache-problems=warn'
```

#### JDBC数据源配置

```yaml
jdbc:
    enable: true
    # sql日志配置
    p6spylog:
        enable: true
        slow: 800
        ignore-sql:
            - 'select 1'
            - 'select 1 from dual'
            - 'select now()'
            - 'select now(6) from dual'
        ignore-contains-sql:
            - 'pg_advisory'
            - 'pg_try_advisory'
        ignore-thread:
            - 'task-scheduler-pool-'
    # sql性能统计配置
    metrics:
        enable: true
        max-sql-count: 200
        histogram: [ 20, 50, 100, 200, 500, 1000, 2000, 5000, 20000 ]
        histogram-top-n: 3
    # 全局数据源配置
    global:
        driver-class-name: 'com.p6spy.engine.spy.P6SpyDriver'
        autocommit: true
        maximum-pool-size: 100
        minimum-idle: 5
        max-lifetime: 21600000
        connection-timeout: 30000
        connection-test-query: 'select 1'
        exception-override-class-name: 'org.clever.data.jdbc.hikari.HikariPreventQueryTimeoutEviction'
    # 设置默认数据源
    default-name: 'mysql'
    # 多数据源配置
    data-source:
        mysql:
            jdbc-url: jdbc:p6spy:mysql://127.0.0.1:3306/test
            username: admin
            password: 123456
            minimum-idle: 2
            maximum-pool-size: 100
            connection-test-query: 'select 1 from dual'
        postgresql:
            jdbc-url: jdbc:p6spy:postgresql://127.0.0.1:5432/test
            username: admin
            password: 123456
            minimum-idle: 2
            maximum-pool-size: 100
            connection-test-query: 'select 1'
            data-source-properties:
                stringtype: unspecified
```

代码使用：

```java
// 获取默认的数据源
private static final Jdbc jdbc = DaoFactory.getJdbc();
private static final QueryDSL queryDSL = DaoFactory.getQueryDSL();

// 或者指定的数据源
private static final Jdbc jdbc = DaoFactory.getJdbc("data-source-name");
private static final QueryDSL queryDSL = DaoFactory.getQueryDSL("data-source-name");
```

#### Redis数据源

```yaml
redis:
    enable: true
    # 数据源全局配置
    global:
        mode: standalone
        ssl: false
        read-timeout: 30s
        connect-timeout: 10s
        shutdown-timeout: 100ms
        client-name: 'xxx-app'
        pool:
            enabled: true
            max-idle: 8
            min-idle: 0
            max-active: 8
            max-wait: 30s
            time-between-eviction-runs: 1m
    # 设置默认的数据源
    default-name: 'default'
    # 多数据源配置
    data-source:
        default:
            mode: standalone
            standalone:
                host: '192.168.1.211'
                port: 30007
                database: 0
                password: 'admin123456'
```

代码使用：

```java
// 获取默认的数据源
private static final Redis redis = RedisAdmin.getRedis();

// 或者指定的数据源
private static final Redis redis = RedisAdmin.getRedis("data-source-name");
```

#### MVC配置

```yaml
web:
    mvc:
        path: '/api/'
        http-method: [ 'POST', 'GET' ]
        package-mapping:
            - path-prefix: '/app'
              package-prefix: 'org.clever.app.mvc'
            - path-prefix: '/task'
              package-prefix: 'org.clever.task.manage.mvc'
        allow-packages:
            - 'org.clever.app.mvc.'
            - 'org.clever.task.manage.mvc.'
        transactional-def-datasource: [ ]
        def-transactional:
            datasource: [ ]
            timeout: -1
            read-only: false
        # 热重载配置
        hot-reload:
            enable: true
            # watchFile: './build/.hotReload'
            interval: 1s
            exclude-packages:
                - 'org.clever.task.core.model'
            exclude-classes:
                - 'org.clever.task.core.model.JobInfo'
                - 'org.clever.task.core.model.AbstractTrigger'
                - 'org.clever.task.core.job.JobContext'
            locations:
                - './clever-examples-javalin/out/production/classes'
                - './clever-task/out/production/classes'
                #- './clever-examples-javalin/build/classes/java/main'
                #- './clever-examples-javalin/build/classes/kotlin/main'
```

内置的过滤器链：

```text
🡓
ApplyConfigFilter (应用web配置,如: ContentType、CharacterEncoding 等等)
🡓
EchoFilter(请求日志)
🡓
ExceptionHandlerFilter (异常处理)
🡓
GlobalRequestParamsFilter (获取全局请求参数: QueryBySort、QueryByPage)
🡓
CorsFilter (跨域处理)
🡓
MvcHandlerMethodFilter (解析获取MVC的HandlerMethod)
🡓
SecurityFilter (认证授权)
    🡓
    AuthenticationFilter
    🡓
    LoginFilter
    🡓
    LogoutFilter
    🡓
    AuthorizationFilter
    🡓
🡓
StaticResourceFilter (静态资源映射)
🡓
MvcFilter (before[可提前响应请求] -> invokeMethod[自定义MVC: 响应请求] -> after -> finally[执行before后一定会执行, 可以处理异常])
    🡓
    HandlerInterceptor
        🡓
        ArgumentsValidated (MVC数据验证@Validated)
        🡓
        TransactionInterceptor (JDBC事务处理)
        🡓
    🡓
🡓
```

内置函数式MVC支持的注解:

| 内置注解                                          | 兼容Spring注解                                                 | 用途                      |
|:----------------------------------------------|:-----------------------------------------------------------|:------------------------|
| `org.clever.web.mvc.annotation.RequestBody`   | `org.springframework.web.bind.annotation.RequestBody`      | 读取请求body参数              |
| `org.clever.web.mvc.annotation.RequestParam`  | `org.springframework.web.bind.annotation.RequestParam`     | 读取请求查询字符串或表单参数          |
| `org.clever.web.mvc.annotation.RequestPart`   | `org.springframework.web.bind.annotation.RequestPart`      | 读取请求文件上传参数              |
| `org.clever.web.mvc.annotation.RequestHeader` | `org.springframework.web.bind.annotation.RequestHeader`    | 读取HTTP请求头参数             |
| `org.clever.web.mvc.annotation.CookieValue`   | `org.springframework.web.bind.annotation.CookieValue`      | 读取HTTP cookie参数         |
| `org.clever.web.mvc.annotation.Validated`     | `org.springframework.validation.annotation.Validated`      | 启用请求参数验证(支持JSR 303验证注解) |
| `org.clever.web.mvc.annotation.Transactional` | `org.springframework.transaction.annotation.Transactional` | 设置JDBC数据源事务             |

#### 安全认证配置

```yaml
web:
    security:
        enable: false
        ignore-paths:
            - '/favicon.ico'
            - '/dist/**'
            - '/static/**'
            - '/**/*.png'
            - '/**/*.ico'
            - '/**/*.js'
            - '/**/*.css'
            - '/**/*.html'
            - '/**/*.ts'
            - '/**/*.tsx'
            - '/**/*.map'
            - '/**/*.png'
            - '/**/*.jpg'
            - '/**/*.gif'
            - '/**/*.ttf'
            - '/**/*.woff'
            - '/**/*.woff2'
            - '/**/.git'
        ignore-auth-paths: [ ]
        ignore-auth-failed-paths: [ ]
        current-user-path: '/current_user'
        login:
            paths: [ '/login' ]
            post-only: true
            allow-repeat-login: true
            concurrent-login-count: 1
            allow-after-login: true
        logout:
            path: '/logout'
        token:
            secret-key: 'security'
            token-validity: 30d
            jwt-token-name: 'authorization'
            refresh-token-name: 'refresh-token'
        data-source:
            jdbc-name: "mysql"
            enable-redis: true
            redis-name: "default"
            redis-namespace: "security"
```

认证授权过滤器链：

```text
🡓
AuthenticationFilter - 身份认证拦截 (读取请求token -> 使用刷新token(RefreshJwtToken) -> 验证token(VerifyJwtToken) -> 加载SecurityContext并绑定到当前线程(SecurityContextRepository))
🡓
LoginFilter          - 登录拦截 (收集登录数据(LoginDataCollect) -> 校验登录数据(VerifyLoginData) -> 加载用户信息(LoadUser) -> 校验用户信息(VerifyUserInfo) -> 创建token(AddJwtTokenExtData) -> 缓存SecurityContext(SecurityContextRepository))
🡓
LogoutFilter         - 登出拦截 (删除Token)
🡓
AuthorizationFilter  - 权限授权拦截 (获取SecurityContext -> 授权投票器开始投票(自定义) -> 根据投票结果判断是否授权通过)
    AuthorizationVoter(授权投票器)
🡓
```

#### 分布式定时任务配置

```yaml
timed-task:
    enable: true
    standby: true
    jdbc-name: 'postgresql'
    namespace: 'default'
    instance-name: 'node01'
    description: 'dev节点01'
    heartbeat-interval: 10000
    scheduler-executor-pool-size: 16
    job-executor-pool-size: 64
    job-executor-queue-size: 64
    load-weight: 1.0
    shell-job-working-dir: './shell_job_log'
    log-retention: 3d
    js-executor:
        engine-pool:
            max-idle: 8
            min-idle: 0
            max-total: 8
            max-wait: 30s
```
