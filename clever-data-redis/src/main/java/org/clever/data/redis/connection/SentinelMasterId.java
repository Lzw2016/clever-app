package org.clever.data.redis.connection;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

/**
 * 简单的 {@link NamedNode}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:27 <br/>
 */
class SentinelMasterId implements NamedNode {
    private final String name;

    public SentinelMasterId(String name) {
        Assert.hasText(name, "Sentinel Master Id must not be null or empty");
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SentinelMasterId)) {
            return false;
        }
        SentinelMasterId that = (SentinelMasterId) o;
        return ObjectUtils.nullSafeEquals(name, that.name);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(name);
    }
}
