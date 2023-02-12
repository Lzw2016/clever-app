package org.clever.data.redis.connection;

import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

/**
 * 可能包含也可能不包含 Redis 密码的值对象。
 * <p>
 * 如果存在密码，{@code isPresent()} 将返回 {@code true}，{@code get()} 将返回值。
 * <p>
 * 密码存储为字符数组。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:26 <br/>
 */
public class RedisPassword {
    private static final RedisPassword NONE = new RedisPassword(new char[]{});

    private final char[] thePassword;

    private RedisPassword(char[] thePassword) {
        this.thePassword = thePassword;
    }

    /**
     * 从 {@link String} 创建一个 {@link RedisPassword}
     *
     * @param passwordAsString 密码为字符串
     * @return {@code passwordAsString} 的 {@link RedisPassword}
     */
    public static RedisPassword of(String passwordAsString) {
        return Optional.ofNullable(passwordAsString)
                .filter(StringUtils::hasText)
                .map(it -> new RedisPassword(it.toCharArray()))
                .orElseGet(RedisPassword::none);
    }

    /**
     * 从 {@code char} 数组创建 {@link RedisPassword}
     *
     * @param passwordAsChars 密码为字符数组
     * @return {@code passwordAsChars} 的 {@link RedisPassword}
     */
    public static RedisPassword of(char[] passwordAsChars) {
        return Optional.ofNullable(passwordAsChars)
                .filter(it -> !ObjectUtils.isEmpty(passwordAsChars))
                .map(it -> new RedisPassword(Arrays.copyOf(it, it.length)))
                .orElseGet(RedisPassword::none);
    }

    /**
     * Create an absent {@link RedisPassword}.
     *
     * @return the absent {@link RedisPassword}.
     */
    public static RedisPassword none() {
        return NONE;
    }

    /**
     * @return {@code true} 如果存在密码，否则 {@code false}
     */
    public boolean isPresent() {
        return !ObjectUtils.isEmpty(thePassword);
    }

    /**
     * 返回密码值（如果存在）。如果密码不存在，则抛出 {@link NoSuchElementException}
     *
     * @return 密码
     * @throws NoSuchElementException 如果没有密码
     */
    public char[] get() throws NoSuchElementException {
        if (isPresent()) {
            return Arrays.copyOf(thePassword, thePassword.length);
        }
        throw new NoSuchElementException("No password present.");
    }

    /**
     * 使用 {@link Function} 映射密码并返回包含映射值的 {@link Optional}。
     * <p>
     * 缺少密码返回 {@link Optional#empty()}
     *
     * @param mapper 不得为 {@literal null}
     * @return 映射的结果
     */
    public <R> Optional<R> map(Function<char[], R> mapper) {
        Assert.notNull(mapper, "Mapper function must not be null!");
        return toOptional().map(mapper);
    }

    /**
     * 采用包含密码值的 {@link Optional} 密码。
     * <p>
     * 缺少密码返回 {@link Optional#empty()}
     *
     * @return {@link Optional} 包含密码值
     */
    public Optional<char[]> toOptional() {
        if (isPresent()) {
            return Optional.of(get());
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", getClass().getSimpleName(), isPresent() ? "*****" : "<none>");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RedisPassword password = (RedisPassword) o;
        return ObjectUtils.nullSafeEquals(thePassword, password.thePassword);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(thePassword);
    }
}
