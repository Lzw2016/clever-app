package org.clever.core.tuples;

/**
 * 七元元组
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 12:14 <br/>
 */
public final class TupleSeven<A, B, C, D, E, F, G> extends Tuple implements Tuple.One<A>, Tuple.Two<B>, Tuple.Three<C>, Tuple.Four<D>, Tuple.Five<E>, Tuple.Six<F>, Tuple.Seven<G> {
    private static final int SIZE = 7;

    private volatile A value1;
    private volatile B value2;
    private volatile C value3;
    private volatile D value4;
    private volatile E value5;
    private volatile F value6;
    private volatile G value7;

    public static <A, B, C, D, E, F, G> TupleSeven<A, B, C, D, E, F, G> creat(final A value1, final B value2, final C value3, final D value4, final E value5, final F value6, final G value7) {
        return new TupleSeven<>(value1, value2, value3, value4, value5, value6, value7);
    }

    public TupleSeven(final A value1, final B value2, final C value3, final D value4, final E value5, final F value6, final G value7) {
        super(value1, value2, value3, value4, value5, value6, value7);
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
        this.value5 = value5;
        this.value6 = value6;
        this.value7 = value7;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public G getValue7() {
        return value7;
    }

    @Override
    public void setValue7(G val) {
        value7 = val;
    }

    @Override
    public F getValue6() {
        return value6;
    }

    @Override
    public void setValue6(F val) {
        value6 = val;
    }

    @Override
    public E getValue5() {
        return value5;
    }

    @Override
    public void setValue5(E val) {
        value5 = val;
    }

    @Override
    public D getValue4() {
        return value4;
    }

    @Override
    public void setValue4(D val) {
        value4 = val;
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
