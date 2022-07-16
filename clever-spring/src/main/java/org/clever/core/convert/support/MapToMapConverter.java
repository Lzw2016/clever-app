package org.clever.core.convert.support;

import org.clever.core.CollectionFactory;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.core.convert.converter.ConditionalGenericConverter;

import java.util.*;

/**
 * 将Map转换成Map<br/>
 * 首先，创建请求的targetType的新Map，其大小等于源Map的大小。
 * 然后将源Map中的每个元素复制到目标Map。
 * 如有必要，将执行从源Map的参数化K、V类型到目标Map的参数化K、V类型的转换
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/30 15:56 <br/>
 */
final class MapToMapConverter implements ConditionalGenericConverter {
    private final ConversionService conversionService;

    public MapToMapConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(Map.class, Map.class));
    }

    @Override
    public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return canConvertKey(sourceType, targetType) && canConvertValue(sourceType, targetType);
    }

    @Override
    public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<Object, Object> sourceMap = (Map<Object, Object>) source;

        // Shortcut if possible...
        boolean copyRequired = !targetType.getType().isInstance(source);
        if (!copyRequired && sourceMap.isEmpty()) {
            return sourceMap;
        }
        TypeDescriptor keyDesc = targetType.getMapKeyTypeDescriptor();
        TypeDescriptor valueDesc = targetType.getMapValueTypeDescriptor();

        List<MapEntry> targetEntries = new ArrayList<>(sourceMap.size());
        for (Map.Entry<Object, Object> entry : sourceMap.entrySet()) {
            Object sourceKey = entry.getKey();
            Object sourceValue = entry.getValue();
            Object targetKey = convertKey(sourceKey, sourceType, keyDesc);
            Object targetValue = convertValue(sourceValue, sourceType, valueDesc);
            targetEntries.add(new MapEntry(targetKey, targetValue));
            if (sourceKey != targetKey || sourceValue != targetValue) {
                copyRequired = true;
            }
        }
        if (!copyRequired) {
            return sourceMap;
        }

        Map<Object, Object> targetMap = CollectionFactory.createMap(targetType.getType(), (keyDesc != null ? keyDesc.getType() : null), sourceMap.size());

        for (MapEntry entry : targetEntries) {
            entry.addToMap(targetMap);
        }
        return targetMap;
    }

    // internal helpers
    private boolean canConvertKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.canConvertElements(sourceType.getMapKeyTypeDescriptor(), targetType.getMapKeyTypeDescriptor(), this.conversionService);
    }

    private boolean canConvertValue(TypeDescriptor sourceType, TypeDescriptor targetType) {
        return ConversionUtils.canConvertElements(sourceType.getMapValueTypeDescriptor(), targetType.getMapValueTypeDescriptor(), this.conversionService);
    }

    private Object convertKey(Object sourceKey, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType == null) {
            return sourceKey;
        }
        return this.conversionService.convert(sourceKey, sourceType.getMapKeyTypeDescriptor(sourceKey), targetType);
    }

    private Object convertValue(Object sourceValue, TypeDescriptor sourceType, TypeDescriptor targetType) {
        if (targetType == null) {
            return sourceValue;
        }
        return this.conversionService.convert(sourceValue, sourceType.getMapValueTypeDescriptor(sourceValue), targetType);
    }

    private static class MapEntry {
        private final Object key;
        private final Object value;

        public MapEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        public void addToMap(Map<Object, Object> map) {
            map.put(this.key, this.value);
        }
    }
}
