//package org.clever.groovy
//
//import lombok.SneakyThrows
//import org.apache.commons.io.FileUtils
//import org.apache.commons.io.FilenameUtils
//import org.apache.commons.lang3.StringUtils
//import org.codehaus.groovy.runtime.InvokerHelper
//
//import java.nio.charset.StandardCharsets
//import java.util.concurrent.atomic.AtomicInteger
//
///**
// * 上架规则服务
// */
//class GroovyDSL  {
//    public static final String DEFAULT_CODE_BASE = "/groovy/shell";
//    private static final AtomicInteger EXECUTOR_COUNT = new AtomicInteger(0);
//
//    protected final HotReloadEngine engine = SpringContextHolder.getBean(HotReloadEngine.class);
//    protected final Jdbc jdbc = DaoFactory.getJdbc();
//    protected final Redis redis = RedisFactory.getRedis();
//    protected volatile Strategy strategy;
//    protected volatile long lastTimeStamp = 0;
//    protected volatile long fileLastModified = 0;
//    protected volatile String sha1;
//    protected final Object lock = new Object();
//    private final DaemonExecutor daemonWatch = new DaemonExecutor(String.format("rule-watch-%s", EXECUTOR_COUNT.incrementAndGet()));
//
//    @SneakyThrows
//    public File downloadCode() {
//        File file = new File("./script-repo/rule_temp/" + getFileName() + ".groovy");
//        FileUtils.writeStringToFile(file, getClassCode(), StandardCharsets.UTF_8);
//        log.info("class 代码文件下载成功: {}", FilenameUtils.normalize(file.getAbsolutePath(), true));
//        return file;
//    }
//
//    public String getClassCode() {
//        Assert.notNull(RULE_CONFIG, "全局配置 RULE_CONFIG 未设置");
//        if (RULE_CONFIG.isUseFileSystem()) {
//            return getClassCodeFromFile();
//        } else {
//            return getClassCodeFromDB();
//        }
//    }
//
//    /**
//     * 获取规则文件的文件名
//     */
//    public abstract String getFileName();
//
//    @SneakyThrows
//    public String getClassCodeFromFile() {
//        File file = new File(FilenameUtils.concat(RULE_CONFIG.getRuleFileDir(), getFileName() + ".groovy"));
//        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//    }
//
//    public String getClassCodeFromDB() {
//        throw new RuntimeException("不支持从数据库加载rule");
//    }
//
//    /**
//     * 同步代码
//     */
//    public TupleTwo<String, String> sync() {
//        String classCode = getClassCode();
//        String sha1 = EncodeDecodeUtils.encodeHex(DigestUtils.sha1(classCode.getBytes()));
//        redis.getRedisTemplate().opsForValue().set(getKey(), classCode);
//        redis.getRedisTemplate().opsForValue().set(getMD5Key(), sha1);
//        return TupleTwo.creat(classCode, sha1);
//    }
//
//    protected String getKey() {
//        return String.format("rule:groovy:%s", getFileName());
//    }
//
//    protected String getMD5Key() {
//        return String.format("rule:groovy:%s-md5", getFileName());
//    }
//
//    protected void watchFile() {
//        File file = new File(FilenameUtils.concat(RULE_CONFIG.getRuleFileDir(), getFileName() + ".groovy"));
//        long lastModified = file.lastModified();
//        if (!Objects.equals(fileLastModified, lastModified)) {
//            sync();
//            this.fileLastModified = lastModified;
//            log.info("规则发生变化: {}", getFileName());
//        }
//    }
//
//    protected StrategyContext doRun() {
//        final long maxTime = RULE_CONFIG.getInterval().toMillis();
//        final boolean needReload = RULE_CONFIG.isWatcher() && (SystemClock.now() - lastTimeStamp) > maxTime;
//        if (strategy == null || needReload) {
//            synchronized (lock) {
//                if (strategy == null || needReload) {
//                    // log.info("### AAA");
//                    String redisSha1 = redis.getRedisTemplate().opsForValue().get(getMD5Key());
//                    String scriptText;
//                    if (StringUtils.isBlank(redisSha1) || !Objects.equals(redisSha1, sha1)) {
//                        if (lastTimeStamp <= 0) {
//                            scriptText = getClassCode();
//                            if (RULE_CONFIG.isWatcher() && RULE_CONFIG.isUseFileSystem()) {
//                                daemonWatch.scheduleAtFixedRate(this::watchFile, 100);
//                            }
//                        } else {
//                            scriptText = redis.getRedisTemplate().opsForValue().get(getKey());
//                        }
//                        // log.info("@@@ BBB");
//                        if (StringUtils.isNotBlank(scriptText)) {
//                            TupleTwo<String, String> tupleTwo = sync();
//                            scriptText = tupleTwo.getValue1();
//                            sha1 = tupleTwo.getValue2();
//                        }
//                        GroovyClassLoader loader = engine.getGroovyClassLoader();
//                        GroovyCodeSource source = new GroovyCodeSource(scriptText, getFileName(), DEFAULT_CODE_BASE);
//                        Binding binding = new Binding();
//                        Class<?> clazz = loader.parseClass(source, true);
//                        Script script = InvokerHelper.createScript(clazz, binding);
//                        Object res = InvokerHelper.invokeMethod(script, "run", new Object[0]);
//                        if (!(res instanceof Strategy)) {
//                            throw new RuleException("规则定义错误");
//                        }
//                        strategy = (Strategy) res;
//                    }
//                    lastTimeStamp = SystemClock.now();
//                }
//            }
//        }
//        return strategy.runStrategy();
//    }
//
//    @SuppressWarnings("rawtypes")
//    @SneakyThrows
//    public static List run(String fileName) {
//        HotReloadEngine engine = SpringContextHolder.getBean(HotReloadEngine.class);
//        File file = new File("./script-repo/rule_temp/" + fileName + ".groovy");
//        String scriptText = FileUtils.readFileToString(file, "UTF-8");
//        GroovyClassLoader loader = engine.getGroovyClassLoader();
//        GroovyCodeSource source = new GroovyCodeSource(scriptText, fileName, DEFAULT_CODE_BASE);
//        Binding binding = new Binding();
//        Class<?> clazz = loader.parseClass(source, true);
//        Script script = InvokerHelper.createScript(clazz, binding);
//        Object res = InvokerHelper.invokeMethod(script, "run", new Object[0]);
//        if (!(res instanceof Strategy)) {
//            throw new RuleException("规则定义错误");
//        }
//        Strategy strategy = (Strategy) res;
//        StrategyContext context = strategy.runStrategy();
//        return context.getResult();
//    }
//}
