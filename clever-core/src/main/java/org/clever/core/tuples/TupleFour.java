package org.clever.core.tuples;

import org.clever.core.tuples.value.Four;
import org.clever.core.tuples.value.One;
import org.clever.core.tuples.value.Three;
import org.clever.core.tuples.value.Two;

/**
 * 四元元组
 * 作者：lizw <br/>
 * 创建时间：2019/08/16 12:05 <br/>
 */
public final class TupleFour<A, B, C, D> extends Tuple implements One<A>, Two<B>, Three<C>, Four<D> {
    private static final int SIZE = 4;

    private A value1;
    private B value2;
    private C value3;
    private D value4;

    public static <A, B, C, D> TupleFour<A, B, C, D> creat(final A value1, final B value2, final C value3, final D value4) {
        return new TupleFour<>(value1, value2, value3, value4);
    }

    public TupleFour(final A value1, final B value2, final C value3, final D value4) {
        super(value1, value2, value3, value4);
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    @Override
    public int getSize() {
        return SIZE;
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
