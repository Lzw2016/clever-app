package org.clever.beans;

import org.clever.core.CollectionFactory;
import org.clever.core.convert.ConversionFailedException;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.util.ClassUtils;
import org.clever.util.NumberUtils;
import org.clever.util.ReflectionUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyEditor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * 用于将特性值转换为目标类型的内部帮助器类。
 * 在给定的PropertyEditorRegistrySupport实例上工作。
 * 由BeanWrapperImpl和SimpleTypeConverter用作委托
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:12 <br/>
 *
 * @see BeanWrapperImpl
 */
class TypeConverterDelegate {
    private static final Logger logger = LoggerFactory.getLogger(TypeConverterDelegate.class);
    private final PropertyEditorRegistrySupport propertyEditorRegistry;
    private final Object targetObject;

    /**
     * 为给定的编辑器注册表创建一个新的TypeConverterDeleteGate
     *
     * @param propertyEditorRegistry 要使用的编辑器注册表
     */
    public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry) {
        this(propertyEditorRegistry, null);
    }

    /**
     * 为给定的编辑器注册表和bean实例创建一个新的TypeConverterDelegate
     *
     * @param propertyEditorRegistry 要使用的编辑器注册表
     * @param targetObject           要处理的目标对象(作为可传递给编辑器的上下文)
     */
    public TypeConverterDelegate(PropertyEditorRegistrySupport propertyEditorRegistry, Object targetObject) {
        this.propertyEditorRegistry = propertyEditorRegistry;
        this.targetObject = targetObject;
    }

    /**
     * 将值转换为指定属性所需的类型
     *
     * @param propertyName 属性名
     * @param oldValue     以前的值，如果可用（可能为null）
     * @param newValue     建议的新值
     * @param requiredType 我们必须转换为的类型（如果未知，例如在集合元素的情况下，则为null）
     * @return 新值，可能是类型转换的结果
     * @throws IllegalArgumentException 如果类型转换失败
     */
    public <T> T convertIfNecessary(String propertyName, Object oldValue, Object newValue, Class<T> requiredType) throws IllegalArgumentException {
        return convertIfNecessary(propertyName, oldValue, newValue, requiredType, TypeDescriptor.valueOf(requiredType));
    }

    /**
     * 将指定属性的值转换为所需的类型（如果需要，从字符串转换）
     *
     * @param propertyName   属性名
     * @param oldValue       以前的值，如果可用（可能为null）
     * @param newValue       建议的新值
     * @param requiredType   我们必须转换为的类型（如果未知，例如在集合元素的情况下，则为null）
     * @param typeDescriptor 目标属性或字段的描述符
     * @return 新值，可能是类型转换的结果
     * @throws IllegalArgumentException 如果类型转换失败
     */
    @SuppressWarnings("unchecked")
    public <T> T convertIfNecessary(String propertyName,
                                    Object oldValue,
                                    Object newValue,
                                    Class<T> requiredType,
                                    TypeDescriptor typeDescriptor) throws IllegalArgumentException {
        // Custom editor for this type?
        PropertyEditor editor = this.propertyEditorRegistry.findCustomEditor(requiredType, propertyName);
        ConversionFailedException conversionAttemptEx = null;
        // No custom editor but custom ConversionService specified?
        ConversionService conversionService = this.propertyEditorRegistry.getConversionService();
        if (editor == null && conversionService != null && newValue != null && typeDescriptor != null) {
            TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
            if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
                try {
                    return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
                } catch (ConversionFailedException ex) {
                    // fallback to default conversion logic below
                    conversionAttemptEx = ex;
                }
            }
        }
        Object convertedValue = newValue;
        // Value not of required type?
        if (editor != null || (requiredType != null && !ClassUtils.isAssignableValue(requiredType, convertedValue))) {
            if (typeDescriptor != null && requiredType != null && Collection.class.isAssignableFrom(requiredType) && convertedValue instanceof String) {
                TypeDescriptor elementTypeDesc = typeDescriptor.getElementTypeDescriptor();
                if (elementTypeDesc != null) {
                    Class<?> elementType = elementTypeDesc.getType();
                    if (Class.class == elementType || Enum.class.isAssignableFrom(elementType)) {
                        convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
                    }
                }
            }
            if (editor == null) {
                editor = findDefaultEditor(requiredType);
            }
            convertedValue = doConvertValue(oldValue, convertedValue, requiredType, editor);
        }
        boolean standardConversion = false;
        if (requiredType != null) {
            // Try to apply some standard type conversion rules if appropriate.
            if (convertedValue != null) {
                if (Object.class == requiredType) {
                    return (T) convertedValue;
                } else if (requiredType.isArray()) {
                    // Array required -> apply appropriate conversion of elements.
                    if (convertedValue instanceof String && Enum.class.isAssignableFrom(requiredType.getComponentType())) {
                        convertedValue = StringUtils.commaDelimitedListToStringArray((String) convertedValue);
                    }
                    return (T) convertToTypedArray(convertedValue, propertyName, requiredType.getComponentType());
                } else if (convertedValue instanceof Collection) {
                    // Convert elements to target type, if determined.
                    convertedValue = convertToTypedCollection((Collection<?>) convertedValue, propertyName, requiredType, typeDescriptor);
                    standardConversion = true;
                } else if (convertedValue instanceof Map) {
                    // Convert keys and values to respective target type, if determined.
                    convertedValue = convertToTypedMap((Map<?, ?>) convertedValue, propertyName, requiredType, typeDescriptor);
                    standardConversion = true;
                }
                if (convertedValue.getClass().isArray() && Array.getLength(convertedValue) == 1) {
                    convertedValue = Array.get(convertedValue, 0);
                    standardConversion = true;
                }
                if (String.class == requiredType && ClassUtils.isPrimitiveOrWrapper(convertedValue.getClass())) {
                    // We can stringify any primitive value...
                    return (T) convertedValue.toString();
                } else if (convertedValue instanceof String && !requiredType.isInstance(convertedValue)) {
                    if (conversionAttemptEx == null && !requiredType.isInterface() && !requiredType.isEnum()) {
                        try {
                            Constructor<T> strCtor = requiredType.getConstructor(String.class);
                            return BeanUtils.instantiateClass(strCtor, convertedValue);
                        } catch (NoSuchMethodException ex) {
                            // proceed with field lookup
                            if (logger.isTraceEnabled()) {
                                logger.trace("No String constructor found on type [" + requiredType.getName() + "]", ex);
                            }
                        } catch (Exception ex) {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Construction via String failed for type [" + requiredType.getName() + "]", ex);
                            }
                        }
                    }
                    String trimmedValue = ((String) convertedValue).trim();
                    if (requiredType.isEnum() && trimmedValue.isEmpty()) {
                        // It's an empty enum identifier: reset the enum value to null.
                        return null;
                    }
                    convertedValue = attemptToConvertStringToEnum(requiredType, trimmedValue, convertedValue);
                    standardConversion = true;
                } else if (convertedValue instanceof Number && Number.class.isAssignableFrom(requiredType)) {
                    convertedValue = NumberUtils.convertNumberToTargetClass((Number) convertedValue, (Class<Number>) requiredType);
                    standardConversion = true;
                }
            } else {
                // convertedValue == null
                if (requiredType == Optional.class) {
                    convertedValue = Optional.empty();
                }
            }
            if (!ClassUtils.isAssignableValue(requiredType, convertedValue)) {
                if (conversionAttemptEx != null) {
                    // Original exception from former ConversionService call above...
                    throw conversionAttemptEx;
                } else if (conversionService != null && typeDescriptor != null) {
                    // ConversionService not tried before, probably custom editor found
                    // but editor couldn't produce the required type...
                    TypeDescriptor sourceTypeDesc = TypeDescriptor.forObject(newValue);
                    if (conversionService.canConvert(sourceTypeDesc, typeDescriptor)) {
                        return (T) conversionService.convert(newValue, sourceTypeDesc, typeDescriptor);
                    }
                }
                // Definitely doesn't match: throw IllegalArgumentException/IllegalStateException
                StringBuilder msg = new StringBuilder();
                msg.append("Cannot convert value of type '").append(ClassUtils.getDescriptiveType(newValue));
                msg.append("' to required type '").append(ClassUtils.getQualifiedName(requiredType)).append('\'');
                if (propertyName != null) {
                    msg.append(" for property '").append(propertyName).append('\'');
                }
                if (editor != null) {
                    msg.append(": PropertyEditor [")
                            .append(editor.getClass().getName())
                            .append("] returned inappropriate value of type '")
                            .append(ClassUtils.getDescriptiveType(convertedValue)).append('\'');
                    throw new IllegalArgumentException(msg.toString());
                } else {
                    msg.append(": no matching editors or conversion strategy found");
                    throw new IllegalStateException(msg.toString());
                }
            }
        }

        if (conversionAttemptEx != null) {
            if (editor == null && !standardConversion && requiredType != null && Object.class != requiredType) {
                throw conversionAttemptEx;
            }
            logger.debug("Original ConversionService attempt failed - ignored since " + "PropertyEditor based conversion eventually succeeded", conversionAttemptEx);
        }
        return (T) convertedValue;
    }

    private Object attemptToConvertStringToEnum(Class<?> requiredType, String trimmedValue, Object currentConvertedValue) {
        Object convertedValue = currentConvertedValue;
        if (Enum.class == requiredType && this.targetObject != null) {
            // target type is declared as raw enum, treat the trimmed value as <enum.fqn>.FIELD_NAME
            int index = trimmedValue.lastIndexOf('.');
            if (index > -1) {
                String enumType = trimmedValue.substring(0, index);
                String fieldName = trimmedValue.substring(index + 1);
                ClassLoader cl = this.targetObject.getClass().getClassLoader();
                try {
                    Class<?> enumValueType = ClassUtils.forName(enumType, cl);
                    Field enumField = enumValueType.getField(fieldName);
                    convertedValue = enumField.get(null);
                } catch (ClassNotFoundException ex) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Enum class [" + enumType + "] cannot be loaded", ex);
                    }
                } catch (Throwable ex) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Field [" + fieldName + "] isn't an enum value for type [" + enumType + "]", ex);
                    }
                }
            }
        }
        if (convertedValue == currentConvertedValue) {
            // Try field lookup as fallback: for JDK 1.5 enum or custom enum
            // with values defined as static fields. Resulting value still needs
            // to be checked, hence we don't return it right away.
            try {
                Field enumField = requiredType.getField(trimmedValue);
                ReflectionUtils.makeAccessible(enumField);
                convertedValue = enumField.get(null);
            } catch (Throwable ex) {
                if (logger.isTraceEnabled()) {
                    logger.trace("Field [" + convertedValue + "] isn't an enum value", ex);
                }
            }
        }
        return convertedValue;
    }

    /**
     * 查找给定类型的默认编辑器
     *
     * @param requiredType 要查找编辑器的类型
     * @return 相应的编辑器，如果没有，则为null
     */
    private PropertyEditor findDefaultEditor(Class<?> requiredType) {
        PropertyEditor editor = null;
        if (requiredType != null) {
            // No custom editor -> check BeanWrapperImpl's default editors.
            editor = this.propertyEditorRegistry.getDefaultEditor(requiredType);
            if (editor == null && String.class != requiredType) {
                // No BeanWrapper default editor -> check standard JavaBean editor.
                editor = BeanUtils.findEditorByConvention(requiredType);
            }
        }
        return editor;
    }

    /**
     * 使用给定的属性编辑器将值转换为所需的类型（如果需要，可以从字符串转换）
     *
     * @param oldValue     以前的值，如果可用（可能为null）
     * @param newValue     建议的新值
     * @param requiredType 我们必须转换到的类型（如果不知道，例如在集合元素的情况下，则为null）
     * @param editor       要使用的PropertyEditor
     * @return 新值，可能是类型转换的结果
     * @throws IllegalArgumentException 如果类型转换失败
     */
    private Object doConvertValue(Object oldValue, Object newValue, Class<?> requiredType, PropertyEditor editor) {
        Object convertedValue = newValue;
        if (editor != null && !(convertedValue instanceof String)) {
            // Not a String -> use PropertyEditor's setValue.
            // With standard PropertyEditors, this will return the very same object;
            // we just want to allow special PropertyEditors to override setValue
            // for type conversion from non-String values to the required type.
            try {
                editor.setValue(convertedValue);
                Object newConvertedValue = editor.getValue();
                if (newConvertedValue != convertedValue) {
                    convertedValue = newConvertedValue;
                    // Reset PropertyEditor: It already did a proper conversion.
                    // Don't use it again for a setAsText call.
                    editor = null;
                }
            } catch (Exception ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
                }
                // Swallow and proceed.
            }
        }
        Object returnValue = convertedValue;
        if (requiredType != null && !requiredType.isArray() && convertedValue instanceof String[]) {
            // Convert String array to a comma-separated String.
            // Only applies if no PropertyEditor converted the String array before.
            // The CSV String will be passed into a PropertyEditor's setAsText method, if any.
            if (logger.isTraceEnabled()) {
                logger.trace("Converting String array to comma-delimited String [" + convertedValue + "]");
            }
            convertedValue = StringUtils.arrayToCommaDelimitedString((String[]) convertedValue);
        }
        if (convertedValue instanceof String) {
            if (editor != null) {
                // Use PropertyEditor's setAsText in case of a String value.
                if (logger.isTraceEnabled()) {
                    logger.trace("Converting String to [" + requiredType + "] using property editor [" + editor + "]");
                }
                String newTextValue = (String) convertedValue;
                return doConvertTextValue(oldValue, newTextValue, editor);
            } else if (String.class == requiredType) {
                returnValue = convertedValue;
            }
        }
        return returnValue;
    }

    /**
     * 使用给定的属性编辑器转换给定的文本值
     *
     * @param oldValue     以前的值，如果可用（可能为null）
     * @param newTextValue 建议的文本值
     * @param editor       要使用的PropertyEditor
     * @return 转换后的值
     */
    private Object doConvertTextValue(Object oldValue, String newTextValue, PropertyEditor editor) {
        try {
            editor.setValue(oldValue);
        } catch (Exception ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("PropertyEditor [" + editor.getClass().getName() + "] does not support setValue call", ex);
            }
            // Swallow and proceed.
        }
        editor.setAsText(newTextValue);
        return editor.getValue();
    }

    private Object convertToTypedArray(Object input, String propertyName, Class<?> componentType) {
        if (input instanceof Collection) {
            // Convert Collection elements to array elements.
            Collection<?> coll = (Collection<?>) input;
            Object result = Array.newInstance(componentType, coll.size());
            int i = 0;
            for (Iterator<?> it = coll.iterator(); it.hasNext(); i++) {
                Object value = convertIfNecessary(buildIndexedPropertyName(propertyName, i), null, it.next(), componentType);
                Array.set(result, i, value);
            }
            return result;
        } else if (input.getClass().isArray()) {
            // Convert array elements, if necessary.
            if (componentType.equals(input.getClass().getComponentType()) && !this.propertyEditorRegistry.hasCustomEditorForElement(componentType, propertyName)) {
                return input;
            }
            int arrayLength = Array.getLength(input);
            Object result = Array.newInstance(componentType, arrayLength);
            for (int i = 0; i < arrayLength; i++) {
                Object value = convertIfNecessary(buildIndexedPropertyName(propertyName, i), null, Array.get(input, i), componentType);
                Array.set(result, i, value);
            }
            return result;
        } else {
            // A plain value: convert it to an array with a single component.
            Object result = Array.newInstance(componentType, 1);
            Object value = convertIfNecessary(buildIndexedPropertyName(propertyName, 0), null, input, componentType);
            Array.set(result, 0, value);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Collection<?> convertToTypedCollection(Collection<?> original, String propertyName, Class<?> requiredType, TypeDescriptor typeDescriptor) {
        if (!Collection.class.isAssignableFrom(requiredType)) {
            return original;
        }
        boolean approximable = CollectionFactory.isApproximableCollectionType(requiredType);
        if (!approximable && !canCreateCopy(requiredType)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Custom Collection type [" + original.getClass().getName() + "] does not allow for creating a copy - injecting original Collection as-is");
            }
            return original;
        }

        boolean originalAllowed = requiredType.isInstance(original);
        TypeDescriptor elementType = (typeDescriptor != null ? typeDescriptor.getElementTypeDescriptor() : null);
        if (elementType == null && originalAllowed && !this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
            return original;
        }

        Iterator<?> it;
        try {
            it = original.iterator();
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot access Collection of type [" + original.getClass().getName() + "] - injecting original Collection as-is: " + ex);
            }
            return original;
        }
        Collection<Object> convertedCopy;
        try {
            if (approximable) {
                convertedCopy = CollectionFactory.createApproximateCollection(original, original.size());
            } else {
                convertedCopy = (Collection<Object>) ReflectionUtils.accessibleConstructor(requiredType).newInstance();
            }
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot create copy of Collection type [" + original.getClass().getName() + "] - injecting original Collection as-is: " + ex);
            }
            return original;
        }
        for (int i = 0; it.hasNext(); i++) {
            Object element = it.next();
            String indexedPropertyName = buildIndexedPropertyName(propertyName, i);
            Object convertedElement = convertIfNecessary(
                    indexedPropertyName,
                    null,
                    element,
                    (elementType != null ? elementType.getType() : null),
                    elementType
            );
            try {
                convertedCopy.add(convertedElement);
            } catch (Throwable ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Collection type [" + original.getClass().getName() + "] seems to be read-only - injecting original Collection as-is: " + ex);
                }
                return original;
            }
            originalAllowed = originalAllowed && (element == convertedElement);
        }
        return (originalAllowed ? original : convertedCopy);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> convertToTypedMap(Map<?, ?> original, String propertyName, Class<?> requiredType, TypeDescriptor typeDescriptor) {
        if (!Map.class.isAssignableFrom(requiredType)) {
            return original;
        }
        boolean approximable = CollectionFactory.isApproximableMapType(requiredType);
        if (!approximable && !canCreateCopy(requiredType)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Custom Map type [" + original.getClass().getName() + "] does not allow for creating a copy - injecting original Map as-is");
            }
            return original;
        }
        boolean originalAllowed = requiredType.isInstance(original);
        TypeDescriptor keyType = (typeDescriptor != null ? typeDescriptor.getMapKeyTypeDescriptor() : null);
        TypeDescriptor valueType = (typeDescriptor != null ? typeDescriptor.getMapValueTypeDescriptor() : null);
        if (keyType == null && valueType == null && originalAllowed && !this.propertyEditorRegistry.hasCustomEditorForElement(null, propertyName)) {
            return original;
        }
        Iterator<?> it;
        try {
            it = original.entrySet().iterator();
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot access Map of type [" + original.getClass().getName() + "] - injecting original Map as-is: " + ex);
            }
            return original;
        }
        Map<Object, Object> convertedCopy;
        try {
            if (approximable) {
                convertedCopy = CollectionFactory.createApproximateMap(original, original.size());
            } else {
                convertedCopy = (Map<Object, Object>) ReflectionUtils.accessibleConstructor(requiredType).newInstance();
            }
        } catch (Throwable ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot create copy of Map type [" + original.getClass().getName() + "] - injecting original Map as-is: " + ex);
            }
            return original;
        }
        while (it.hasNext()) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) it.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            String keyedPropertyName = buildKeyedPropertyName(propertyName, key);
            Object convertedKey = convertIfNecessary(keyedPropertyName, null, key, (keyType != null ? keyType.getType() : null), keyType);
            Object convertedValue = convertIfNecessary(keyedPropertyName, null, value, (valueType != null ? valueType.getType() : null), valueType);
            try {
                convertedCopy.put(convertedKey, convertedValue);
            } catch (Throwable ex) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Map type [" + original.getClass().getName() + "] seems to be read-only - injecting original Map as-is: " + ex);
                }
                return original;
            }
            originalAllowed = originalAllowed && (key == convertedKey) && (value == convertedValue);
        }
        return (originalAllowed ? original : convertedCopy);
    }

    private String buildIndexedPropertyName(String propertyName, int index) {
        return (propertyName != null ? propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + index + PropertyAccessor.PROPERTY_KEY_SUFFIX : null);
    }

    private String buildKeyedPropertyName(String propertyName, Object key) {
        return (propertyName != null ? propertyName + PropertyAccessor.PROPERTY_KEY_PREFIX + key + PropertyAccessor.PROPERTY_KEY_SUFFIX : null);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean canCreateCopy(Class<?> requiredType) {
        return !requiredType.isInterface()
                && !Modifier.isAbstract(requiredType.getModifiers())
                && Modifier.isPublic(requiredType.getModifiers())
                && ClassUtils.hasConstructor(requiredType);
    }
}
