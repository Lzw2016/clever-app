package org.clever.data.redis.connection.convert;

import org.clever.core.convert.converter.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * 将一种类型的值列表转换为另一种类型的值列表
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/29 22:38 <br/>
 *
 * @param <S> 列表中要转换的元素的类型
 * @param <T> 转换后的列表中的元素类型
 */
public class ListConverter<S, T> implements Converter<List<S>, List<T>> {
    private final Converter<S, T> itemConverter;

    /**
     * @param itemConverter 用于转换单个列表项的 {@link Converter}
     */
    public ListConverter(Converter<S, T> itemConverter) {
        this.itemConverter = itemConverter;
    }

    @Override
    public List<T> convert(List<S> source) {
        List<T> results = new ArrayList<>(source.size());
        for (S result : source) {
            results.add(itemConverter.convert(result));
        }
        return results;
    }
}
