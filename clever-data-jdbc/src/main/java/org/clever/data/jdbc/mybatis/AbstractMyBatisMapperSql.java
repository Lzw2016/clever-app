package org.clever.data.jdbc.mybatis;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.SystemClock;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.dynamic.sql.DynamicSqlParser;
import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.data.dynamic.sql.parsing.XNode;
import org.clever.data.dynamic.sql.parsing.XPathParser;
import org.clever.data.dynamic.sql.parsing.xml.XMLMapperEntityResolver;
import org.clever.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXParseException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/09/30 15:37 <br/>
 */
public abstract class AbstractMyBatisMapperSql implements MyBatisMapperSql {
    protected static final Logger log = LoggerFactory.getLogger(MyBatisMapperSql.class);
    /**
     * 所有的mybatis动态sql信息 {@code Map<stdXmlPath, SqlSourceGroup>}
     * <pre>{@code
     * Map<stdXmlPath, SqlSourceGroup>
     * SqlSourceGroup
     *     stdSqlSource Map<sqlId, SqlSource>
     *     projectMap   Map<project, Map<sqlId, SqlSource>>
     *     dbTypeMap    Map<dbType, Map<sqlId, SqlSource>>
     * 项目方言 sql.xml / sql.mysql.xml / sql.projectA.xml
     * .../aaa/bbb
     *     - sql.xml
     *     - sql.mysql.xml
     *     - sql.oracle.xml
     *     - sql.sqlserver.xml
     *     - sql.projectA.xml
     *     - sql.projectB.xml
     *     - sql.projectC.xml
     * }</pre>
     */
    private final ConcurrentMap<String, SqlSourceGroup> allSqlSourceGroupMap = new ConcurrentHashMap<>(512);

    protected SqlSourceGroup getSqlSourceGroup(final String stdXmlPath) {
        return allSqlSourceGroupMap.get(FilenameUtils.normalize(stdXmlPath, true));
    }

    protected void putSqlSourceGroup(final String stdXmlPath, SqlSourceGroup sqlSourceGroup) {
        allSqlSourceGroupMap.put(FilenameUtils.normalize(stdXmlPath, true), sqlSourceGroup);
    }

    @Override
    public SqlSource getSqlSource(final String sqlId, final String stdXmlPath, final DbType dbType, String... projects) {
        if (projects == null) {
            projects = new String[0];
        }
        SqlSourceGroup sqlSourceGroup = getSqlSourceGroup(stdXmlPath);
        if (sqlSourceGroup == null) {
            synchronized (allSqlSourceGroupMap) {
                sqlSourceGroup = getSqlSourceGroup(stdXmlPath);
                if (sqlSourceGroup == null) {
                    final String stdXmlPathNoExt = FilenameUtils.removeExtension(stdXmlPath);
                    final List<String> xmlPaths = new ArrayList<>(projects.length + 2);
                    xmlPaths.add(stdXmlPath);
                    xmlPaths.add(String.format("%s.%s.xml", stdXmlPathNoExt, dbType.getDb()));
                    for (String project : projects) {
                        if (StringUtils.isBlank(project)) {
                            continue;
                        }
                        xmlPaths.add(String.format("%s.%s.xml", stdXmlPathNoExt, StringUtils.trim(project)));
                    }
                    // 文件存在才创建 SqlSourceGroup
                    if (xmlPaths.stream().anyMatch(this::fileExists)) {
                        sqlSourceGroup = new SqlSourceGroup();
                        putSqlSourceGroup(stdXmlPath, sqlSourceGroup);
                    }
                }
            }
            if (sqlSourceGroup == null) {
                return null;
            }
        }
        SqlSource sqlSource = sqlSourceGroup.getSqlSource(sqlId, dbType, projects);
        if (sqlSource == null) {
            loadSqlSourceGroup(sqlSourceGroup, sqlId, stdXmlPath, dbType, projects);
            // 加载sql -> stdXmlPath, dbType, projects
            sqlSource = sqlSourceGroup.getSqlSource(sqlId, dbType, projects);
        }
        return sqlSource;
    }

    protected void loadSqlSourceGroup(final SqlSourceGroup sqlSourceGroup, final String sqlId, final String stdXmlPath, final DbType dbType, String... projects) {
        String extName = FilenameUtils.getExtension(stdXmlPath);
        Assert.isTrue("xml".equals(extName), String.format("sql.xml文件后缀名必须是“.xml”：%s", stdXmlPath));
        // 按照优先级加载 SqlSource
        final String stdXmlPathNoExt = FilenameUtils.removeExtension(stdXmlPath);
        // 项目优先级
        for (String project : projects) {
            if (StringUtils.isBlank(project)) {
                continue;
            }
            String projectXmlPath = String.format("%s.%s.xml", stdXmlPathNoExt, StringUtils.trim(project));
            if (fileExists(projectXmlPath)) {
                Map<String, SqlSource> sqlSourceMap = Collections.emptyMap();
                try (InputStream inputStream = openInputStream(projectXmlPath)) {
                    sqlSourceMap = loadSqlSource(inputStream, getAbsolutePath(projectXmlPath)).getValue1();
                    sqlSourceGroup.clearAndSetProjectMap(project, sqlSourceMap);
                } catch (Exception ignored) {
                }
                if (sqlSourceMap.containsKey(sqlId)) {
                    return;
                }
            }
        }
        // 数据库优先级
        final String dbTypeXmlPath = String.format("%s.%s.xml", stdXmlPathNoExt, dbType.getDb());
        if (fileExists(dbTypeXmlPath)) {
            Map<String, SqlSource> sqlSourceMap = Collections.emptyMap();
            try (InputStream inputStream = openInputStream(dbTypeXmlPath)) {
                sqlSourceMap = loadSqlSource(inputStream, getAbsolutePath(dbTypeXmlPath)).getValue1();
                sqlSourceGroup.clearAndSetDbTypeMap(dbType, sqlSourceMap);
            } catch (Exception ignored) {
            }
            if (sqlSourceMap.containsKey(sqlId)) {
                return;
            }
        }
        // 标准SQL文件
        if (fileExists(stdXmlPath)) {
            try (InputStream inputStream = openInputStream(stdXmlPath)) {
                Map<String, SqlSource> sqlSourceMap = loadSqlSource(inputStream, getAbsolutePath(stdXmlPath)).getValue1();
                sqlSourceGroup.clearAndSetStdSqlSource(sqlSourceMap);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 加载指定文件
     *
     * @param inputStream  文件输入流
     * @param absolutePath 文件绝对路径
     * @return {@code TupleTwo<Map<sqlId, SqlSource>, 是否存在异常>}
     */
    protected TupleTwo<Map<String, SqlSource>, Boolean> loadSqlSource(InputStream inputStream, String absolutePath) {
        final Map<String, SqlSource> sqlSourceMap = new HashMap<>();
        final TupleTwo<Map<String, SqlSource>, Boolean> tupleTwo = TupleTwo.creat(sqlSourceMap, false);
        try {
            final Properties variables = new Properties();
            final String xml = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            final XPathParser parser = new XPathParser(xml, false, variables, new XMLMapperEntityResolver());
            final XNode mapper = parser.evalNode("/mapper");
            if (mapper == null) {
                return tupleTwo;
            }
            final List<XNode> nodes = mapper.evalNodes("sql|select|insert|update|delete");
            if (nodes == null) {
                return tupleTwo;
            }
            for (XNode node : nodes) {
                final String name = node.getName();
                final String sqlId = node.getStringAttribute("id", "");
                if (StringUtils.isBlank(sqlId)) {
                    log.warn("# sql.xml文件<{}> SqlId为空,忽略该SQL | path={}", name, absolutePath);
                    continue;
                }
                try {
                    SqlSource sqlSource = DynamicSqlParser.parserSql(node);
                    if (sqlSourceMap.containsKey(sqlId)) {
                        log.warn("# SqlId重复(自动覆盖) | sqlId={} | path={}", sqlId, absolutePath);
                    }
                    sqlSourceMap.put(sqlId, sqlSource);
                    // log.debug("# SQL读取成功 | sqlId={} | path={}", sqlId, absolutePath);
                } catch (Exception ex) {
                    tupleTwo.setValue2(true);
                    log.warn("# 解析sql.xml文件失败 | sqlId={} | path={}", sqlId, absolutePath, ex);
                }
            }
        } catch (Exception e) {
            tupleTwo.setValue2(true);
            if (e.getCause() instanceof SAXParseException) {
                SAXParseException saxParseException = (SAXParseException) e.getCause();
                String error = String.format(
                        "#第%d行，第%d列存在错误: %s",
                        saxParseException.getLineNumber(),
                        saxParseException.getColumnNumber(),
                        saxParseException.getMessage()
                );
                log.error("# 解析sql.xml文件失败 | error={}", error);
            }
            log.error("# 解析sql.xml文件失败 | path={}", absolutePath, e);
        }
        return tupleTwo;
    }

    /**
     * 重新加载指定文件(文件被删除或者文件更新之后调用)
     *
     * @param xmlPath       sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”、“org/clever/biz/dao/UserDao.mysql.xml”
     * @param skipException 是否跳过异常
     */
    public void reloadFile(final String xmlPath, boolean skipException) {
        final boolean exists = fileExists(xmlPath);
        final String extName = FilenameUtils.getExtension(xmlPath);
        Assert.isTrue("xml".equals(extName), String.format("sql.xml文件后缀名必须是“.xml”：%s", xmlPath));
        final String name = FilenameUtils.getBaseName(xmlPath);
        Assert.isNotBlank(name, String.format("文件名不能为空：%s", xmlPath));
        final String path = FilenameUtils.getFullPath(xmlPath);
        final String stdName = StringUtils.split(name, '.')[0];
        final String stdXmlPath = FilenameUtils.concat(path, String.format("%s.xml", stdName));
        final String projectOrBbType = StringUtils.trim(name.substring(Math.min(stdName.length() + 1, name.length())));
        final DbType dbType = DbType.getDbType(projectOrBbType);
        SqlSourceGroup sqlSourceGroup = getSqlSourceGroup(stdXmlPath);
        if (exists && sqlSourceGroup == null) {
            synchronized (allSqlSourceGroupMap) {
                sqlSourceGroup = getSqlSourceGroup(stdXmlPath);
                if (sqlSourceGroup == null) {
                    sqlSourceGroup = new SqlSourceGroup();
                    putSqlSourceGroup(stdXmlPath, sqlSourceGroup);
                }
            }
        }
        if (!exists && sqlSourceGroup == null) {
            return;
        }
        if (exists) {
            // 文件存在
            Map<String, SqlSource> sqlSourceMap = null;
            boolean hasException = false;
            try (InputStream inputStream = openInputStream(xmlPath)) {
                TupleTwo<Map<String, SqlSource>, Boolean> tupleTwo = loadSqlSource(inputStream, getAbsolutePath(xmlPath));
                sqlSourceMap = tupleTwo.getValue1();
                hasException = tupleTwo.getValue2();
            } catch (Exception ignored) {
            }
            if (sqlSourceMap != null && (!hasException || skipException)) {
                if (dbType != null) {
                    sqlSourceGroup.clearAndSetDbTypeMap(dbType, sqlSourceMap);
                } else if (StringUtils.isNotBlank(projectOrBbType)) {
                    sqlSourceGroup.clearAndSetProjectMap(projectOrBbType, sqlSourceMap);
                } else {
                    sqlSourceGroup.clearAndSetStdSqlSource(sqlSourceMap);
                }
            }
        } else {
            // 文件被删除
            if (dbType != null) {
                sqlSourceGroup.removeDbTypeMap(dbType);
            } else if (StringUtils.isNotBlank(projectOrBbType)) {
                sqlSourceGroup.removeProjectMap(projectOrBbType);
            } else {
                sqlSourceGroup.clearStdSqlSource();
            }
        }
    }

    /**
     * 获取文件的绝对路径
     *
     * @param xmlPath sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”、“org/clever/biz/dao/UserDao.mysql.xml”
     */
    public abstract String getAbsolutePath(String xmlPath);

    /**
     * 文件是否存在
     *
     * @param xmlPath sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”、“org/clever/biz/dao/UserDao.mysql.xml”
     */
    public abstract boolean fileExists(String xmlPath);

    /**
     * 打开文件流
     *
     * @param xmlPath sql.xml文件同路径，如：“org/clever/biz/dao/UserDao.xml”、“org/clever/biz/dao/UserDao.mysql.xml”
     */
    public abstract InputStream openInputStream(String xmlPath);

    /**
     * 加载所有文件
     */
    public abstract void reloadAll();

    /**
     * 初始化加载所有配置
     */
    protected void initLoad() {
        log.info("# ==========================================================================");
        log.info("# === 初始化读取sql.xml文件 ===");
        long startTime = SystemClock.now();
        reloadAll();
        long endTime = SystemClock.now();
        log.info("# === 读取sql.xml文件完成 | 耗时: {}ms ===", (endTime - startTime));
        log.info("# ==========================================================================");
    }
}
