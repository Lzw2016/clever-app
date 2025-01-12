package org.clever.core;

import org.clever.core.random.RandomUtil;
import org.clever.core.validator.ValidatorUtils;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TreeSet;

/**
 * 网络相关工具
 * <p>
 * 作者： lzw<br/>
 * 创建时间：2019-08-18 16:29 <br/>
 */
public class NetUtil {
    public final static String LOCAL_IP = "127.0.0.1";
    /**
     * 默认最小端口，1024
     */
    public static final int PORT_RANGE_MIN = 1024;
    /**
     * 默认最大端口，65535
     */
    public static final int PORT_RANGE_MAX = 0xFFFF;

    /**
     * 根据long值获取ip v4地址
     *
     * @param longIP IP的long表示形式
     * @return IP V4 地址
     */
    public static String longToIpv4(long longIP) {
        // 直接右移24位
        return (longIP >>> 24)
            + "."
            // 将高8位置0，然后右移16位
            + ((longIP & 0x00FFFFFF) >>> 16)
            + "."
            + ((longIP & 0x0000FFFF) >>> 8)
            + "."
            + (longIP & 0x000000FF);
    }

    /**
     * 根据ip地址计算出long型的数据
     *
     * @param strIP IP V4 地址
     * @return long值
     */
    public static long ipv4ToLong(String strIP) {
        if (ValidatorUtils.isIpv4(strIP)) {
            long[] ip = new long[4];
            // 先找到IP地址字符串中.的位置
            int position1 = strIP.indexOf(".");
            int position2 = strIP.indexOf(".", position1 + 1);
            int position3 = strIP.indexOf(".", position2 + 1);
            // 将每个.之间的字符串转换成整型
            ip[0] = Long.parseLong(strIP.substring(0, position1));
            ip[1] = Long.parseLong(strIP.substring(position1 + 1, position2));
            ip[2] = Long.parseLong(strIP.substring(position2 + 1, position3));
            ip[3] = Long.parseLong(strIP.substring(position3 + 1));
            return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
        }
        return 0;
    }

    /**
     * 是否为有效的端口<br>
     * 此方法并不检查端口是否被占用
     *
     * @param port 端口号
     * @return 是否有效
     */
    public static boolean isValidPort(int port) {
        // 有效端口是0～65535
        return port >= 0 && port <= PORT_RANGE_MAX;
    }

    /**
     * 检测本地端口可用性<br>
     * 来自org.springframework.util.SocketUtils
     *
     * @param port 被检测的端口
     * @return 是否可用
     */
    public static boolean isUsableLocalPort(int port) {
        if (!isValidPort(port)) {
            // 给定的IP未在指定端口范围中
            return false;
        }
        try {
            ServerSocketFactory.getDefault().createServerSocket(port, 1, InetAddress.getByName(LOCAL_IP)).close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 查找指定范围内的可用端口<br>
     * 此方法只检测给定范围内的随机一个端口，检测maxPort-minPort次<br>
     * 来自org.springframework.util.SocketUtils
     *
     * @param minPort 端口最小值（包含）
     * @param maxPort 端口最大值（包含）
     * @return 可用的端口
     * @since 4.5.4
     */
    public static int getUsableLocalPort(int minPort, int maxPort) {
        for (int i = minPort; i <= maxPort; i++) {
            int port = RandomUtil.randomInt(minPort, maxPort + 1);
            if (isUsableLocalPort(port)) {
                return port;
            }
        }
        throw new RuntimeException(String.format("Could not find an available port in the range [%s, %s] after %s attempts", minPort, maxPort, maxPort - minPort));
    }

    /**
     * 查找1024~65535范围内的可用端口<br>
     * 此方法只检测给定范围内的随机一个端口，检测65535-1024次<br>
     * 来自org.springframework.util.SocketUtils
     *
     * @return 可用的端口
     * @since 4.5.4
     */
    public static int getUsableLocalPort() {
        return getUsableLocalPort(PORT_RANGE_MIN);
    }

    /**
     * 查找指定范围内的可用端口，最大值为65535<br>
     * 此方法只检测给定范围内的随机一个端口，检测65535-minPort次<br>
     * 来自org.springframework.util.SocketUtils
     *
     * @param minPort 端口最小值（包含）
     * @return 可用的端口
     * @since 4.5.4
     */
    public static int getUsableLocalPort(int minPort) {
        return getUsableLocalPort(minPort, PORT_RANGE_MAX);
    }

    /**
     * 获取多个本地可用端口<br>
     * 来自org.springframework.util.SocketUtils
     *
     * @param numRequested 获取数量
     * @param minPort      端口最小值（包含）
     * @param maxPort      端口最大值（包含）
     * @return 可用的端口
     * @since 4.5.4
     */
    public static TreeSet<Integer> getUsableLocalPorts(int numRequested, int minPort, int maxPort) {
        final TreeSet<Integer> availablePorts = new TreeSet<>();
        int attemptCount = 0;
        while ((++attemptCount <= numRequested + 100) && availablePorts.size() < numRequested) {
            availablePorts.add(getUsableLocalPort(minPort, maxPort));
        }
        if (availablePorts.size() != numRequested) {
            throw new RuntimeException(String.format("Could not find %s available  ports in the range [%s, %s]", numRequested, minPort, maxPort));
        }
        return availablePorts;
    }

    /**
     * 通过域名得到IP
     *
     * @param hostName HOST
     * @return ip address or hostName if UnknownHostException
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }
}
