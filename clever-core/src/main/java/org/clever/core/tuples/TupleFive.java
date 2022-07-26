package org.clever.core.tuples;

import org.clever.core.tuples.value.*;

/**
 * 五元元组
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 12:09 <br/>
 */
public final class TupleFive<A, B, C, D, E> extends Tuple implements One<A>, Two<B>, Three<C>, Four<D>, Five<E> {
    private static final int SIZE = 5;

    private A value1;
    private B value2;
    private C value3;
    private D value4;
    private E value5;

    public static <A, B, C, D, E> TupleFive<A, B, C, D, E> creat(final A value1, final B value2, final C value3, final D value4, final E value5) {
        return new TupleFive<>(value1, value2, value3, value4, value5);
    }

    public TupleFive(final A value1, final B value2, final C value3, final D value4, final E value5) {
        super(value1, value2, value3, value4, value5);
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
        this.value5 = value5;
    }

    @Override
    public int getSize() {
        return SIZE;
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
