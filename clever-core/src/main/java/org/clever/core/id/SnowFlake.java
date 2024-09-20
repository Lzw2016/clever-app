package org.clever.core.id;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.StringPool;
import org.clever.core.SystemClock;
import org.clever.core.Assert;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadLocalRandom;

/**
 * twitter的snowflake算法 -- java实现
 * 雪花算法简单描述：
 * 最高位是符号位，始终为0，不可用。
 * 41位的时间序列，精确到毫秒级，41位的长度可以使用69年。时间位还有一个很重要的作用是可以根据时间进行排序。
 * 10位的机器标识，10位的长度最多支持部署1024个节点。
 * 12位的计数序列号，序列号即一系列的自增id，可以支持同一节点同一毫秒生成多个ID序号，12位的计数序列号支持每个节点每毫秒产生4096个ID序号。
 * <p>
 * 作者：LiZW <br/>
 * 创建时间：2016-5-8 16:14 <br/>
 */
@Slf4j
public class SnowFlake {
    /**
     * SnowFlake 全局单例，“数据中心ID”和“机器号ID”都是0
     */
    public static final SnowFlake SNOW_FLAKE = new SnowFlake(0L, 0L);

    /**
     * 时间起始标记点，作为基准，一般取系统的最近时间（一旦确定不能变动）<br/>
     * 1291046400000L -> 2010-11-30 00:00:00
     */
    private final static long START_STAMP = 1291046400000L;
    /**
     * 机器标识位数
     */
    private final static long DATA_CENTER_BIT = 5L;
    private final static long DATACENTER_ID_BITS = 5L;
    public final static long MAX_WORKER_ID = ~(-1L << DATA_CENTER_BIT);
    public final static long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    /**
     * 毫秒内自增位
     */
    private final static long SEQUENCE_BITS = 12L;
    private final static long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private final static long DATACENTER_ID_SHIFT = SEQUENCE_BITS + DATA_CENTER_BIT;
    /**
     * 时间戳左移动位
     */
    private final static long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + DATA_CENTER_BIT + DATACENTER_ID_BITS;
    private final static long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 工作机器 ID
     */
    private final long workerId;
    /**
     * 数据中心 ID
     */
    private final long datacenterId;
    /**
     * 并发控制
     */
    private long sequence = 0L;
    /**
     * 上次生产 ID 时间戳
     */
    private long lastTimestamp = -1L;

    public SnowFlake() {
        this.datacenterId = getDatacenterId(MAX_DATACENTER_ID);
        this.workerId = getMaxWorkerId(datacenterId, MAX_WORKER_ID);
    }

    /**
     * 有参构造器
     *
     * @param workerId     工作机器 ID
     * @param datacenterId 数据中心ID
     */
    public SnowFlake(long workerId, long datacenterId) {
        Assert.isFalse(
                workerId > MAX_WORKER_ID || workerId < 0,
                String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID)
        );
        Assert.isFalse(
                datacenterId > MAX_DATACENTER_ID || datacenterId < 0,
                String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID)
        );
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 获取 maxWorkerId
     */
    @SuppressWarnings("SameParameterValue")
    protected static long getMaxWorkerId(long datacenterId, long maxWorkerId) {
        StringBuilder mPid = new StringBuilder();
        mPid.append(datacenterId);
        String name = ManagementFactory.getRuntimeMXBean().getName();
        if (StringUtils.isNotBlank(name)) {
            // GET jvmPid
            mPid.append(name.split(StringPool.AT)[0]);
        }
        // MAC + PID 的 hashcode 获取16个低位
        return (mPid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    /**
     * 数据标识id部分
     */
    @SuppressWarnings("SameParameterValue")
    protected static long getDatacenterId(long maxDatacenterId) {
        long id = 0L;
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                id = 1L;
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    id = ((0x000000FF & (long) mac[mac.length - 2]) | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
                    id = id % (maxDatacenterId + 1);
                }
            }
        } catch (Exception e) {
            log.warn(" getDatacenterId: " + e.getMessage());
        }
        return id;
    }

    /**
     * 获取下一个 ID
     *
     * @return 下一个 ID
     */
    public synchronized long nextId() {
        long timestamp = timeGen();
        //闰秒
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (offset <= 5) {
                try {
                    wait(offset << 1);
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
            }
        }
        if (lastTimestamp == timestamp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 同一毫秒的序列数已经达到最大
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号置为 1 - 3 随机数
            sequence = ThreadLocalRandom.current().nextLong(1, 3);
        }
        lastTimestamp = timestamp;
        // 时间戳部分 | 数据中心部分 | 机器标识部分 | 序列号部分
        return ((timestamp - START_STAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            Thread.yield();
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return SystemClock.now();
    }
}
