package org.clever.task.core;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import lombok.extern.slf4j.Slf4j;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.TaskScheduler;
import org.clever.util.Assert;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.clever.task.core.model.query.QTaskScheduler.taskScheduler;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/02 09:39 <br/>
 */
@Slf4j
public class TaskStoreTest {
    @Test
    public void t01() {
        Jdbc jdbc = BaseTest.newMysql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        Date date = queryDSL.select(Expressions.currentTimestamp()).fetchFirst();
        log.info("--> {}", date);
        jdbc.close();
    }

    @Test
    public void t02() {
        Jdbc jdbc = BaseTest.newMysql();
        // Jdbc jdbc = BaseTest.newPostgresql();
        Date date = jdbc.currentDate();
        log.info("--> {}", date);
        jdbc.close();
    }

    @Test
    public void t03() {
        Jdbc jdbc = BaseTest.newMysql();
        // Jdbc jdbc = BaseTest.newPostgresql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        final Date now = jdbc.currentDate();
        List<TaskScheduler> list = queryDSL.select(taskScheduler)
                .from(taskScheduler)
                .where(taskScheduler.namespace.eq("namespace"))
                .where(taskScheduler.lastHeartbeatTime.isNotNull())
                // heartbeat_interval * 2 > now - last_heartbeat_time --> heartbeat_interval * 2 + last_heartbeat_time > now
                .where(taskScheduler.heartbeatInterval.multiply(2).add(taskScheduler.lastHeartbeatTime).gt(now.getTime()))
                .fetch();
        log.info("--> {}", list);
        jdbc.close();
    }

    @Test
    public void t04() {
        Jdbc jdbc = BaseTest.newMysql();
        // Jdbc jdbc = BaseTest.newPostgresql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        final Date now = jdbc.currentDate();
        BooleanExpression available = taskScheduler.heartbeatInterval.multiply(2).add(taskScheduler.lastHeartbeatTime).gt(now.getTime()).as("available");
        List<Tuple> list = queryDSL.select(taskScheduler, available)
                .from(taskScheduler)
                .where(taskScheduler.namespace.eq("namespace"))
                .fetch();
        List<SchedulerInfo> infos = list.stream().map(tuple -> {
            TaskScheduler scheduler = tuple.get(taskScheduler);
            Assert.notNull(scheduler, "scheduler 不能为 null, 未知的错误");
            SchedulerInfo info = new SchedulerInfo();
            info.setId(scheduler.getId());
            info.setNamespace(scheduler.getNamespace());
            info.setInstanceName(scheduler.getInstanceName());
            info.setAvailable(tuple.get(available));
            info.setLastHeartbeatTime(scheduler.getLastHeartbeatTime());
            info.setHeartbeatInterval(scheduler.getHeartbeatInterval());
            info.setConfig(scheduler.getConfig());
            info.setDescription(scheduler.getDescription());
            info.setCreateAt(scheduler.getCreateAt());
            info.setUpdateAt(scheduler.getUpdateAt());
            return info;
        }).collect(Collectors.toList());
        log.info("--> {}", infos);
        jdbc.close();
    }
}
