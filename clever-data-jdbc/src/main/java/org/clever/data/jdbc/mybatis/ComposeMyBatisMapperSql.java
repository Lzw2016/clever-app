package org.clever.data.jdbc.mybatis;

import org.clever.data.dynamic.sql.builder.SqlSource;
import org.clever.data.dynamic.sql.dialect.DbType;
import org.clever.util.Assert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/07 09:24 <br/>
 */
public class ComposeMyBatisMapperSql implements MyBatisMapperSql {
    /**
     * 真实的 MyBatisMapperSql
     */
    private final CopyOnWriteArrayList<MyBatisMapperSql> targets = new CopyOnWriteArrayList<>();

    public ComposeMyBatisMapperSql(List<MyBatisMapperSql> targets) {
        Assert.notNull(targets, "参数 targets 不能为空");
        this.targets.addAll(targets);
    }

    @Override
    public SqlSource getSqlSource(String sqlId, String stdXmlPath, DbType dbType, String... projects) {
        SqlSource sqlSource = null;
        for (MyBatisMapperSql target : targets) {
            sqlSource = target.getSqlSource(sqlId, stdXmlPath, dbType, projects);
            if (sqlSource != null) {
                break;
            }
        }
        return sqlSource;
    }

    @Override
    public void reloadFile(String xmlPath, boolean skipException) {
        for (MyBatisMapperSql target : targets) {
            target.reloadFile(xmlPath, skipException);
        }
    }

    @Override
    public void reloadAll() {
        for (MyBatisMapperSql target : targets) {
            target.reloadAll();
        }
    }

    @Override
    public void startWatch(int period) {
        for (MyBatisMapperSql target : targets) {
            target.startWatch(period);
        }
    }

    @Override
    public void stopWatch() {
        for (MyBatisMapperSql target : targets) {
            target.stopWatch();
        }
    }

    @Override
    public boolean isWatch() {
        boolean watch = false;
        for (MyBatisMapperSql target : targets) {
            watch = target.isWatch();
            if (watch) {
                break;
            }
        }
        return watch;
    }

    public void addMapperSql(MyBatisMapperSql myBatisMapperSql) {
        targets.add(myBatisMapperSql);
    }

    public void removeMapperSql(MyBatisMapperSql myBatisMapperSql) {
        targets.remove(myBatisMapperSql);
    }

    public void removeMapperSql(int index) {
        targets.remove(index);
    }
}
