package org.clever.beans;

import org.clever.beans.propertyeditors.*;
import org.clever.core.convert.ConversionService;
import org.clever.core.io.Resource;
import org.clever.core.io.support.ResourceArrayPropertyEditor;
import org.clever.util.ClassUtils;
import org.xml.sax.InputSource;

import java.beans.PropertyEditor;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

/**
 * {@link PropertyEditorRegistry}接口的基本实现<br/>
 * 提供默认编辑器和自定义编辑器的管理。主要用作{@link BeanWrapperImpl}的基类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:07 <br/>
 *
 * @see java.beans.PropertyEditorManager
 * @see java.beans.PropertyEditorSupport#setAsText
 * @see java.beans.PropertyEditorSupport#setValue
 */
public class PropertyEditorRegistrySupport implements PropertyEditorRegistry {
    /**
     * 不初始化与XML相关的基础结构，默认值为“false”
     */
    private static final boolean shouldIgnoreXml = false; // SpringProperties.getFlag("clever.xml.ignore");

    private ConversionService conversionService;
    private boolean defaultEditorsActive = false;
    private boolean configValueEditorsActive = false;
    private Map<Class<?>, PropertyEditor> defaultEditors;
    private Map<Class<?>, PropertyEditor> overriddenDefaultEditors;
    private Map<Class<?>, PropertyEditor> customEditors;
    private Map<String, CustomEditorHolder> customEditorsForPath;
    private Map<Class<?>, PropertyEditor> customEditorCache;

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * 返回关联的ConversionService
     */
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    //---------------------------------------------------------------------
    // Management of default editors
    //---------------------------------------------------------------------

    /**
     * 激活此注册表实例的默认编辑器，允许在需要时延迟注册默认编辑器
     */
    protected void registerDefaultEditors() {
        this.defaultEditorsActive = true;
    }

    /**
     * 激活仅用于配置目的的配置值编辑器<br/>
     * 默认情况下，这些编辑器不会注册，因为它们通常不适合用于数据绑定目的。
     * 当然，您可以在任何情况下通过{@link #registerCustomEditor}单独注册它们
     */
    public void useConfigValueEditors() {
        this.configValueEditorsActive = true;
    }

    /**
     * 使用给定的属性编辑器替代指定类型的默认编辑器<br/>
     * 请注意，这与注册自定义编辑器不同，因为编辑器在语义上仍然是默认编辑器。
     * ConversionService将覆盖此类默认编辑器，而自定义编辑器通常覆盖ConversionService
     *
     * @param requiredType   属性的类型
     * @param propertyEditor 需要注册的编辑器
     * @see #registerCustomEditor(Class, PropertyEditor)
     */
    public void overrideDefaultEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        if (this.overriddenDefaultEditors == null) {
            this.overriddenDefaultEditors = new HashMap<>();
        }
        this.overriddenDefaultEditors.put(requiredType, propertyEditor);
    }

    /**
     * 检索给定属性类型的默认编辑器(如果有)<br/>
     * 如果默认编辑器处于活动状态，则延迟注册它们
     *
     * @param requiredType 属性类型
     * @see #registerDefaultEditors
     */
    public PropertyEditor getDefaultEditor(Class<?> requiredType) {
        if (!this.defaultEditorsActive) {
            return null;
        }
        if (this.overriddenDefaultEditors != null) {
            PropertyEditor editor = this.overriddenDefaultEditors.get(requiredType);
            if (editor != null) {
                return editor;
            }
        }
        if (this.defaultEditors == null) {
            createDefaultEditors();
        }
        return this.defaultEditors.get(requiredType);
    }

    /**
     * 实际注册此注册表实例的默认编辑器
     */
    private void createDefaultEditors() {
        this.defaultEditors = new HashMap<>(64);
        // Simple editors, without parameterization capabilities.
        // The JDK does not contain a default editor for any of these target types.
        this.defaultEditors.put(Charset.class, new CharsetEditor());
        this.defaultEditors.put(Class.class, new ClassEditor());
        this.defaultEditors.put(Class[].class, new ClassArrayEditor());
        this.defaultEditors.put(Currency.class, new CurrencyEditor());
        this.defaultEditors.put(File.class, new FileEditor());
        this.defaultEditors.put(InputStream.class, new InputStreamEditor());
        if (!shouldIgnoreXml) {
            this.defaultEditors.put(InputSource.class, new InputSourceEditor());
        }
        this.defaultEditors.put(Locale.class, new LocaleEditor());
        this.defaultEditors.put(Path.class, new PathEditor());
        this.defaultEditors.put(Pattern.class, new PatternEditor());
        this.defaultEditors.put(Properties.class, new PropertiesEditor());
        this.defaultEditors.put(Reader.class, new ReaderEditor());
        this.defaultEditors.put(Resource[].class, new ResourceArrayPropertyEditor());
        this.defaultEditors.put(TimeZone.class, new TimeZoneEditor());
        this.defaultEditors.put(URI.class, new URIEditor());
        this.defaultEditors.put(URL.class, new URLEditor());
        this.defaultEditors.put(UUID.class, new UUIDEditor());
        this.defaultEditors.put(ZoneId.class, new ZoneIdEditor());

        // Default instances of collection editors.
        // Can be overridden by registering custom instances of those as custom editors.
        this.defaultEditors.put(Collection.class, new CustomCollectionEditor(Collection.class));
        this.defaultEditors.put(Set.class, new CustomCollectionEditor(Set.class));
        this.defaultEditors.put(SortedSet.class, new CustomCollectionEditor(SortedSet.class));
        this.defaultEditors.put(List.class, new CustomCollectionEditor(List.class));
        this.defaultEditors.put(SortedMap.class, new CustomMapEditor(SortedMap.class));

        // Default editors for primitive arrays.
        this.defaultEditors.put(byte[].class, new ByteArrayPropertyEditor());
        this.defaultEditors.put(char[].class, new CharArrayPropertyEditor());

        // The JDK does not contain a default editor for char!
        this.defaultEditors.put(char.class, new CharacterEditor(false));
        this.defaultEditors.put(Character.class, new CharacterEditor(true));

        // clever's CustomBooleanEditor accepts more flag values than the JDK's default editor.
        this.defaultEditors.put(boolean.class, new CustomBooleanEditor(false));
        this.defaultEditors.put(Boolean.class, new CustomBooleanEditor(true));

        // The JDK does not contain default editors for number wrapper types!
        // Override JDK primitive number editors with our own CustomNumberEditor.
        this.defaultEditors.put(byte.class, new CustomNumberEditor(Byte.class, false));
        this.defaultEditors.put(Byte.class, new CustomNumberEditor(Byte.class, true));
        this.defaultEditors.put(short.class, new CustomNumberEditor(Short.class, false));
        this.defaultEditors.put(Short.class, new CustomNumberEditor(Short.class, true));
        this.defaultEditors.put(int.class, new CustomNumberEditor(Integer.class, false));
        this.defaultEditors.put(Integer.class, new CustomNumberEditor(Integer.class, true));
        this.defaultEditors.put(long.class, new CustomNumberEditor(Long.class, false));
        this.defaultEditors.put(Long.class, new CustomNumberEditor(Long.class, true));
        this.defaultEditors.put(float.class, new CustomNumberEditor(Float.class, false));
        this.defaultEditors.put(Float.class, new CustomNumberEditor(Float.class, true));
        this.defaultEditors.put(double.class, new CustomNumberEditor(Double.class, false));
        this.defaultEditors.put(Double.class, new CustomNumberEditor(Double.class, true));
        this.defaultEditors.put(BigDecimal.class, new CustomNumberEditor(BigDecimal.class, true));
        this.defaultEditors.put(BigInteger.class, new CustomNumberEditor(BigInteger.class, true));

        // Only register config value editors if explicitly requested.
        if (this.configValueEditorsActive) {
            StringArrayPropertyEditor sae = new StringArrayPropertyEditor();
            this.defaultEditors.put(String[].class, sae);
            this.defaultEditors.put(short[].class, sae);
            this.defaultEditors.put(int[].class, sae);
            this.defaultEditors.put(long[].class, sae);
        }
    }

    /**
     * 将在此实例中注册的默认编辑器复制到给定的目标注册表
     *
     * @param target 要复制到的目标注册表
     */
    protected void copyDefaultEditorsTo(PropertyEditorRegistrySupport target) {
        target.defaultEditorsActive = this.defaultEditorsActive;
        target.configValueEditorsActive = this.configValueEditorsActive;
        target.defaultEditors = this.defaultEditors;
        target.overriddenDefaultEditors = this.overriddenDefaultEditors;
    }

    //---------------------------------------------------------------------
    // Management of custom editors
    //---------------------------------------------------------------------

    @Override
    public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        registerCustomEditor(requiredType, null, propertyEditor);
    }

    @Override
    public void registerCustomEditor(Class<?> requiredType, String propertyPath, PropertyEditor propertyEditor) {
        if (requiredType == null && propertyPath == null) {
            throw new IllegalArgumentException("Either requiredType or propertyPath is required");
        }
        if (propertyPath != null) {
            if (this.customEditorsForPath == null) {
                this.customEditorsForPath = new LinkedHashMap<>(16);
            }
            this.customEditorsForPath.put(propertyPath, new CustomEditorHolder(propertyEditor, requiredType));
        } else {
            if (this.customEditors == null) {
                this.customEditors = new LinkedHashMap<>(16);
            }
            this.customEditors.put(requiredType, propertyEditor);
            this.customEditorCache = null;
        }
    }

    @Override
    public PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath) {
        Class<?> requiredTypeToUse = requiredType;
        if (propertyPath != null) {
            if (this.customEditorsForPath != null) {
                // Check property-specific editor first.
                PropertyEditor editor = getCustomEditor(propertyPath, requiredType);
                if (editor == null) {
                    List<String> strippedPaths = new ArrayList<>();
                    addStrippedPropertyPaths(strippedPaths, "", propertyPath);
                    for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editor == null; ) {
                        String strippedPath = it.next();
                        editor = getCustomEditor(strippedPath, requiredType);
                    }
                }
                if (editor != null) {
                    return editor;
                }
            }
            if (requiredType == null) {
                requiredTypeToUse = getPropertyType(propertyPath);
            }
        }
        // No property-specific editor -> check type-specific editor.
        return getCustomEditor(requiredTypeToUse);
    }

    /**
     * 确定此注册表是否包含指定数组/集合元素的自定义编辑器
     *
     * @param elementType  元素的目标类型(如果未知，则可以为null)
     * @param propertyPath 属性路径(通常是数组/集合的路径；如果未知，则可以为null)
     * @return 是否找到匹配的自定义编辑器
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasCustomEditorForElement(Class<?> elementType, String propertyPath) {
        if (propertyPath != null && this.customEditorsForPath != null) {
            for (Map.Entry<String, CustomEditorHolder> entry : this.customEditorsForPath.entrySet()) {
                if (PropertyAccessorUtils.matchesProperty(entry.getKey(), propertyPath) && entry.getValue().getPropertyEditor(elementType) != null) {
                    return true;
                }
            }
        }
        // No property-specific editor -> check type-specific editor.
        return (elementType != null && this.customEditors != null && this.customEditors.containsKey(elementType));
    }

    /**
     * 确定给定属性路径的属性类型<br/>
     * 如果没有指定所需的类型，则由{@link #findCustomEditor}调用，以便能够找到特定于类型的编辑器，即使只给出了属性路径。<br/>
     * 默认实现始终返回null。BeanRapperImpl使用由BeanWrapper接口定义的标准{@code getPropertyType}方法来覆盖这一点
     *
     * @param propertyPath 用于确定类型的属性路径
     * @return 属性的类型，如果不可确定，则为null
     * @see BeanWrapper#getPropertyType(String)
     */
    protected Class<?> getPropertyType(String propertyPath) {
        return null;
    }

    /**
     * 获取已为给定属性注册的自定义编辑器
     *
     * @param propertyName 要查找的属性路径
     * @param requiredType 要查找的类型
     * @return 自定义编辑器，如果没有特定于此属性，则为null
     */
    private PropertyEditor getCustomEditor(String propertyName, Class<?> requiredType) {
        CustomEditorHolder holder = (this.customEditorsForPath != null ? this.customEditorsForPath.get(propertyName) : null);
        return (holder != null ? holder.getPropertyEditor(requiredType) : null);
    }

    /**
     * 获取给定类型的自定义编辑器。
     * 如果找不到直接匹配，请尝试超类的自定义编辑器(在任何情况下都可以通过{@code getAsText}将值呈现为字符串)
     *
     * @param requiredType 要查找的类型
     * @return 自定义编辑器，如果找不到此类型的编辑器，则为null
     * @see java.beans.PropertyEditor#getAsText()
     */
    private PropertyEditor getCustomEditor(Class<?> requiredType) {
        if (requiredType == null || this.customEditors == null) {
            return null;
        }
        // Check directly registered editor for type.
        PropertyEditor editor = this.customEditors.get(requiredType);
        if (editor == null) {
            // Check cached editor for type, registered for superclass or interface.
            if (this.customEditorCache != null) {
                editor = this.customEditorCache.get(requiredType);
            }
            if (editor == null) {
                // Find editor for superclass or interface.
                for (Map.Entry<Class<?>, PropertyEditor> entry : this.customEditors.entrySet()) {
                    if (editor != null) {
                        break;
                    }
                    Class<?> key = entry.getKey();
                    if (key.isAssignableFrom(requiredType)) {
                        editor = entry.getValue();
                        // Cache editor for search type, to avoid the overhead
                        // of repeated assignable-from checks.
                        if (this.customEditorCache == null) {
                            this.customEditorCache = new HashMap<>();
                        }
                        this.customEditorCache.put(requiredType, editor);
                    }
                }
            }
        }
        return editor;
    }

    /**
     * 从已注册的自定义编辑器中猜测指定属性的属性类型(前提是它们已注册为特定类型)
     *
     * @param propertyName 属性的名称
     * @return 属性类型，如果不可确定，则为null
     */
    protected Class<?> guessPropertyTypeFromEditors(String propertyName) {
        if (this.customEditorsForPath != null) {
            CustomEditorHolder editorHolder = this.customEditorsForPath.get(propertyName);
            if (editorHolder == null) {
                List<String> strippedPaths = new ArrayList<>();
                addStrippedPropertyPaths(strippedPaths, "", propertyName);
                for (Iterator<String> it = strippedPaths.iterator(); it.hasNext() && editorHolder == null; ) {
                    String strippedName = it.next();
                    editorHolder = this.customEditorsForPath.get(strippedName);
                }
            }
            if (editorHolder != null) {
                return editorHolder.getRegisteredType();
            }
        }
        return null;
    }

    /**
     * 将在此实例中注册的自定义编辑器复制到给定的目标注册表
     *
     * @param target         要复制到的目标注册表
     * @param nestedProperty 目标注册表的嵌套属性路径(如果有)。如果此值为非null，则仅复制为该嵌套属性下的路径注册的编辑器。如果此值为null，则将复制所有编辑器
     */
    protected void copyCustomEditorsTo(PropertyEditorRegistry target, String nestedProperty) {
        String actualPropertyName = (nestedProperty != null ? PropertyAccessorUtils.getPropertyName(nestedProperty) : null);
        if (this.customEditors != null) {
            this.customEditors.forEach(target::registerCustomEditor);
        }
        if (this.customEditorsForPath != null) {
            this.customEditorsForPath.forEach((editorPath, editorHolder) -> {
                if (nestedProperty != null) {
                    int pos = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(editorPath);
                    if (pos != -1) {
                        String editorNestedProperty = editorPath.substring(0, pos);
                        String editorNestedPath = editorPath.substring(pos + 1);
                        if (editorNestedProperty.equals(nestedProperty) || editorNestedProperty.equals(actualPropertyName)) {
                            target.registerCustomEditor(editorHolder.getRegisteredType(), editorNestedPath, editorHolder.getPropertyEditor());
                        }
                    }
                } else {
                    target.registerCustomEditor(editorHolder.getRegisteredType(), editorPath, editorHolder.getPropertyEditor());
                }
            });
        }
    }

    /**
     * 添加具有剥离键和/或索引的所有变体的属性路径。使用嵌套路径递归调用自身
     *
     * @param strippedPaths 要添加到的结果列表
     * @param nestedPath    当前嵌套路径
     * @param propertyPath  用于检查要剥离的键/索引的属性路径
     */
    private void addStrippedPropertyPaths(List<String> strippedPaths, String nestedPath, String propertyPath) {
        int startIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR);
        if (startIndex != -1) {
            int endIndex = propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR);
            if (endIndex != -1) {
                String prefix = propertyPath.substring(0, startIndex);
                String key = propertyPath.substring(startIndex, endIndex + 1);
                String suffix = propertyPath.substring(endIndex + 1);
                // Strip the first key.
                strippedPaths.add(nestedPath + prefix + suffix);
                // Search for further keys to strip, with the first key stripped.
                addStrippedPropertyPaths(strippedPaths, nestedPath + prefix, suffix);
                // Search for further keys to strip, with the first key not stripped.
                addStrippedPropertyPaths(strippedPaths, nestedPath + prefix + key, suffix);
            }
        }
    }

    /**
     * 具有属性名称的注册自定义编辑器的持有者。保留PropertyEditor本身及其注册的类型
     */
    private static final class CustomEditorHolder {
        private final PropertyEditor propertyEditor;
        private final Class<?> registeredType;

        private CustomEditorHolder(PropertyEditor propertyEditor, Class<?> registeredType) {
            this.propertyEditor = propertyEditor;
            this.registeredType = registeredType;
        }

        private PropertyEditor getPropertyEditor() {
            return this.propertyEditor;
        }

        private Class<?> getRegisteredType() {
            return this.registeredType;
        }

        private PropertyEditor getPropertyEditor(Class<?> requiredType) {
            // Special case: If no required type specified, which usually only happens for
            // Collection elements, or required type is not assignable to registered type,
            // which usually only happens for generic properties of type Object -
            // then return PropertyEditor if not registered for Collection or array type.
            // (If not registered for Collection or array, it is assumed to be intended
            // for elements.)
            if (this.registeredType == null
                    || (requiredType != null && (ClassUtils.isAssignable(this.registeredType, requiredType) || ClassUtils.isAssignable(requiredType, this.registeredType)))
                    || (requiredType == null && (!Collection.class.isAssignableFrom(this.registeredType) && !this.registeredType.isArray()))) {
                return this.propertyEditor;
            } else {
                return null;
            }
        }
    }
}
