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
- schemacrawler
- poi
- p6spy

支持JDK
- [OK] JDK8
- [  ] JDK11
- [  ] JDK17
- [  ] JDK21

### clever-spring
需要删除的关键字
- @Deprecated
- @since
- spring.
- springframework

通用类
- [OK] Ordered
- [OK] NamedThreadLocal
- [OK] NamedInheritableThreadLocal
- [OK] Constants
- [OK] InfrastructureProxy
- [OK] OrderComparator
- [OK] PriorityOrdered
- [  ]

工具类型
- [OK] NestedExceptionUtils
- [OK] NestedRuntimeException
- [OK] CollectionFactory
- [OK] ClassUtils
- [05] StringUtils
- [05] ConversionUtils
- [OK] LocaleContextHolder
- [OK] Assert
- [OK] CollectionUtils
- [OK] ConcurrentReferenceHashMap
- [OK] MultiValueMap
- [OK] MultiValueMapAdapter
- [OK] LinkedMultiValueMap
- [OK] NumberUtils
- [OK] ObjectUtils
- [OK] ReflectionUtils
- [OK] StringUtils
- [OK] StringValueResolver
- [OK] ResourceUtils
- [OK] PropertyPlaceholderHelper
- [OK] SystemPropertyUtils
- [OK] PathMatcher
- [OK] AntPathMatcher
- [OK] SingletonSupplier
- [OK] PatternMatchUtils
- [OK] LinkedCaseInsensitiveMap
- [OK] ConcurrentLruCache
- [  ]

运行时环境检测
- [OK] KotlinDetector
- [OK] NativeDetector
- [OK] LocaleContext
- [OK] TimeZoneAwareLocaleContext
- [OK] SimpleLocaleContext
- [OK] SimpleTimeZoneAwareLocaleContext
- [OK] 删除SpringProperties
- [  ]

注解扩展
```
元注解: 标注在另一个注解上的注解
模式注解: 声明组件在应用中扮演角色的注解
组合注解: 被一个或多个注解元标注的注解
注解的存在: 直接标注存在，被组合注解标注存在
属性别名: 显式别名、隐式别名、可传递的隐式别名
属性重写: 隐式重写、显式重写、可传递的重写
```
- [OK] AliasFor
- [OK] AnnotationFilter
- [OK] PackagesAnnotationFilter
- [OK] RepeatableContainers
- [OK]     StandardRepeatableContainers
- [OK]     ExplicitRepeatableContainer
- [OK]     NoRepeatableContainers
- [OK] MergedAnnotationSelector
- [OK] ValueExtractor
- [OK] SynthesizedAnnotation
- [OK] AttributeMethods
- [OK] AnnotationAttributes
- [OK] MergedAnnotation
- [OK] MergedAnnotations
- [OK] AbstractMergedAnnotation
- [OK] MissingMergedAnnotation
- [OK] MergedAnnotationCollectors
- [OK] MergedAnnotationsCollection
- [OK] AnnotationTypeMapping
- [OK] TypeMappedAnnotation
- [OK] TypeMappedAnnotations
- [OK] AnnotationsProcessor
- [OK] AnnotationsScanner
- [OK] AnnotationConfigurationException
- [OK] AnnotatedElementUtils
- [OK] AnnotationUtils
- [OK] BridgeMethodResolver
- [OK] IntrospectionFailureLogger
- [OK] MergedAnnotationPredicates
- [OK] MergedAnnotationSelectors
- [OK] SynthesizedMergedAnnotationInvocationHandler
- [  ]

类型反射
- [OK] ResolvableType
- [OK] ResolvableTypeProvider
- [OK] SerializableTypeWrapper
- [OK] MethodParameter
- [OK] GenericTypeResolver
- [OK] ParameterizedTypeReference
- [OK] ParameterNameDiscoverer
- [OK] PrioritizedParameterNameDiscoverer
- [OK] DefaultParameterNameDiscoverer
- [OK] StandardReflectionParameterNameDiscoverer
- [OK] LocalVariableTableParameterNameDiscoverer
- [OK] KotlinReflectionParameterNameDiscoverer
- [OK] TypeDescriptor
- [OK] AttributeAccessor
- [OK] AttributeAccessorSupport
- [  ]

IO流
- [OK] InputStreamSource
- [OK] Resource
- [OK] WritableResource
- [OK] ContextResource
- [OK] AbstractResource
- [OK] AbstractFileResolvingResource
- [OK] InputStreamResource
- [OK] ByteArrayResource
- [OK] FileSystemResource
- [OK] ClassPathResource
- [OK] VfsUtils
- [OK] VfsResource
- [OK] ClassPathResource
- [OK] UrlResource
- [OK] FileUrlResource
- [OK] ProtocolResolver
- [OK] ResourceLoader
- [OK] DefaultResourceLoader
- [OK] FileSystemResourceLoader
- [OK] ResourceEditor
- [OK] EncodedResource
- [OK] PathMatchingResourcePatternResolver
- [OK] ResourcePatternResolver
- [OK] VfsPatternUtils
- [  ]

应用环境
- [OK] ReadOnlySystemAttributesMap
- [OK] PropertyResolver
- [OK] Environment
- [OK] ConfigurablePropertyResolver
- [OK] AbstractPropertyResolver
- [OK] PropertySourcesPropertyResolver
- [OK] Profiles
- [OK] ProfilesParser
- [OK] PropertySource
- [OK] PropertySources
- [OK] MutablePropertySources
- [OK] EnumerablePropertySource
- [OK] MapPropertySource
- [OK] PropertiesPropertySource
- [OK] SystemEnvironmentPropertySource
- [OK] ConfigurableEnvironment
- [OK] AbstractEnvironment
- [OK] StandardEnvironment
Boot应用环境
- [OK] PropertySourceLoader
- [OK] PropertiesPropertySourceLoader
- [OK] YamlPropertySourceLoader
- [OK] OriginTrackedPropertiesLoader
- [  ]
Boot配置文件加载
- [OK] ConfigDataEnvironment
- [OK] LocationResourceLoader
- [OK] StandardConfigDataLocationResolver
- [OK] ConfigDataLocationResolvers
- [OK] ConfigDataImporter
- [  ]

基础类型转换
- [OK] Converter
- [OK] ConverterFactory
- [OK] GenericConverter
- [OK] ConditionalConverter
- [OK] ConditionalGenericConverter
- [OK] ConverterRegistry
- [OK] ConversionService
- [OK] ConfigurableConversionService
- [OK] GenericConversionService
- [OK]     Converters
- [OK]     ConverterCacheKey
- [OK]     ConvertersForPair
- [OK]     NoOpConverter
- [OK]     ConverterAdapter
- [OK]     ConverterFactoryAdapter
- [OK] DefaultConversionService
- [OK] Printer
- [OK] Parser
- [OK] Formatter
- [OK] AnnotationFormatterFactory
- [OK] FormatterRegistry
- [OK] FormattingConversionService
- [OK]     PrinterConverter
- [OK]     ParserConverter
- [OK]     AnnotationPrinterConverter
- [OK]     AnnotationParserConverter
- [OK]     AnnotationConverterKey
- [OK] DefaultFormattingConversionService
- [OK] Property
- [OK] ConverterNotFoundException
- [OK] ConversionFailedException
- [OK] ConversionException
- [OK] ConversionUtils
转换器实现
- [OK] AbstractConditionalEnumConverter
- [OK] ArrayToArrayConverter
- [OK] ArrayToCollectionConverter
- [OK] ArrayToObjectConverter
- [OK] ArrayToStringConverter
- [OK] ByteBufferConverter
- [OK] CharacterToNumberFactory
- [OK] CollectionToArrayConverter
- [OK] CollectionToStringConverter
- [OK] EnumToIntegerConverter
- [OK] EnumToStringConverter
- [OK] FallbackObjectToStringConverter
- [OK] IdToEntityConverter
- [OK] IntegerToEnumConverterFactory
- [OK] MapToMapConverter
- [OK] NumberToCharacterConverter
- [OK] NumberToNumberConverterFactory
- [OK] ObjectToArrayConverter
- [OK] ObjectToCollectionConverter
- [OK] ObjectToObjectConverter
- [OK] ObjectToOptionalConverter
- [OK] ObjectToStringConverter
- [OK] PropertiesToStringConverter
- [OK] StreamConverter
- [OK] StringToArrayConverter
- [OK] StringToBooleanConverter
- [OK] StringToCharacterConverter
- [OK] StringToCharsetConverter
- [OK] StringToCollectionConverter
- [OK] StringToCurrencyConverter
- [OK] StringToEnumConverterFactory
- [OK] StringToLocaleConverter
- [OK] StringToNumberConverterFactory
- [OK] StringToPropertiesConverter
- [OK] StringToTimeZoneConverter
- [OK] StringToUUIDConverter
- [OK] ZonedDateTimeToCalendarConverter
- [OK] ZoneIdToTimeZoneConverter
格式化实现
- [OK] NumberFormat
- [OK] DateTimeFormat
- [OK] AbstractNumberFormatter
- [OK] CurrencyStyleFormatter
- [OK] NumberFormatAnnotationFormatterFactory
- [OK] NumberStyleFormatter
- [OK] PercentStyleFormatter
- [OK] CurrencyUnitFormatter
- [OK] Jsr354NumberFormatAnnotationFormatterFactory
- [OK] MonetaryAmountFormatter
- [OK] DateFormatter
- [OK] FormatterRegistrar
- [OK] DateFormatterRegistrar
- [OK] DateTimeFormatAnnotationFormatterFactory
- [OK] DateTimeFormatterFactory
- [OK] DateTimeParser
- [OK] DurationFormatter
- [OK] JodaDateTimeFormatAnnotationFormatterFactory
- [OK] JodaTimeContext
- [OK] JodaTimeContextHolder
- [OK] JodaTimeConverters
- [OK] JodaTimeFormatterRegistrar
- [OK] LocalDateParser
- [OK] LocalDateTimeParser
- [OK] LocalTimeParser
- [OK] MillisecondInstantPrinter
- [OK] MonthDayFormatter
- [OK] PeriodFormatter
- [OK] ReadableInstantPrinter
- [OK] ReadablePartialPrinter
- [OK] YearMonthFormatter
- [OK] DateTimeContext
- [OK] DateTimeContextHolder
- [OK] DateTimeConverters
- [OK] DateTimeFormatterFactory
- [OK] DateTimeFormatterRegistrar
- [OK] DateTimeFormatterUtils
- [OK] DurationFormatter
- [OK] InstantFormatter
- [OK] Jsr310DateTimeFormatAnnotationFormatterFactory
- [OK] MonthDayFormatter
- [OK] MonthFormatter
- [OK] PeriodFormatter
- [OK] TemporalAccessorParser
- [OK] TemporalAccessorPrinter
- [OK] YearFormatter
- [OK] YearMonthFormatter

Bean类型转换
- [OK] BeansException
- [OK] FatalBeanException
- [OK] InvalidPropertyException
- [OK] BeanInstantiationException
- [OK] PropertyAccessException
- [OK] TypeMismatchException
- [OK] ConversionNotSupportedException
- [OK] MethodInvocationException
- [OK] NotReadablePropertyException
- [OK] NotWritablePropertyException
- [OK] NullValueInNestedPathException
- [OK] PropertyBatchUpdateException
- [OK] BeanInfo
- [OK] ExtendedBeanInfo
- [OK] BeanInfoFactory
- [OK] ExtendedBeanInfoFactory
- [OK] CachedIntrospectionResults
- [OK] GenericTypeAwarePropertyDescriptor
- [OK] Mergeable
- [OK] BeanMetadataElement
- [OK] BeanMetadataAttributeAccessor
- [OK] PropertyValue
- [OK] MutablePropertyValues
- [OK] PropertyAccessorUtils
- [OK] PropertyDescriptorUtils
- [OK] PropertyMatches
- [OK] PropertyValues
- [OK] TypeConverterDelegate
- [OK] PropertyEditorRegistry
- [OK] TypeConverter
- [OK] PropertyAccessor
- [OK] PropertyAccessorFactory
- [OK] ConfigurablePropertyAccessor
- [OK] BeanWrapper
- [OK] PropertyEditorRegistrySupport
- [OK] TypeConverterSupport
- [OK] AbstractPropertyAccessor
- [OK] AbstractNestablePropertyAccessor
- [OK] BeanWrapperImpl
- [OK] BeanUtils
editors
- [OK] CharsetEditor
- [OK] ClassEditor
- [OK] ClassArrayEditor
- [OK] CurrencyEditor
- [OK] FileEditor
- [OK] InputStreamEditor
- [OK] InputSourceEditor
- [OK] LocaleEditor
- [OK] PathEditor
- [OK] PatternEditor
- [OK] PropertiesEditor
- [OK] ReaderEditor
- [OK] ResourceArrayPropertyEditor
- [OK] TimeZoneEditor
- [OK] URIEditor
- [OK] URLEditor
- [OK] UUIDEditor
- [OK] ZoneIdEditor
- [OK] CustomCollectionEditor
- [OK] CustomMapEditor
- [OK] ByteArrayPropertyEditor
- [OK] CharArrayPropertyEditor
- [OK] CharacterEditor
- [OK] CustomBooleanEditor
- [OK] CustomNumberEditor
- [OK] StringArrayPropertyEditor

JDBC数据源事务管理器
- [OK] TransactionException
- [OK] TransactionUsageException
- [OK] InvalidTimeoutException
- [OK] IllegalTransactionStateException
- [OK] CannotCreateTransactionException
- [OK] NestedTransactionNotSupportedException
- [OK] UnexpectedRollbackException
- [OK] TransactionSuspensionNotSupportedException
- [OK] TransactionSystemException
- [OK] TransactionTimedOutException
- [  ]
- [OK] TransactionManager
- [OK] PlatformTransactionManager
- [OK] ResourceTransactionManager
- [OK] TransactionDefinition
- [OK] StaticTransactionDefinition
- [OK] DefaultTransactionDefinition
- [OK] TransactionExecution
- [OK] SavepointManager
- [OK] TransactionStatus
- [OK] AbstractTransactionStatus
- [OK] DefaultTransactionStatus
- [OK] SmartTransactionObject
- [OK] TransactionSynchronizationManager
- [OK] TransactionSynchronization
- [OK] TransactionSynchronizationUtils
- [OK] ResourceHolder
- [OK] AbstractPlatformTransactionManager
- [OK] ResourceHolderSupport
- [  ] TransactionTemplate
- [  ]
JDBC DataSource
- [OK] BadSqlGrammarException
- [OK] CannotGetJdbcConnectionException
- [OK] IncorrectResultSetColumnCountException
- [OK] InvalidResultSetAccessException
- [OK] SQLWarningException
- [OK] UncategorizedSQLException
- [OK] MetaDataAccessException
- [  ]
- [OK] ConnectionHandle
- [OK] ConnectionHolder
- [OK] DataSourceTransactionManager
- [OK] DataSourceUtils
- [OK] DelegatingDataSource
- [OK] JdbcTransactionObjectSupport
- [OK] TransactionAwareDataSourceProxy
- [OK] ConnectionProxy
- [OK] SmartDataSource
- [OK] SimpleConnectionHandle
- [  ]
- [OK] SqlRowSetMetaData
- [OK] SqlRowSet
- [OK] ResultSetWrappingSqlRowSet
- [OK] ResultSetWrappingSqlRowSetMetaData
- [OK] SqlValue
- [OK] SQLExceptionTranslator
- [OK] KeyHolder
- [OK] DatabaseMetaDataCallback
- [OK] AbstractFallbackSQLExceptionTranslator
- [OK] CustomSQLErrorCodesTranslation
- [OK] CustomSQLExceptionTranslatorRegistry
- [OK] JdbcAccessor
- [OK] JdbcUtils
- [OK] SQLErrorCodes
- [OK] SQLErrorCodesFactory
- [OK] SQLErrorCodeSQLExceptionTranslator
- [OK] SQLExceptionSubclassTranslator
- [OK] SQLStateSQLExceptionTranslator
- [  ]
- [OK] ArgumentPreparedStatementSetter
- [OK] ArgumentTypePreparedStatementSetter
- [OK] BatchPreparedStatementSetter
- [OK] CallableStatementCallback
- [OK] CallableStatementCreator
- [OK] ColumnMapRowMapper
- [OK] ConnectionCallback
- [OK] DisposableSqlTypeValue
- [OK] InterruptibleBatchPreparedStatementSetter
- [OK] JdbcOperations
- [OK] JdbcTemplate
- [OK] ParameterDisposer
- [OK] ParameterizedPreparedStatementSetter
- [OK] PreparedStatementCallback
- [OK] PreparedStatementCreator
- [OK] PreparedStatementCreatorFactory
- [OK] PreparedStatementSetter
- [OK] ResultSetExtractor
- [OK] ResultSetSupportingSqlParameter
- [OK] RowCallbackHandler
- [OK] RowMapper
- [OK] RowMapperResultSetExtractor
- [OK] SingleColumnRowMapper
- [OK] SqlOutParameter
- [OK] SqlParameter
- [OK] SqlParameterValue
- [OK] SqlProvider
- [OK] SqlReturnResultSet
- [OK] SqlReturnType
- [OK] SqlReturnUpdateCount
- [OK] SqlRowSetResultSetExtractor
- [OK] SqlTypeValue
- [OK] StatementCallback
- [OK] StatementCreatorUtils
- [  ]
- [OK] SqlParameterSource
- [OK] EmptySqlParameterSource
- [OK] AbstractSqlParameterSource
- [OK] BeanPropertySqlParameterSource
- [OK] MapSqlParameterSource
- [OK] NamedParameterJdbcOperations
- [OK] NamedParameterJdbcTemplate
- [OK] NamedParameterUtils
- [OK] ParsedSql
- [OK] SqlParameterSourceUtils
- [  ]
DAO数据访问层
- [OK] PersistenceExceptionTranslator
- [OK] DataAccessUtils
- [OK] CannotAcquireLockException
- [OK] CannotSerializeTransactionException
- [OK] ConcurrencyFailureException
- [OK] DataAccessException
- [OK] DataAccessResourceFailureException
- [OK] DataIntegrityViolationException
- [OK] DataRetrievalFailureException
- [OK] DeadlockLoserDataAccessException
- [OK] DuplicateKeyException
- [OK] EmptyResultDataAccessException
- [OK] IncorrectResultSizeDataAccessException
- [OK] InvalidDataAccessApiUsageException
- [OK] InvalidDataAccessResourceUsageException
- [OK] NonTransientDataAccessException
- [OK] NonTransientDataAccessResourceException
- [OK] PermissionDeniedDataAccessException
- [OK] PessimisticLockingFailureException
- [OK] QueryTimeoutException
- [OK] RecoverableDataAccessException
- [OK] TransientDataAccessException
- [OK] TransientDataAccessResourceException
- [OK] TypeMismatchDataAccessException
- [OK] UncategorizedDataAccessException
- [  ]

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
- [  ] 定时任务支持中断
- [OK] 数据完整性校验、一致性校验
- [  ] 支持控制任务执行节点：指定节点优先、固定节点白名单、固定节点黑名单
- [  ] 支持负载均衡策略：抢占、随机、轮询、一致性HASH
- [  ] 支持任务类型：java调用任务、Http任务、js脚本任务、shell脚本任务
- [OK] 定时任务调度平台API接口
- [OK] 定时任务调度平台Web页面
- [OK] 拆分管理控制台代码(区分定时任务核心代码与管理API代码)
- [OK] 内置日志数据清理任务
- [OK] 新建统计数据表，定时收集统计信息
- [  ] 保存http、java、js、shell任务的执行日志，方便控制台查询
- [OK] 对于 java、js 任务需要支持 JobContext
- [  ] 任何一个节点都能给其它节点发送指令，用于在web控制台操作调度器和执行定时任务
- [OK] 数据校验需要检查最后一条数据时间，如果其它节点已经运行了，就不运行了
- [  ] 

### clever-task-ext
未开始

### clever-js-api
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
