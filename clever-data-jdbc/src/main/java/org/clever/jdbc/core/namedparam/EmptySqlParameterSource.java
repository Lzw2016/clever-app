package org.clever.jdbc.core.namedparam;

/**
 * {@link SqlParameterSource}接口的简单空实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 00:00 <br/>
 */
public class EmptySqlParameterSource implements SqlParameterSource {
    /**
     * 的共享实例 {@link EmptySqlParameterSource}.
     */
    public static final EmptySqlParameterSource INSTANCE = new EmptySqlParameterSource();

    @Override
    public boolean hasValue(String paramName) {
        return false;
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        throw new IllegalArgumentException("This SqlParameterSource is empty");
    }

    @Override
    public int getSqlType(String paramName) {
        return TYPE_UNKNOWN;
    }

    @Override
    public String getTypeName(String paramName) {
        return null;
    }

    @Override
    public String[] getParameterNames() {
        return null;
    }
}
