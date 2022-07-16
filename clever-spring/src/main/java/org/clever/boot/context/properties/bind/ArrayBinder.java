package org.clever.boot.context.properties.bind;

import org.clever.boot.context.properties.bind.Binder.Context;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.core.ResolvableType;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * arrays 的 {@link AggregateBinder}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:21 <br/>
 */
class ArrayBinder extends IndexedElementsBinder<Object> {
    ArrayBinder(Context context) {
        super(context);
    }

    @Override
    protected Object bindAggregate(ConfigurationPropertyName name, Bindable<?> target, AggregateElementBinder elementBinder) {
        IndexedCollectionSupplier result = new IndexedCollectionSupplier(ArrayList::new);
        ResolvableType aggregateType = target.getType();
        ResolvableType elementType = target.getType().getComponentType();
        bindIndexed(name, target, elementBinder, aggregateType, elementType, result);
        if (result.wasSupplied()) {
            List<Object> list = (List<Object>) result.get();
            Object array = Array.newInstance(elementType.resolve(), list.size());
            for (int i = 0; i < list.size(); i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        }
        return null;
    }

    @Override
    protected Object merge(Supplier<Object> existing, Object additional) {
        return additional;
    }
}
