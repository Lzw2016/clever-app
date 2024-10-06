package org.clever.core.tuples;

/**
 * 十元元组
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 12:15 <br/>
 */
public final class TupleTen<A, B, C, D, E, F, G, H, I, J> extends Tuple implements Tuple.One<A>, Tuple.Two<B>, Tuple.Three<C>, Tuple.Four<D>, Tuple.Five<E>, Tuple.Six<F>, Tuple.Seven<G>, Tuple.Eight<H>, Tuple.Nine<I>, Tuple.Ten<J> {
    private static final int SIZE = 10;

    private volatile A value1;
    private volatile B value2;
    private volatile C value3;
    private volatile D value4;
    private volatile E value5;
    private volatile F value6;
    private volatile G value7;
    private volatile H value8;
    private volatile I value9;
    private volatile J value10;

    public static <A, B, C, D, E, F, G, H, I, J> TupleTen<A, B, C, D, E, F, G, H, I, J> creat(final A value1, final B value2, final C value3, final D value4, final E value5, final F value6, final G value7, final H value8, final I value9, final J value10) {
        return new TupleTen<>(value1, value2, value3, value4, value5, value6, value7, value8, value9, value10);
    }

    public TupleTen(final A value1, final B value2, final C value3, final D value4, final E value5, final F value6, final G value7, final H value8, final I value9, final J value10) {
        super(value1, value2, value3, value4, value5, value6, value7, value8, value9, value10);
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
        this.value5 = value5;
        this.value6 = value6;
        this.value7 = value7;
        this.value8 = value8;
        this.value9 = value9;
        this.value10 = value10;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public J getValue10() {
        return value10;
    }

    @Override
    public void setValue10(J val) {
        value10 = val;
    }

    @Override
    public I getValue9() {
        return value9;
    }

    @Override
    public void setValue9(I val) {
        value9 = val;
    }

    @Override
    public H getValue8() {
        return value8;
    }

    @Override
    public void setValue8(H val) {
        value8 = val;
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
