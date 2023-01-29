package org.clever.data.redis.connection.stream;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 表示使用者组中的流使用者的值对象。组名和使用者名编码为密钥
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 22:13 <br/>
 */
public class Consumer {
    private final String group;
    private final String name;

    private Consumer(String group, String name) {
        this.group = group;
        this.name = name;
    }

    /**
     * 创建一个新的消费者
     *
     * @param group 消费者组的名称，不能为 {@literal null} 或为空
     * @param name  消费者名称，不能为 {@literal null} 或为空
     * @return 消费者 {@link io.lettuce.core.Consumer} 对象
     */
    public static Consumer from(String group, String name) {
        Assert.hasText(group, "Group must not be null");
        Assert.hasText(name, "Name must not be null");
        return new Consumer(group, name);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", group, name);
    }

    public String getGroup() {
        return this.group;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Consumer consumer = (Consumer) o;
        if (!ObjectUtils.nullSafeEquals(group, consumer.group)) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(name, consumer.name);
    }

    @Override
    public int hashCode() {
        int result = ObjectUtils.nullSafeHashCode(group);
        result = 31 * result + ObjectUtils.nullSafeHashCode(name);
        return result;
    }
}
