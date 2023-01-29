package org.clever.data.redis.connection;

import org.clever.data.redis.connection.BitFieldSubCommands.BitFieldSubCommand;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;

import java.util.*;

/**
 * 实际的 {@code BITFIELD} 命令表示包含几个要执行的 {@link BitFieldSubCommands.BitFieldSubCommand}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/24 20:12 <br/>
 */
public class BitFieldSubCommands implements Iterable<BitFieldSubCommand> {
    private final List<BitFieldSubCommand> subCommands;

    private BitFieldSubCommands(List<BitFieldSubCommand> subCommands) {
        this.subCommands = new ArrayList<>(subCommands);
    }

    private BitFieldSubCommands(List<BitFieldSubCommand> subCommands, BitFieldSubCommand subCommand) {
        this(subCommands);
        Assert.notNull(subCommand, "SubCommand must not be null!");
        this.subCommands.add(subCommand);
    }

    /**
     * 创建一个新的 {@link BitFieldSubCommands}
     */
    public static BitFieldSubCommands create() {
        return new BitFieldSubCommands(Collections.emptyList());
    }

    /**
     * 使用多个 BitFieldSubCommand 创建一个新的 {@link BitFieldSubCommands}
     */
    public static BitFieldSubCommands create(BitFieldSubCommand... subCommands) {
        Assert.notNull(subCommands, "Subcommands must not be null");
        return new BitFieldSubCommands(Arrays.asList(subCommands));
    }

    /**
     * 获取新的 {@link BitFieldGetBuilder} 用于创建和添加 {@link BitFieldGet} 子命令
     *
     * @param type 不能是 {@literal null}
     */
    public BitFieldGetBuilder get(BitFieldType type) {
        return new BitFieldGetBuilder(this).forType(type);
    }

    /**
     * 创建新的 {@link BitFieldSubCommands} 添加给定的 {@link BitFieldGet} 到子命令
     *
     * @param get 不能是 {@literal null}
     */
    protected BitFieldSubCommands get(BitFieldGet get) {
        return new BitFieldSubCommands(subCommands, get);
    }

    /**
     * 获取新的 {@link BitFieldSetBuilder} 用于创建和添加 {@link BitFieldSet} 子命令
     *
     * @param type 不能是 {@literal null}
     */
    public BitFieldSetBuilder set(BitFieldType type) {
        return new BitFieldSetBuilder(this).forType(type);
    }

    /**
     * 创建新的 {@link BitFieldSubCommands} 添加给定的 {@link BitFieldSet} 到子命令
     *
     * @param set 不能是 {@literal null}
     */
    protected BitFieldSubCommands set(BitFieldSet set) {
        return new BitFieldSubCommands(subCommands, set);
    }

    /**
     * 获取新的 {@link BitFieldIncrByBuilder} 用于创建和添加 {@link BitFieldIncrBy} 子命令
     *
     * @param type 不能是 {@literal null}
     */
    public BitFieldIncrByBuilder incr(BitFieldType type) {
        return new BitFieldIncrByBuilder(this).forType(type);
    }

    /**
     * 创建新的 {@link BitFieldSubCommands} 添加给定的 {@link BitFieldIncrBy} 到子命令
     *
     * @param incrBy 不能是 {@literal null}
     */
    protected BitFieldSubCommands incr(BitFieldIncrBy incrBy) {
        return new BitFieldSubCommands(subCommands, incrBy);
    }

    /**
     * 获取子命令的{@link List}
     *
     * @return 从不 {@literal null}
     */
    public List<BitFieldSubCommand> getSubCommands() {
        return subCommands;
    }

    @Override
    public Iterator<BitFieldSubCommand> iterator() {
        return subCommands.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BitFieldSubCommands)) {
            return false;
        }
        BitFieldSubCommands that = (BitFieldSubCommands) o;
        return ObjectUtils.nullSafeEquals(subCommands, that.subCommands);
    }

    @Override
    public int hashCode() {
        return ObjectUtils.nullSafeHashCode(subCommands);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [subCommands=" + subCommands + ']';
    }

    public static class BitFieldSetBuilder {
        private final BitFieldSubCommands ref;
        BitFieldSet set = new BitFieldSet();

        private BitFieldSetBuilder(BitFieldSubCommands ref) {
            this.ref = ref;
        }

        public BitFieldSetBuilder forType(BitFieldType type) {
            this.set.type = type;
            return this;
        }

        /**
         * 设置从零开始的位 {@literal offset}
         *
         * @param offset 不能是 {@literal null}
         */
        public BitFieldSetBuilder valueAt(long offset) {
            return valueAt(Offset.offset(offset));
        }

        /**
         * 设置位偏移量
         *
         * @param offset 不能是 {@literal null}
         */
        public BitFieldSetBuilder valueAt(Offset offset) {
            Assert.notNull(offset, "Offset must not be null!");
            this.set.offset = offset;
            return this;
        }

        /**
         * 设置值
         *
         * @param value 不能是 {@literal null}
         */
        public BitFieldSubCommands to(long value) {
            this.set.value = value;
            return ref.set(this.set);
        }
    }

    public static class BitFieldGetBuilder {
        private final BitFieldSubCommands ref;
        BitFieldGet get = new BitFieldGet();

        private BitFieldGetBuilder(BitFieldSubCommands ref) {
            this.ref = ref;
        }

        public BitFieldGetBuilder forType(BitFieldType type) {
            this.get.type = type;
            return this;
        }

        /**
         * 设置从零开始的位 {@literal offset}
         *
         * @param offset 不能是 {@literal null}
         */
        public BitFieldSubCommands valueAt(long offset) {
            return valueAt(Offset.offset(offset));
        }

        /**
         * 设置位偏移
         *
         * @param offset 不能是 {@literal null}
         */
        public BitFieldSubCommands valueAt(Offset offset) {
            Assert.notNull(offset, "Offset must not be null!");
            this.get.offset = offset;
            return ref.get(this.get);
        }
    }

    public static class BitFieldIncrByBuilder {
        private final BitFieldSubCommands ref;
        BitFieldIncrBy incrBy = new BitFieldIncrBy();

        private BitFieldIncrByBuilder(BitFieldSubCommands ref) {
            this.ref = ref;
        }

        public BitFieldIncrByBuilder forType(BitFieldType type) {
            this.incrBy.type = type;
            return this;
        }

        /**
         * 设置从零开始的位 {@literal offset}
         *
         * @param offset 不能是 {@literal null}
         */
        public BitFieldIncrByBuilder valueAt(long offset) {
            return valueAt(Offset.offset(offset));
        }

        /**
         * 设置位偏移
         *
         * @param offset 不能是 {@literal null}
         */
        public BitFieldIncrByBuilder valueAt(Offset offset) {
            Assert.notNull(offset, "Offset must not be null!");
            this.incrBy.offset = offset;
            return this;
        }

        /**
         * 设置 {@link BitFieldIncrBy.Overflow} 用于此命令和任何后续的 {@link BitFieldIncrBy} 命令
         */
        public BitFieldIncrByBuilder overflow(BitFieldIncrBy.Overflow overflow) {
            this.incrBy.overflow = overflow;
            return this;
        }

        /**
         * 设置用于增加的值
         */
        public BitFieldSubCommands by(long value) {
            this.incrBy.value = value;
            return ref.incr(this.incrBy);
        }
    }

    /**
     * 用作 {@link BitFieldSubCommands} 一部分的子命令
     */
    public interface BitFieldSubCommand {
        /**
         * 实际的子命令
         *
         * @return 从不{@literal null}
         */
        String getCommand();

        /**
         * {@link BitFieldType} 申请命令
         *
         * @return 从不{@literal null}
         */
        BitFieldType getType();

        /**
         * 申请命令的位偏移量
         *
         * @return 从不{@literal null}
         */
        Offset getOffset();
    }

    /**
     * {@link BitFieldSubCommand} 中使用的偏移量。可以是零或基于类型。<br/>
     * 请参阅 Redis 参考中的位和<a href="https://redis.io/commands/bitfield#bits-and-positional-offsets">位和位置偏移</a>。
     */
    public static class Offset {
        private final long offset;
        private final boolean zeroBased;

        private Offset(long offset, boolean zeroBased) {
            this.offset = offset;
            this.zeroBased = zeroBased;
        }

        /**
         * 创建新的基于零的偏移量<br />
         * <b>注意:</b> 通过调用 {@link #multipliedByTypeLength()} 更改为基于类型的偏移量.
         *
         * @param offset 不能是 {@literal null}
         */
        public static Offset offset(long offset) {
            return new Offset(offset, true);
        }

        /**
         * 创建新的基于类型的偏移量
         */
        public Offset multipliedByTypeLength() {
            return new Offset(offset, false);
        }

        /**
         * @return 如果偏移量从 0 开始并且不乘以类型长度，则为真
         */
        public boolean isZeroBased() {
            return zeroBased;
        }

        /**
         * @return 实际偏移值
         */
        public long getValue() {
            return offset;
        }

        /**
         * @return Redis 命令表示
         */
        public String asString() {
            return (isZeroBased() ? "" : "#") + getValue();
        }

        @Override
        public String toString() {
            return asString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Offset)) {
                return false;
            }
            Offset that = (Offset) o;
            if (offset != that.offset) {
                return false;
            }
            return zeroBased == that.zeroBased;
        }

        @Override
        public int hashCode() {
            int result = (int) (offset ^ (offset >>> 32));
            result = 31 * result + (zeroBased ? 1 : 0);
            return result;
        }
    }

    /**
     * 与 {@link BitFieldSubCommand} 一起使用的有符号和无符号整数的实际 Redis 位域类型表示
     */
    public static class BitFieldType {
        /**
         * 8 位有符号整数
         */
        public static final BitFieldType INT_8 = new BitFieldType(true, 8);
        /**
         * 16 位有符号整数
         */
        public static final BitFieldType INT_16 = new BitFieldType(true, 16);
        /**
         * 32 位有符号整数
         */
        public static final BitFieldType INT_32 = new BitFieldType(true, 32);
        /**
         * 64 位有符号整数
         */
        public static final BitFieldType INT_64 = new BitFieldType(true, 64);
        /**
         * 8 位无符号整数
         */
        public static final BitFieldType UINT_8 = new BitFieldType(false, 8);
        /**
         * 16 位无符号整数
         */
        public static final BitFieldType UINT_16 = new BitFieldType(false, 16);
        /**
         * 32 位无符号整数
         */
        public static final BitFieldType UINT_32 = new BitFieldType(false, 32);
        /**
         * 64 位无符号整数
         */
        public static final BitFieldType UINT_64 = new BitFieldType(false, 64);

        private final boolean signed;
        private final int bits;

        private BitFieldType(boolean signed, Integer bits) {
            this.signed = signed;
            this.bits = bits;
        }

        /**
         * 创建新的签名 {@link BitFieldType}
         *
         * @param bits 不能是 {@literal null}
         */
        public static BitFieldType signed(int bits) {
            return new BitFieldType(true, bits);
        }

        /**
         * 创建新的无符号 {@link BitFieldType}
         *
         * @param bits 不能是 {@literal null}
         */
        public static BitFieldType unsigned(int bits) {
            return new BitFieldType(false, bits);
        }

        /**
         * @return 如果 {@link BitFieldType} 已签名，则为真
         */
        public boolean isSigned() {
            return signed;
        }

        /**
         * 获取类型的实际位
         *
         * @return 从不 {@literal null}
         */
        public int getBits() {
            return bits;
        }

        /**
         * 获取 Redis 命令表示
         */
        public String asString() {
            return (isSigned() ? "i" : "u") + getBits();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BitFieldType)) {
                return false;
            }
            BitFieldType that = (BitFieldType) o;
            if (signed != that.signed) {
                return false;
            }
            return bits == that.bits;
        }

        @Override
        public int hashCode() {
            int result = (signed ? 1 : 0);
            result = 31 * result + bits;
            return result;
        }

        @Override
        public String toString() {
            return asString();
        }
    }

    public static abstract class AbstractBitFieldSubCommand implements BitFieldSubCommand {
        BitFieldType type;
        Offset offset;

        @Override
        public BitFieldType getType() {
            return type;
        }

        @Override
        public Offset getOffset() {
            return offset;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AbstractBitFieldSubCommand)) {
                return false;
            }
            AbstractBitFieldSubCommand that = (AbstractBitFieldSubCommand) o;
            if (!ObjectUtils.nullSafeEquals(getClass(), that.getClass())) {
                return false;
            }
            if (!ObjectUtils.nullSafeEquals(type, that.type)) {
                return false;
            }
            return ObjectUtils.nullSafeEquals(offset, that.offset);
        }

        @Override
        public int hashCode() {
            int result = ObjectUtils.nullSafeHashCode(type);
            result = 31 * result + ObjectUtils.nullSafeHashCode(offset);
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [type=" + type + ", offset=" + offset + ']';
        }
    }

    /**
     * {@code SET} 子命令与 {@link BitFieldSubCommands} 一起使用
     */
    public static class BitFieldSet extends AbstractBitFieldSubCommand {
        private long value;

        /**
         * 创建一个新的 {@link BitFieldSet}
         *
         * @param type   不能是 {@literal null}
         * @param offset 不能是 {@literal null}
         * @param value  不能是 {@literal null}
         */
        public static BitFieldSet create(BitFieldType type, Offset offset, long value) {
            Assert.notNull(type, "BitFieldType must not be null");
            Assert.notNull(offset, "Offset must not be null");
            BitFieldSet instance = new BitFieldSet();
            instance.type = type;
            instance.offset = offset;
            instance.value = value;
            return instance;
        }

        @Override
        public String getCommand() {
            return "SET";
        }

        /**
         * 获取要设置的值
         *
         * @return 从不{@literal null}
         */
        public long getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BitFieldSet)) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            BitFieldSet that = (BitFieldSet) o;
            // noinspection RedundantIfStatement
            if (value != that.value) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + (int) (value ^ (value >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [type=" + type + ", offset=" + offset + ", value=" + value + ']';
        }
    }

    /**
     * {@code GET} 子命令与 {@link BitFieldSubCommands} 一起使用
     */
    public static class BitFieldGet extends AbstractBitFieldSubCommand {
        /**
         * 创建一个新的 {@link BitFieldGet}
         *
         * @param type   不能是 {@literal null}
         * @param offset 不能是 {@literal null}
         */
        public static BitFieldGet create(BitFieldType type, Offset offset) {
            Assert.notNull(type, "BitFieldType must not be null");
            Assert.notNull(offset, "Offset must not be null");
            BitFieldGet instance = new BitFieldGet();
            instance.type = type;
            instance.offset = offset;
            return instance;
        }

        @Override
        public String getCommand() {
            return "GET";
        }
    }

    /**
     * 与 {@link BitFieldSubCommands} 一起使用的 {@code INCRBY} 子命令
     */
    public static class BitFieldIncrBy extends AbstractBitFieldSubCommand {
        private long value;
        private Overflow overflow;

        /**
         * 创建一个新的 {@link BitFieldIncrBy}
         *
         * @param type   不能是 {@literal null}
         * @param offset 不能是 {@literal null}
         * @param value  不能是 {@literal null}
         */
        public static BitFieldIncrBy create(BitFieldType type, Offset offset, long value) {
            return create(type, offset, value, null);
        }

        /**
         * 创建一个新的 {@link BitFieldIncrBy}
         *
         * @param type     不能是 {@literal null}.
         * @param offset   不能是 {@literal null}.
         * @param value    不能是 {@literal null}.
         * @param overflow 可以是 {@literal null} 以使用 redis 默认值
         */
        public static BitFieldIncrBy create(BitFieldType type, Offset offset, long value, Overflow overflow) {
            Assert.notNull(type, "BitFieldType must not be null");
            Assert.notNull(offset, "Offset must not be null");
            BitFieldIncrBy instance = new BitFieldIncrBy();
            instance.type = type;
            instance.offset = offset;
            instance.value = value;
            instance.overflow = overflow;
            return instance;
        }

        @Override
        public String getCommand() {
            return "INCRBY";
        }

        /**
         * 获取增量值
         *
         * @return 从不 {@literal null}
         */
        public long getValue() {
            return value;
        }

        /**
         * 获取溢出以应用。可以是 {@literal null} 以使用 redis 默认值
         *
         * @return 可以是 {@literal null}
         */
        public Overflow getOverflow() {
            return overflow;
        }

        public enum Overflow {
            SAT, FAIL, WRAP
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof BitFieldIncrBy)) {
                return false;
            }
            BitFieldIncrBy that = (BitFieldIncrBy) o;
            if (value != that.value) {
                return false;
            }
            return overflow == that.overflow;
        }

        @Override
        public int hashCode() {
            int result = (int) (value ^ (value >>> 32));
            result = 31 * result + ObjectUtils.nullSafeHashCode(overflow);
            return result;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [type=" + type + ", offset=" + offset + ", value=" + value + ", overflow=" + overflow + ']';
        }
    }
}
