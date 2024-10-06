package org.clever.core.tuples;

/**
 * 二元元组
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 11:44 <br/>
 */
public final class TupleTwo<A, B> extends Tuple implements Tuple.One<A>, Tuple.Two<B> {
    private static final int SIZE = 2;

    private volatile A value1;
    private volatile B value2;

    public static <A, B> TupleTwo<A, B> creat(final A value1, final B value2) {
        return new TupleTwo<>(value1, value2);
    }

    public TupleTwo(final A value1, final B value2) {
        super(value1, value2);
        this.value1 = value1;
        this.value2 = value2;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public B getValue2() {
        return value2;
    }

    @Override
    public void setValue2(B val) {
        value2 = val;
    }

    @Override
    public A getValue1() {
        return value1;
    }

    @Override
    public void setValue1(A val) {
        value1 = val;
    }
}
