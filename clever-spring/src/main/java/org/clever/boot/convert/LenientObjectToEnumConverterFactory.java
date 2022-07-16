package org.clever.boot.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.core.convert.converter.ConverterFactory;
import org.clever.util.Assert;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;

import java.util.*;

/**
 * 用于从类型转换为 {@link java.lang.Enum}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:57 <br/>
 */
abstract class LenientObjectToEnumConverterFactory<T> implements ConverterFactory<T, Enum<?>> {
    private static final Map<String, List<String>> ALIASES;

    static {
        MultiValueMap<String, String> aliases = new LinkedMultiValueMap<>();
        aliases.add("true", "on");
        aliases.add("false", "off");
        ALIASES = Collections.unmodifiableMap(aliases);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Enum<?>> Converter<T, E> getConverter(Class<E> targetType) {
        Class<?> enumType = targetType;
        while (enumType != null && !enumType.isEnum()) {
            enumType = enumType.getSuperclass();
        }
        assert targetType != null;
        Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
        return new LenientToEnumConverter<>((Class<E>) enumType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private class LenientToEnumConverter<E extends Enum> implements Converter<T, E> {
        private final Class<E> enumType;

        LenientToEnumConverter(Class<E> enumType) {
            this.enumType = enumType;
        }

        @Override
        public E convert(T source) {
            String value = source.toString().trim();
            if (value.isEmpty()) {
                return null;
            }
            try {
                return (E) Enum.valueOf(this.enumType, value);
            } catch (Exception ex) {
                return findEnum(value);
            }
        }

        private E findEnum(String value) {
            String name = getCanonicalName(value);
            List<String> aliases = ALIASES.getOrDefault(name, Collections.emptyList());
            for (E candidate : (Set<E>) EnumSet.allOf(this.enumType)) {
                String candidateName = getCanonicalName(candidate.name());
                if (name.equals(candidateName) || aliases.contains(candidateName)) {
                    return candidate;
                }
            }
            throw new IllegalArgumentException("No enum constant " + this.enumType.getCanonicalName() + "." + value);
        }

        private String getCanonicalName(String name) {
            StringBuilder canonicalName = new StringBuilder(name.length());
            name.chars()
                    .filter(Character::isLetterOrDigit)
                    .map(Character::toLowerCase)
                    .forEach((c) -> canonicalName.append((char) c));
            return canonicalName.toString();
        }
    }
}
