package org.clever.task.core;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Ops;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimeExpression;
import com.querydsl.core.types.dsl.Expressions;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Assert;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.QueryDSL;
import org.clever.task.core.model.EnumConstant;
import org.clever.task.core.model.SchedulerInfo;
import org.clever.task.core.model.entity.TaskScheduler;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.clever.task.core.model.query.QTaskJobTrigger.taskJobTrigger;
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
        // heartbeat_interval * 2 > now - last_heartbeat_time
        BooleanExpression whereCondition = taskScheduler.heartbeatInterval.multiply(2).gt(
            Expressions.numberOperation(
                Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, Expressions.currentTimestamp(), taskScheduler.lastHeartbeatTime
            ).multiply(1000)
        );
        List<TaskScheduler> list = queryDSL.select(taskScheduler)
            .from(taskScheduler)
            .where(taskScheduler.namespace.eq("namespace"))
            .where(taskScheduler.lastHeartbeatTime.isNotNull())
            .where(whereCondition)
            .fetch();
        log.info("--> {}", list);
        jdbc.close();
    }

    @Test
    public void t04() {
        Jdbc jdbc = BaseTest.newMysql();
        // Jdbc jdbc = BaseTest.newPostgresql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        // heartbeat_interval * 2 > now - last_heartbeat_time
        BooleanExpression available = taskScheduler.heartbeatInterval.multiply(2).gt(
            Expressions.numberOperation(
                Long.TYPE, Ops.DateTimeOps.DIFF_SECONDS, Expressions.currentTimestamp(), taskScheduler.lastHeartbeatTime
            ).multiply(1000)
        ).as("available");
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

    @Test
    public void t05() {
        Jdbc jdbc = BaseTest.newMysql();
        // Jdbc jdbc = BaseTest.newPostgresql();
        QueryDSL queryDSL = QueryDSL.create(jdbc);
        // final Date now = queryDSL.currentDate();
        // "  case " +
        // "    when isnull(last_fire_time) then date_add(start_time, interval fixed_interval second) " +
        // "    when timestampdiff(microsecond, last_fire_time, start_time)>=0 then date_add(start_time, interval fixed_interval second) " +
        // "    when timestampdiff(microsecond, last_fire_time, start_time)<0 then date_add(last_fire_time, interval fixed_interval second) " +
        // "    else date_add(now(), interval fixed_interval second) " +
        // "  end " +
        DateTimeExpression<Date> nextFireTime = Expressions.cases()
            .when(taskJobTrigger.lastFireTime.isNull()) //
            .then(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, taskJobTrigger.startTime, taskJobTrigger.fixedInterval)) //
            //.when(Expressions.numberOperation(Long.class, Ops.DateTimeOps.DIFF_SECONDS, taskJobTrigger.lastFireTime, taskJobTrigger.startTime).goe(0)) //
            .when(taskJobTrigger.lastFireTime.goe(taskJobTrigger.startTime)) //
            .then(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, taskJobTrigger.startTime, taskJobTrigger.fixedInterval)) //
            .when(taskJobTrigger.lastFireTime.lt(taskJobTrigger.startTime)) //
            .then(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, taskJobTrigger.lastFireTime, taskJobTrigger.fixedInterval)) //
            .otherwise(Expressions.dateTimeOperation(Date.class, Ops.DateTimeOps.ADD_SECONDS, Expressions.currentTimestamp(), taskJobTrigger.fixedInterval));
        queryDSL.update(taskJobTrigger)
            .set(taskJobTrigger.nextFireTime, nextFireTime)
            .where(taskJobTrigger.disable.eq(EnumConstant.JOB_TRIGGER_DISABLE_0))
            .where(taskJobTrigger.type.eq(EnumConstant.JOB_TRIGGER_TYPE_2))
            .where(taskJobTrigger.fixedInterval.gt(0))
            // .where(taskJobTrigger.nextFireTime.isNotNull().or(taskJobTrigger.nextFireTime.ne(nextFireTime)))
            .where(taskJobTrigger.namespace.eq("namespace"))
            .execute();
        jdbc.close();
    }

    @Test
    public void t06() {
        Jdbc jdbc = BaseTest.newMysql();
        long t1 = System.currentTimeMillis();
        long t2 = jdbc.currentDate().getTime();
        long t4 = System.currentTimeMillis();
        long offset = (t2 - t1) - (t4 - t2);
        log.info("t2={} | t4={} | offset -> {}", t2, t4, offset);
        log.info("t2-t4={}", t2 - t4);
        jdbc.close();
    }
}
