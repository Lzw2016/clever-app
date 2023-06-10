package org.clever.web.support.bind;

import org.clever.beans.MutablePropertyValues;
import org.clever.beans.PropertyValue;
import org.clever.core.CollectionFactory;
import org.clever.validation.DataBinder;
import org.clever.web.http.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 用于从 Web 请求参数到 JavaBean 对象的数据绑定的特殊 {@link DataBinder}。
 * 专为 web 环境设计，但不依赖于 Servlet API；
 * 作为更具体的 DataBinder 变体的基类，例如 {@link org.clever.web.support.bind.ServletRequestDataBinder}。
 *
 * <p>包括对字段标记的支持，这些标记解决了 HTML 复选框和选择选项的常见问题：检测到字段是表单的一部分，但没有生成请求参数，因为它是空的。
 * 字段标记允许检测该状态并相应地重置相应的 bean 属性。对于不存在的参数，默认值可以为该字段指定一个非空值。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 22:18 <br/>
 *
 * @see #registerCustomEditor
 * @see #setAllowedFields
 * @see #setRequiredFields
 * @see #setFieldMarkerPrefix
 * @see #setFieldDefaultPrefix
 */
public class WebDataBinder extends DataBinder {
    /**
     * 字段标记参数开头的默认前缀，后跟字段名称：例如字段“subscribeToNewsletter”的“_subscribeToNewsletter”。
     * <p>这样的标记参数表示该字段是可见的，即存在于导致提交的表单中。
     * 如果没有找到相应的字段值参数，则该字段将被重置。在这种情况下，字段标记参数的值无关紧要；可以使用任意值。
     * 这对于 HTML 复选框和选择选项特别有用。
     *
     * @see #setFieldMarkerPrefix
     */
    public static final String DEFAULT_FIELD_MARKER_PREFIX = "_";
    /**
     * 字段默认参数开头的默认前缀，后跟字段名称：例如“!subscribeToNewsletter” 用于字段“subscribeToNewsletter”。
     * <p>默认参数与字段标记的不同之处在于它们提供默认值而不是空值。
     *
     * @see #setFieldDefaultPrefix
     */
    public static final String DEFAULT_FIELD_DEFAULT_PREFIX = "!";
    private String fieldMarkerPrefix = DEFAULT_FIELD_MARKER_PREFIX;
    private String fieldDefaultPrefix = DEFAULT_FIELD_DEFAULT_PREFIX;
    private boolean bindEmptyMultipartFiles = true;

    /**
     * @param target 要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @see #DEFAULT_OBJECT_NAME
     */
    public WebDataBinder(Object target) {
        super(target);
    }

    /**
     * @param target     要绑定到的目标对象（如果绑定器仅用于转换普通参数值，则为 {@code null}）
     * @param objectName 目标对象的名称
     */
    public WebDataBinder(Object target, String objectName) {
        super(target, objectName);
    }

    /**
     * 指定可用于标记可能为空字段的参数的前缀，名称为“前缀 + 字段”。
     * 这样的标记参数通过存在性来检查：您可以为它发送任何值，例如“可见”。这对于 HTML 复选框和选择选项特别有用。
     * <p>对于“_FIELD”参数（例如“_subscribeToNewsletter”），默认值为“_”。如果您想完全关闭空字段检查，请将此设置为 null。
     * <p>HTML 复选框仅在选中时发送一个值，因此不可能检测到以前选中的框刚刚被取消选中，至少不能使用标准的 HTML 方法。
     * <p>解决此问题的一种方法是，如果您知道复选框在表单中可见，则查找复选框参数值，如果未找到值，则重置复选框。这通常发生在自定义 {@code onBind} 实现中。
     * <p>这种自动重置机制解决了这一缺陷，前提是为每个复选框字段发送一个标记参数，例如“subscribeToNewsletter”字段的“_subscribeToNewsletter”。
     * 由于在任何情况下都会发送标记参数，因此数据绑定器可以检测到空字段并自动重置其值。
     *
     * @see #DEFAULT_FIELD_MARKER_PREFIX
     */
    public void setFieldMarkerPrefix(String fieldMarkerPrefix) {
        this.fieldMarkerPrefix = fieldMarkerPrefix;
    }

    /**
     * 返回标记可能为空字段的参数的前缀。
     */
    public String getFieldMarkerPrefix() {
        return this.fieldMarkerPrefix;
    }

    /**
     * 指定可用于指示默认值字段的参数的前缀，名称为“prefix + field”。未提供字段时使用默认字段的值。
     * <p>对于“!FIELD”参数（例如“!subscribeToNewsletter”），默认值为“！”。如果您想完全关闭字段默认值，请将此设置为 null。
     * <p>HTML 复选框仅在选中时发送一个值，因此不可能检测到以前选中的框刚刚被取消选中，至少不能使用标准的 HTML 方法。当复选框表示非布尔值时，默认字段特别有用。
     * <p>默认参数的存在优先于给定字段的字段标记的行为。
     *
     * @see #DEFAULT_FIELD_DEFAULT_PREFIX
     */
    public void setFieldDefaultPrefix(String fieldDefaultPrefix) {
        this.fieldDefaultPrefix = fieldDefaultPrefix;
    }

    /**
     * 返回标记默认字段的参数的前缀。
     */
    public String getFieldDefaultPrefix() {
        return this.fieldDefaultPrefix;
    }

    /**
     * 设置是否绑定空的 MultipartFile 参数。默认为“真”。
     * <p>如果您希望在用户重新提交表单而不选择其他文件时保留已绑定的 MultipartFile，请关闭此选项。
     * 否则，已经绑定的 MultipartFile 将被一个空的 MultipartFile 持有者替换。
     *
     * @see org.clever.web.http.multipart.MultipartFile
     */
    public void setBindEmptyMultipartFiles(boolean bindEmptyMultipartFiles) {
        this.bindEmptyMultipartFiles = bindEmptyMultipartFiles;
    }

    /**
     * 返回是否绑定空的 MultipartFile 参数。
     */
    public boolean isBindEmptyMultipartFiles() {
        return this.bindEmptyMultipartFiles;
    }

    /**
     * 此实现在委托给超类绑定过程之前执行字段默认值和标记检查。
     *
     * @see #checkFieldDefaults
     * @see #checkFieldMarkers
     */
    @Override
    protected void doBind(MutablePropertyValues mpvs) {
        checkFieldDefaults(mpvs);
        checkFieldMarkers(mpvs);
        adaptEmptyArrayIndices(mpvs);
        super.doBind(mpvs);
    }

    /**
     * 检查字段默认值的给定属性值，即以字段默认前缀开头的字段。
     * <p>字段默认值的存在表示如果该字段不存在则应使用指定的值。
     *
     * @param mpvs 要绑定的属性值（可以修改）
     * @see #getFieldDefaultPrefix
     */
    protected void checkFieldDefaults(MutablePropertyValues mpvs) {
        String fieldDefaultPrefix = getFieldDefaultPrefix();
        if (fieldDefaultPrefix != null) {
            PropertyValue[] pvArray = mpvs.getPropertyValues();
            for (PropertyValue pv : pvArray) {
                if (pv.getName().startsWith(fieldDefaultPrefix)) {
                    String field = pv.getName().substring(fieldDefaultPrefix.length());
                    if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
                        mpvs.add(field, pv.getValue());
                    }
                    mpvs.removePropertyValue(pv);
                }
            }
        }
    }

    /**
     * 检查字段标记的给定属性值，即以字段标记前缀开头的字段。
     * <p>字段标记的存在表明指定的字段存在于表单中。如果属性值不包含相应的字段值，则该字段将被视为空的，并将被适当地重置。
     *
     * @param mpvs 要绑定的属性值（可以修改）
     * @see #getFieldMarkerPrefix
     * @see #getEmptyValue(String, Class)
     */
    protected void checkFieldMarkers(MutablePropertyValues mpvs) {
        String fieldMarkerPrefix = getFieldMarkerPrefix();
        if (fieldMarkerPrefix != null) {
            PropertyValue[] pvArray = mpvs.getPropertyValues();
            for (PropertyValue pv : pvArray) {
                if (pv.getName().startsWith(fieldMarkerPrefix)) {
                    String field = pv.getName().substring(fieldMarkerPrefix.length());
                    if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
                        Class<?> fieldType = getPropertyAccessor().getPropertyType(field);
                        mpvs.add(field, getEmptyValue(field, fieldType));
                    }
                    mpvs.removePropertyValue(pv);
                }
            }
        }
    }

    /**
     * 检查名称以 {@code "[]"} 结尾的属性值。这被一些客户端用于没有显式索引值的数组语法。
     * 如果找到这样的值，请删除括号以适应为数据绑定目的表达相同值的预期方式。
     *
     * @param mpvs 要绑定的属性值（可以修改）
     */
    protected void adaptEmptyArrayIndices(MutablePropertyValues mpvs) {
        for (PropertyValue pv : mpvs.getPropertyValues()) {
            String name = pv.getName();
            if (name.endsWith("[]")) {
                String field = name.substring(0, name.length() - 2);
                if (getPropertyAccessor().isWritableProperty(field) && !mpvs.contains(field)) {
                    mpvs.add(field, pv.getValue());
                }
                mpvs.removePropertyValue(pv);
            }
        }
    }

    /**
     * 确定指定字段的空值。
     * <p>如果字段类型已知，则默认实现委托给 {@link #getEmptyValue(Class)}，否则回退到 {@code null}。
     *
     * @param field     字段名称
     * @param fieldType 字段的类型
     * @return 空值（对于大多数字段：{@code null}）
     */
    protected Object getEmptyValue(String field, Class<?> fieldType) {
        return (fieldType != null ? getEmptyValue(fieldType) : null);
    }

    /**
     * 确定指定字段的空值。
     * <p>默认实现返回：
     * <ul>
     * <li>{@code Boolean.FALSE} 用于布尔字段
     * <li>数组类型的空数组
     * <li>集合类型的集合实现
     * <li>地图类型的地图实现
     * <li>否则，{@code null} 用作默认值
     * </ul>
     *
     * @param fieldType 字段的类型
     * @return 空值（对于大多数字段：{@code null}）
     */
    public Object getEmptyValue(Class<?> fieldType) {
        try {
            if (boolean.class == fieldType || Boolean.class == fieldType) {
                // 布尔属性的特殊处理。
                return Boolean.FALSE;
            } else if (fieldType.isArray()) {
                // 数组属性的特殊处理。
                return Array.newInstance(fieldType.getComponentType(), 0);
            } else if (Collection.class.isAssignableFrom(fieldType)) {
                return CollectionFactory.createCollection(fieldType, 0);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                return CollectionFactory.createMap(fieldType, 0);
            }
        } catch (IllegalArgumentException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug("Failed to create default value - falling back to null: " + ex.getMessage());
            }
        }
        // 默认值：null。
        return null;
    }

    /**
     * 绑定给定请求中包含的所有多部分文件（如果有）（在多部分请求的情况下）。被子类调用。
     * <p>如果多部分文件不为空或者我们也配置为绑定空多部分文件，则多部分文件只会添加到属性值中。
     *
     * @param multipartFiles 字段名称 String 到 MultipartFile 对象的映射
     * @param mpvs           要绑定的属性值（可以修改）
     * @see org.clever.web.http.multipart.MultipartFile
     * @see #setBindEmptyMultipartFiles
     */
    protected void bindMultipart(Map<String, List<MultipartFile>> multipartFiles, MutablePropertyValues mpvs) {
        multipartFiles.forEach((key, values) -> {
            if (values.size() == 1) {
                MultipartFile value = values.get(0);
                if (isBindEmptyMultipartFiles() || !value.isEmpty()) {
                    mpvs.add(key, value);
                }
            } else {
                mpvs.add(key, values);
            }
        });
    }
}
