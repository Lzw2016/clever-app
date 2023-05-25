package org.clever.core.tuples;

/**
 * 三元元组
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 12:01 <br/>
 */
public final class TupleThree<A, B, C> extends Tuple implements Tuple.One<A>, Tuple.Two<B>, Tuple.Three<C> {
    private static final int SIZE = 3;

    private A value1;
    private B value2;
    private C value3;

    public static <A, B, C> TupleThree<A, B, C> creat(final A value1, final B value2, final C value3) {
        return new TupleThree<>(value1, value2, value3);
    }

    public TupleThree(final A value1, final B value2, final C value3) {
        super(value1, value2, value3);
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public C getValue3() {
        return value3;
    }

    @Override
    public void setValue3(C val) {
        value3 = val;
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
