package org.clever.core.tuples;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/02/20 09:20 <br/>
 */
public class TupleOne<A> extends Tuple implements Tuple.One<A> {
    private static final int SIZE = 1;

    private volatile A value1;

    public static <A> TupleOne<A> creat(final A value1) {
        return new TupleOne<>(value1);
    }

    public TupleOne(final A value1) {
        super(value1);
        this.value1 = value1;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public A getValue1() {
        return value1;
    }

    @Override
    public void setValue1(A val) {
        this.value1 = val;
    }
}
