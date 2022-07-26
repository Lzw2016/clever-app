package org.clever.core.tuples;

import java.io.Serializable;
import java.util.*;

/**
 * 元组抽象类
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 10:53 <br/>
 */
public abstract class Tuple implements Iterable<Object>, Serializable, Comparable<Tuple> {

    private final Object[] valueArray;
    private final List<Object> valueList;

    protected Tuple(final Object... values) {
        super();
        this.valueArray = values;
        this.valueList = Arrays.asList(values);
    }

    /**
     * @return 元组的大小
     */
    public abstract int getSize();

    /**
     * 获取元组中特定位置的值
     *
     * @param index 位置，从0开始
     */
    public final Object getValue(final int index) {
        if (index >= getSize()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Cannot retrieve position %s in %s. Positions for this class start with 0 and end with %s",
                            index,
                            this.getClass().getSimpleName(),
                            (getSize() - 1)
                    )
            );
        }
        return this.valueArray[index];
    }

    /**
     * 获取遍历元组的迭代器
     */
    public final Iterator<Object> iterator() {
        return this.valueList.iterator();
    }

    /**
     * 元组中是否包含该对象
     */
    public final boolean contains(final Object value) {
        for (final Object val : this.valueList) {
            if (val == null) {
                if (value == null) {
                    return true;
                }
            } else {
                if (val.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 集合中的数据是否全部被包含在元组中
     */
    public final boolean containsAll(final Collection<?> collection) {
        for (final Object value : collection) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 元组中是否包含该对象
     */
    public final boolean containsAll(final Object... values) {
        if (values == null) {
            throw new IllegalArgumentException("Values array cannot be null");
        }
        for (final Object value : values) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 对象在元组中的位置(第一次出现的位置)
     */
    public final int indexOf(final Object value) {
        int i = 0;
        for (final Object val : this.valueList) {
            if (val == null) {
                if (value == null) {
                    return i;
                }
            } else {
                if (val.equals(value)) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    /**
     * 对象在元组中的位置(最后一次出现的位置)
     */
    public final int lastIndexOf(final Object value) {
        for (int i = getSize() - 1; i >= 0; i--) {
            final Object val = this.valueList.get(i);
            if (val == null) {
                if (value == null) {
                    return i;
                }
            } else {
                if (val.equals(value)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * 转换成集合
     */
    public final List<Object> toList() {
        return Collections.unmodifiableList(new ArrayList<>(this.valueList));
    }

    /**
     * 转换成数组
     */
    public final Object[] toArray() {
        return this.valueArray.clone();
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public int compareTo(final Tuple o) {
        final int tLen = this.valueArray.length;
        final Object[] oValues = o.valueArray;
        final int oLen = oValues.length;
        for (int i = 0; i < tLen && i < oLen; i++) {
            final Comparable tElement = (Comparable) this.valueArray[i];
            final Comparable oElement = (Comparable) oValues[i];
            final int comparison = tElement.compareTo(oElement);
            if (comparison != 0) {
                return comparison;
            }
        }
        return Integer.compare(tLen, oLen);
    }

    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.valueList == null) ? 0 : this.valueList.hashCode());
        return result;
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Tuple other = (Tuple) obj;
        return this.valueList.equals(other.valueList);
    }

    @Override
    public final String toString() {
        return this.valueList.toString();
    }
}
