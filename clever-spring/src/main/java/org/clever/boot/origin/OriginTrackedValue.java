package org.clever.boot.origin;

import org.clever.util.ObjectUtils;

/**
 * {@link Object}值和{@link Origin}的包装。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:58 <br/>
 *
 * @see #of(Object)
 * @see #of(Object, Origin)
 */
public class OriginTrackedValue implements OriginProvider {
    private final Object value;
    private final Origin origin;

    private OriginTrackedValue(Object value, Origin origin) {
        this.value = value;
        this.origin = origin;
    }

    /**
     * 返回跟踪的值。
     *
     * @return 跟踪值
     */
    public Object getValue() {
        return this.value;
    }

    @Override
    public Origin getOrigin() {
        return this.origin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return ObjectUtils.nullSafeEquals(this.value, ((OriginTrackedValue) obj).value);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(this.value);
    }

    @Override
    public String toString() {
        return (this.value != null) ? this.value.toString() : null;
    }

    public static OriginTrackedValue of(Object value) {
        return of(value, null);
    }

    /**
     * 创建包含指定值和原点的{@link OriginTrackedValue}。
     * 如果源值实现{@link CharSequence}，那么生成的{@link OriginTrackedValue}也将实现{@link CharSequence}。
     *
     * @param value  源值
     * @param origin 起源
     * @return 如果源值为null，则为{@link OriginTrackedValue}。
     */
    public static OriginTrackedValue of(Object value, Origin origin) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence) {
            return new OriginTrackedCharSequence((CharSequence) value, origin);
        }
        return new OriginTrackedValue(value, origin);
    }

    /**
     * {@link CharSequence}的{@link OriginTrackedValue}。
     */
    private static class OriginTrackedCharSequence extends OriginTrackedValue implements CharSequence {
        OriginTrackedCharSequence(CharSequence value, Origin origin) {
            super(value, origin);
        }

        @Override
        public int length() {
            return getValue().length();
        }

        @Override
        public char charAt(int index) {
            return getValue().charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return getValue().subSequence(start, end);
        }

        @Override
        public CharSequence getValue() {
            return (CharSequence) super.getValue();
        }
    }
}
