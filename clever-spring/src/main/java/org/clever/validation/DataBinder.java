package org.clever.validation;

import org.clever.beans.*;
import org.clever.core.MethodParameter;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.format.Formatter;
import org.clever.format.support.FormatterPropertyEditorAdapter;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.PatternMatchUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 用于设置目标对象属性值的绑定器，包括对验证和绑定结果分析的支持。
 * <p>可以通过指定允许的字段模式、必填字段、自定义编辑器等来自定义绑定过程。
 * <p>这个通用数据绑定器可以在任何类型的环境中使用。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 20:40 <br/>
 */
public class DataBinder implements PropertyEditorRegistry, TypeConverter {
    protected static final Logger logger = LoggerFactory.getLogger(DataBinder.class);
    /**
     * 用于绑定的默认对象名称：“target”
     */
    public static final String DEFAULT_OBJECT_NAME = "target";
    /**
     * 数组和集合增长的默认限制：256
     */
    public static final int DEFAULT_AUTO_GROW_COLLECTION_LIMIT = 256;

    private final Object target;
    private final String objectName;
    private AbstractPropertyBindingResult bindingResult;
    private boolean directFieldAccess = false;
    private SimpleTypeConverter typeConverter;
    private boolean ignoreUnknownFields = true;
    private boolean ignoreInvalidFields = false;
    private boolean autoGrowNestedPaths = true;
    private int autoGrowCollectionLimit = DEFAULT_AUTO_GROW_COLLECTION_LIMIT;
    private String[] allowedFields;
    private String[] disallowedFields;
    private String[] requiredFields;
    private ConversionService conversionService;
    private MessageCodesResolver messageCodesResolver;
    private BindingErrorProcessor bindingErrorProcessor = new DefaultBindingErrorProcessor();
    private final List<Validator> validators = new ArrayList<>();

    /**
     * 使用默认对象名称创建一个新的 DataBinder 实例。
     *
     * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @see #DEFAULT_OBJECT_NAME
     */
    public DataBinder(Object target) {
        this(target, DEFAULT_OBJECT_NAME);
    }

    /**
     * 创建一个新的 DataBinder 实例
     *
     * @param target     要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @param objectName 目标对象的名称
     */
    public DataBinder(Object target, String objectName) {
        this.target = ObjectUtils.unwrapOptional(target);
        this.objectName = objectName;
    }

    /**
     * 返回包装的目标对象。
     */
    public Object getTarget() {
        return this.target;
    }

    /**
     * 返回绑定对象的名称。
     */
    public String getObjectName() {
        return this.objectName;
    }

    /**
     * 设置此活页夹是否应尝试“自动增长”包含空值的嵌套路径。
     * <p>如果为“true”，空路径位置将填充默认对象值并遍历，而不是导致异常。当访问越界索引时，此标志还启用集合元素的自动增长。
     * <p>在标准 DataBinder 上默认为“true”。请注意，此功能支持 bean 属性访问（DataBinder 的默认模式）和字段访问。
     *
     * @see #initBeanPropertyAccess()
     */
    public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call setAutoGrowNestedPaths before other configuration methods");
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    /**
     * 返回是否已激活嵌套路径的“自动增长”。
     */
    public boolean isAutoGrowNestedPaths() {
        return this.autoGrowNestedPaths;
    }

    /**
     * 指定数组和集合自动增长的限制。
     * <p>默认值为 256，防止在大索引的情况下出现 OutOfMemoryErrors。如果您的自动增长需求异常高，请提高此限制。
     *
     * @see #initBeanPropertyAccess()
     */
    public void setAutoGrowCollectionLimit(int autoGrowCollectionLimit) {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call setAutoGrowCollectionLimit before other configuration methods");
        this.autoGrowCollectionLimit = autoGrowCollectionLimit;
    }

    /**
     * 返回数组和集合自动增长的当前限制。
     */
    public int getAutoGrowCollectionLimit() {
        return this.autoGrowCollectionLimit;
    }

    /**
     * 为此 DataBinder 初始化标准 JavaBean 属性访问。
     * <p>这是默认设置；显式调用只会导致急切的初始化。
     *
     * @see #initDirectFieldAccess()
     * @see #createBeanPropertyBindingResult()
     */
    public void initBeanPropertyAccess() {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call initBeanPropertyAccess before other configuration methods");
        this.directFieldAccess = false;
    }

    /**
     * 使用标准 JavaBean 属性访问创建 {@link AbstractPropertyBindingResult} 实例。
     */
    protected AbstractPropertyBindingResult createBeanPropertyBindingResult() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(getTarget(), getObjectName(), isAutoGrowNestedPaths(), getAutoGrowCollectionLimit());
        if (this.conversionService != null) {
            result.initConversion(this.conversionService);
        }
        if (this.messageCodesResolver != null) {
            result.setMessageCodesResolver(this.messageCodesResolver);
        }
        return result;
    }

    /**
     * 初始化此 DataBinder 的直接字段访问，作为默认 bean 属性访问的替代方法。
     *
     * @see #initBeanPropertyAccess()
     * @see #createDirectFieldBindingResult()
     */
    public void initDirectFieldAccess() {
        Assert.state(this.bindingResult == null, "DataBinder is already initialized - call initDirectFieldAccess before other configuration methods");
        this.directFieldAccess = true;
    }

    /**
     * 使用直接字段访问创建 {@link AbstractPropertyBindingResult} 实例。
     */
    protected AbstractPropertyBindingResult createDirectFieldBindingResult() {
        DirectFieldBindingResult result = new DirectFieldBindingResult(getTarget(), getObjectName(), isAutoGrowNestedPaths());
        if (this.conversionService != null) {
            result.initConversion(this.conversionService);
        }
        if (this.messageCodesResolver != null) {
            result.setMessageCodesResolver(this.messageCodesResolver);
        }
        return result;
    }

    /**
     * 返回此 DataBinder 持有的内部 BindingResult，作为 AbstractPropertyBindingResult。
     */
    protected AbstractPropertyBindingResult getInternalBindingResult() {
        if (this.bindingResult == null) {
            this.bindingResult = (this.directFieldAccess ? createDirectFieldBindingResult() : createBeanPropertyBindingResult());
        }
        return this.bindingResult;
    }

    /**
     * 返回此活页夹的 BindingResult 的基础 PropertyAccessor。
     */
    protected ConfigurablePropertyAccessor getPropertyAccessor() {
        return getInternalBindingResult().getPropertyAccessor();
    }

    /**
     * 返回此活页夹的底层 SimpleTypeConverter。
     */
    protected SimpleTypeConverter getSimpleTypeConverter() {
        if (this.typeConverter == null) {
            this.typeConverter = new SimpleTypeConverter();
            if (this.conversionService != null) {
                this.typeConverter.setConversionService(this.conversionService);
            }
        }
        return this.typeConverter;
    }

    /**
     * 返回此活页夹的 BindingResult 的基础 TypeConverter。
     */
    protected PropertyEditorRegistry getPropertyEditorRegistry() {
        if (getTarget() != null) {
            return getInternalBindingResult().getPropertyAccessor();
        } else {
            return getSimpleTypeConverter();
        }
    }

    /**
     * 返回此活页夹的 BindingResult 的基础 TypeConverter。
     */
    protected TypeConverter getTypeConverter() {
        if (getTarget() != null) {
            return getInternalBindingResult().getPropertyAccessor();
        } else {
            return getSimpleTypeConverter();
        }
    }

    /**
     * 返回由此 DataBinder 创建的 BindingResult 实例。这允许在绑定操作后方便地访问绑定结果。
     *
     * @return BindingResult 实例，被视为 BindingResult 或 Errors 实例（Errors 是 BindingResult 的超接口）
     * @see Errors
     * @see #bind
     */
    public BindingResult getBindingResult() {
        return getInternalBindingResult();
    }

    /**
     * 设置是否忽略未知字段，即是否忽略目标对象中没有相应字段的绑定参数。
     * <p>默认为“true”。关闭此选项以强制所有绑定参数必须在目标对象中具有匹配字段。
     * <p>请注意，此设置仅适用于此 DataBinder 上的<i>绑定</i>操作，不适用于通过其{@link #getBindingResult() BindingResult}<i>检索</i>值。
     *
     * @see #bind
     */
    public void setIgnoreUnknownFields(boolean ignoreUnknownFields) {
        this.ignoreUnknownFields = ignoreUnknownFields;
    }

    /**
     * 返回绑定时是否忽略未知字段。
     */
    public boolean isIgnoreUnknownFields() {
        return this.ignoreUnknownFields;
    }

    /**
     * 设置是否忽略无效字段，即是否忽略目标对象中对应字段不可访问的绑定参数（例如由于嵌套路径中的空值）。
     * <p>默认为“假”。启用此选项可忽略目标对象图不存在部分中嵌套对象的绑定参数。
     * <p>请注意，此设置仅适用于此 DataBinder 上的<i>绑定</i>操作，不适用于通过其{@link #getBindingResult() BindingResult}<i>检索</i>值。
     *
     * @see #bind
     */
    public void setIgnoreInvalidFields(boolean ignoreInvalidFields) {
        this.ignoreInvalidFields = ignoreInvalidFields;
    }

    /**
     * 返回绑定时是否忽略无效字段。
     */
    public boolean isIgnoreInvalidFields() {
        return this.ignoreInvalidFields;
    }

    /**
     * 注册应该允许绑定的字段模式。
     * <p>默认为所有字段。
     * <p>对此进行限制，例如在绑定 HTTP 请求参数时避免恶意用户进行不必要的修改。
     * <p>支持 {@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"} 和 {@code "xxx*yyy"} 匹配（具有任意数量的模式部分），以及直接平等。
     * <p>此方法的默认实现以 {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) 规范} 形式存储允许的字段模式。因此，覆盖此方法的子类必须考虑到这一点。
     * <p>可以通过覆盖 {@link #isAllowed} 方法来实现更复杂的匹配。
     * <p>或者，指定一个<i>不允许</i>字段模式的列表。
     *
     * @param allowedFields 允许的字段模式数组
     * @see #setDisallowedFields
     * @see #isAllowed(String)
     */
    public void setAllowedFields(String... allowedFields) {
        this.allowedFields = PropertyAccessorUtils.canonicalPropertyNames(allowedFields);
    }

    /**
     * 返回应该允许绑定的字段模式。
     *
     * @return 允许的字段模式数组
     * @see #setAllowedFields(String...)
     */
    public String[] getAllowedFields() {
        return this.allowedFields;
    }

    /**
     * 注册不应<i>不允许</i>绑定的字段模式。
     * <p>默认为无。
     * <p>将字段标记为不允许，例如在绑定 HTTP 请求参数时避免恶意用户进行不必要的修改。
     * <p>支持 {@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"} 和 {@code "xxx*yyy"} 匹配（具有任意数量的模式部分），以及直接平等。
     * <p>此方法的默认实现以 {@linkplain PropertyAccessorUtils#canonicalPropertyName(String) 规范} 形式存储不允许的字段模式。
     * 默认实现还将不允许的字段模式转换为 {@linkplain String#toLowerCase() lowercase} 以支持 {@link #isAllowed} 中不区分大小写的模式匹配。
     * 因此，覆盖此方法的子类必须考虑这两种转换。
     * <p>可以通过覆盖 {@link #isAllowed} 方法来实现更复杂的匹配。
     * <p>或者，指定<i>允许</i>字段模式的列表。
     *
     * @param disallowedFields 不允许的字段模式数组
     * @see #setAllowedFields
     * @see #isAllowed(String)
     */
    public void setDisallowedFields(String... disallowedFields) {
        if (disallowedFields == null) {
            this.disallowedFields = null;
        } else {
            String[] fieldPatterns = new String[disallowedFields.length];
            for (int i = 0; i < fieldPatterns.length; i++) {
                fieldPatterns[i] = PropertyAccessorUtils.canonicalPropertyName(disallowedFields[i]).toLowerCase();
            }
            this.disallowedFields = fieldPatterns;
        }
    }

    /**
     * 返回不应<i>不允许</i>绑定的字段模式。
     *
     * @return 不允许的字段模式数组
     * @see #setDisallowedFields(String...)
     */
    public String[] getDisallowedFields() {
        return this.disallowedFields;
    }

    /**
     * 注册每个绑定过程所需的字段。
     * <p>如果指定字段之一未包含在传入属性值列表中，则会创建相应的“missing field”错误，错误代码为“required”（由默认绑定错误处理器）。
     *
     * @param requiredFields 字段名称数组
     * @see #setBindingErrorProcessor
     * @see DefaultBindingErrorProcessor#MISSING_FIELD_ERROR_CODE
     */
    public void setRequiredFields(String... requiredFields) {
        this.requiredFields = PropertyAccessorUtils.canonicalPropertyNames(requiredFields);
        if (logger.isDebugEnabled()) {
            logger.debug("DataBinder requires binding of required fields [" + StringUtils.arrayToCommaDelimitedString(requiredFields) + "]");
        }
    }

    /**
     * 返回每个绑定过程所需的字段。
     *
     * @return 字段名称数组
     */
    public String[] getRequiredFields() {
        return this.requiredFields;
    }

    /**
     * 设置用于将错误解析为消息代码的策略。将给定策略应用于基础错误持有者。
     * <p>默认是 DefaultMessageCodesResolver。
     *
     * @see BeanPropertyBindingResult#setMessageCodesResolver
     * @see DefaultMessageCodesResolver
     */
    public void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
        Assert.state(this.messageCodesResolver == null, "DataBinder is already initialized with MessageCodesResolver");
        this.messageCodesResolver = messageCodesResolver;
        if (this.bindingResult != null && messageCodesResolver != null) {
            this.bindingResult.setMessageCodesResolver(messageCodesResolver);
        }
    }

    /**
     * 设置用于处理绑定错误的策略，即必填字段错误和 {@code PropertyAccessException}。
     * <p>默认是 DefaultBindingErrorProcessor。
     *
     * @see DefaultBindingErrorProcessor
     */
    public void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
        Assert.notNull(bindingErrorProcessor, "BindingErrorProcessor must not be null");
        this.bindingErrorProcessor = bindingErrorProcessor;
    }

    /**
     * 返回处理绑定错误的策略。
     */
    public BindingErrorProcessor getBindingErrorProcessor() {
        return this.bindingErrorProcessor;
    }

    /**
     * 将验证器设置为在每个绑定步骤后应用。
     *
     * @see #addValidators(Validator...)
     * @see #replaceValidators(Validator...)
     */
    public void setValidator(Validator validator) {
        assertValidators(validator);
        this.validators.clear();
        if (validator != null) {
            this.validators.add(validator);
        }
    }

    private void assertValidators(Validator... validators) {
        Object target = getTarget();
        for (Validator validator : validators) {
            if (validator != null && (target != null && !validator.supports(target.getClass()))) {
                throw new IllegalStateException("Invalid target for Validator [" + validator + "]: " + target);
            }
        }
    }

    /**
     * 在每个绑定步骤之后添加要应用的验证器。
     *
     * @see #setValidator(Validator)
     * @see #replaceValidators(Validator...)
     */
    public void addValidators(Validator... validators) {
        assertValidators(validators);
        this.validators.addAll(Arrays.asList(validators));
    }

    /**
     * 在每个绑定步骤后替换要应用的验证器。
     *
     * @see #setValidator(Validator)
     * @see #addValidators(Validator...)
     */
    public void replaceValidators(Validator... validators) {
        assertValidators(validators);
        this.validators.clear();
        this.validators.addAll(Arrays.asList(validators));
    }

    /**
     * 在每个绑定步骤后返回要应用的主验证器（如果有）。
     */
    public Validator getValidator() {
        return (!this.validators.isEmpty() ? this.validators.get(0) : null);
    }

    /**
     * 返回验证器以在数据绑定后应用。
     */
    public List<Validator> getValidators() {
        return Collections.unmodifiableList(this.validators);
    }

    //---------------------------------------------------------------------
    // PropertyEditorRegistry/TypeConverter 接口的实现
    //---------------------------------------------------------------------

    /**
     * 指定一个 Spring 3.0 ConversionService 用于转换属性值，作为 JavaBeans PropertyEditors 的替代。
     */
    public void setConversionService(ConversionService conversionService) {
        Assert.state(this.conversionService == null, "DataBinder is already initialized with ConversionService");
        this.conversionService = conversionService;
        if (this.bindingResult != null && conversionService != null) {
            this.bindingResult.initConversion(conversionService);
        }
    }

    /**
     * 返回关联的 ConversionService（如果有）。
     */
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    /**
     * 添加自定义格式化程序，将其应用于与 {@link Formatter} 声明的类型匹配的所有字段。
     * <p>在幕后注册相应的 {@link PropertyEditor} 适配器。
     *
     * @param formatter 要添加的格式化程序，通常为特定类型声明
     * @see #registerCustomEditor(Class, PropertyEditor)
     */
    public void addCustomFormatter(Formatter<?> formatter) {
        FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
        getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
    }

    /**
     * 为 {@link Formatter} 类中指定的字段类型添加自定义格式化程序，仅将其应用于指定字段（如果有），否则应用于所有字段。
     * <p>在幕后注册相应的 {@link PropertyEditor} 适配器。
     *
     * @param formatter 要添加的格式化程序，通常为特定类型声明
     * @param fields    将格式化程序应用于的字段，如果应用于所有字段则为无
     * @see #registerCustomEditor(Class, String, PropertyEditor)
     */
    public void addCustomFormatter(Formatter<?> formatter, String... fields) {
        FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
        Class<?> fieldType = adapter.getFieldType();
        if (ObjectUtils.isEmpty(fields)) {
            getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
        } else {
            for (String field : fields) {
                getPropertyEditorRegistry().registerCustomEditor(fieldType, field, adapter);
            }
        }
    }

    /**
     * 添加自定义格式化程序，仅将其应用于指定的字段类型（如果有），否则应用于与 {@link Formatter} 声明的类型匹配的所有字段。
     * <p>在幕后注册相应的 {@link PropertyEditor} 适配器。
     *
     * @param formatter  要添加的格式化程序（如果字段类型明确指定为参数，则不需要一般声明字段类型）
     * @param fieldTypes 应用格式化程序的字段类型，如果从给定的 {@link Formatter} 实现类派生，则没有
     * @see #registerCustomEditor(Class, PropertyEditor)
     */
    public void addCustomFormatter(Formatter<?> formatter, Class<?>... fieldTypes) {
        FormatterPropertyEditorAdapter adapter = new FormatterPropertyEditorAdapter(formatter);
        if (ObjectUtils.isEmpty(fieldTypes)) {
            getPropertyEditorRegistry().registerCustomEditor(adapter.getFieldType(), adapter);
        } else {
            for (Class<?> fieldType : fieldTypes) {
                getPropertyEditorRegistry().registerCustomEditor(fieldType, adapter);
            }
        }
    }

    @Override
    public void registerCustomEditor(Class<?> requiredType, PropertyEditor propertyEditor) {
        getPropertyEditorRegistry().registerCustomEditor(requiredType, propertyEditor);
    }

    @Override
    public void registerCustomEditor(Class<?> requiredType, String field, PropertyEditor propertyEditor) {
        getPropertyEditorRegistry().registerCustomEditor(requiredType, field, propertyEditor);
    }

    @Override
    public PropertyEditor findCustomEditor(Class<?> requiredType, String propertyPath) {
        return getPropertyEditorRegistry().findCustomEditor(requiredType, propertyPath);
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType);
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType, methodParam);
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType, field);
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType, TypeDescriptor typeDescriptor) throws TypeMismatchException {
        return getTypeConverter().convertIfNecessary(value, requiredType, typeDescriptor);
    }

    /**
     * 将给定的属性值绑定到该绑定器的目标。
     * <p>此调用可以创建字段错误，表示基本绑定错误，如必填字段（代码“required”）或值和 bean 属性之间的类型不匹配（代码“typeMismatch”）。
     * <p>请注意，给定的 PropertyValues 应该是一次性实例：为了提高效率，如果它实现了 MutablePropertyValues 接口，它将被修改为仅包含允许的字段；
     * 否则，将为此创建一个内部可变副本。如果您希望原始实例在任何情况下都保持不变，请传入 PropertyValues 的副本。
     *
     * @param pvs 要绑定的属性值
     * @see #doBind(org.clever.beans.MutablePropertyValues)
     */
    public void bind(PropertyValues pvs) {
        MutablePropertyValues mpvs = (pvs instanceof MutablePropertyValues ? (MutablePropertyValues) pvs : new MutablePropertyValues(pvs));
        doBind(mpvs);
    }

    /**
     * 绑定过程的实际实现，使用传入的 MutablePropertyValues 实例。
     *
     * @param mpvs 要绑定的属性值，作为 MutablePropertyValues 实例
     * @see #checkAllowedFields
     * @see #checkRequiredFields
     * @see #applyPropertyValues
     */
    protected void doBind(MutablePropertyValues mpvs) {
        checkAllowedFields(mpvs);
        checkRequiredFields(mpvs);
        applyPropertyValues(mpvs);
    }

    /**
     * 根据允许的字段检查给定的属性值，删除不允许的字段的值。
     *
     * @param mpvs 要绑定的属性值（可以修改）
     * @see #getAllowedFields
     * @see #isAllowed(String)
     */
    protected void checkAllowedFields(MutablePropertyValues mpvs) {
        PropertyValue[] pvs = mpvs.getPropertyValues();
        for (PropertyValue pv : pvs) {
            String field = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
            if (!isAllowed(field)) {
                mpvs.removePropertyValue(pv);
                getBindingResult().recordSuppressedField(field);
                if (logger.isDebugEnabled()) {
                    logger.debug("Field [" + field + "] has been removed from PropertyValues " + "and will not be bound, because it has not been found in the list of allowed fields");
                }
            }
        }
    }

    /**
     * 确定给定字段是否允许绑定。
     * <p>为每个传入的属性值调用。
     * <p>检查 {@code "xxx*"}、{@code "*xxx"}、{@code "*xxx*"} 和 {@code "xxx*yyy"} 匹配（具有任意数量的模式部分） ，以及直接相等，在允许的字段模式和不允许的字段模式的配置列表中。
     * <p>匹配允许的字段模式是区分大小写的；然而，与不允许的字段模式匹配是不区分大小写的。
     * <p>与不允许的模式匹配的字段将不会被接受，即使它也恰好与允许列表中的模式匹配。
     * <p>可以在子类中重写，但必须注意遵守上述约定。
     *
     * @param field 要检查的字段
     * @return {@code true} 如果允许该字段
     * @see #setAllowedFields
     * @see #setDisallowedFields
     * @see org.clever.util.PatternMatchUtils#simpleMatch(String, String)
     */
    protected boolean isAllowed(String field) {
        String[] allowed = getAllowedFields();
        String[] disallowed = getDisallowedFields();
        return (ObjectUtils.isEmpty(allowed) || PatternMatchUtils.simpleMatch(allowed, field)) && (ObjectUtils.isEmpty(disallowed) || !PatternMatchUtils.simpleMatch(disallowed, field.toLowerCase()));
    }

    /**
     * 根据必填字段检查给定的属性值，在适当的地方生成缺失字段错误。
     *
     * @param mpvs 要绑定的属性值（可以修改）
     * @see #getRequiredFields
     * @see #getBindingErrorProcessor
     * @see BindingErrorProcessor#processMissingFieldError
     */
    protected void checkRequiredFields(MutablePropertyValues mpvs) {
        String[] requiredFields = getRequiredFields();
        if (!ObjectUtils.isEmpty(requiredFields)) {
            Map<String, PropertyValue> propertyValues = new HashMap<>();
            PropertyValue[] pvs = mpvs.getPropertyValues();
            for (PropertyValue pv : pvs) {
                String canonicalName = PropertyAccessorUtils.canonicalPropertyName(pv.getName());
                propertyValues.put(canonicalName, pv);
            }
            for (String field : requiredFields) {
                PropertyValue pv = propertyValues.get(field);
                boolean empty = (pv == null || pv.getValue() == null);
                if (!empty) {
                    if (pv.getValue() instanceof String) {
                        empty = !StringUtils.hasText((String) pv.getValue());
                    } else if (pv.getValue() instanceof String[]) {
                        String[] values = (String[]) pv.getValue();
                        empty = (values.length == 0 || !StringUtils.hasText(values[0]));
                    }
                }
                if (empty) {
                    // 使用绑定错误处理器来创建 FieldError。
                    getBindingErrorProcessor().processMissingFieldError(field, getInternalBindingResult());
                    // Remove property from property values to bind：它已经导致字段错误并拒绝了值。
                    if (pv != null) {
                        mpvs.removePropertyValue(pv);
                        propertyValues.remove(field);
                    }
                }
            }
        }
    }

    /**
     * 将给定的属性值应用于目标对象。
     * <p>默认实现将所有提供的属性值应用为 bean 属性值。默认情况下，未知字段将被忽略。
     *
     * @param mpvs 要绑定的属性值（可以修改）
     * @see #getTarget
     * @see #getPropertyAccessor
     * @see #isIgnoreUnknownFields
     * @see #getBindingErrorProcessor
     * @see BindingErrorProcessor#processPropertyAccessException
     */
    protected void applyPropertyValues(MutablePropertyValues mpvs) {
        try {
            // 将请求参数绑定到目标对象上。
            getPropertyAccessor().setPropertyValues(mpvs, isIgnoreUnknownFields(), isIgnoreInvalidFields());
        } catch (PropertyBatchUpdateException ex) {
            // 使用绑定错误处理器来创建 FieldErrors。
            for (PropertyAccessException pae : ex.getPropertyAccessExceptions()) {
                getBindingErrorProcessor().processPropertyAccessException(pae, getInternalBindingResult());
            }
        }
    }

    /**
     * 调用指定的验证器（如果有）。
     *
     * @see #setValidator(Validator)
     * @see #getBindingResult()
     */
    public void validate() {
        Object target = getTarget();
        Assert.state(target != null, "No target to validate");
        BindingResult bindingResult = getBindingResult();
        // 使用相同的绑定结果调用每个验证器
        for (Validator validator : getValidators()) {
            validator.validate(target, bindingResult);
        }
    }

    /**
     * 使用给定的验证提示调用指定的验证器（如果有）。
     * <p>注意：验证提示可能会被实际目标验证器忽略。
     *
     * @param validationHints 要传递给 {@link SmartValidator} 的一个或多个提示对象
     * @see #setValidator(Validator)
     * @see SmartValidator#validate(Object, Errors, Object...)
     */
    public void validate(Object... validationHints) {
        Object target = getTarget();
        Assert.state(target != null, "No target to validate");
        BindingResult bindingResult = getBindingResult();
        // 使用相同的绑定结果调用每个验证器
        for (Validator validator : getValidators()) {
            if (!ObjectUtils.isEmpty(validationHints) && validator instanceof SmartValidator) {
                ((SmartValidator) validator).validate(target, bindingResult, validationHints);
            } else if (validator != null) {
                validator.validate(target, bindingResult);
            }
        }
    }

    /**
     * 关闭此 DataBinder，如果遇到任何错误，这可能会导致抛出 BindException。
     *
     * @return 模型映射，包含目标对象和错误实例
     * @throws BindException 如果绑定操作有任何错误
     * @see BindingResult#getModel()
     */
    public Map<?, ?> close() throws BindException {
        if (getBindingResult().hasErrors()) {
            throw new BindException(getBindingResult());
        }
        return getBindingResult().getModel();
    }
}
