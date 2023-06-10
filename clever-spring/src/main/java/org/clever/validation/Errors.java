package org.clever.validation;

import org.clever.beans.PropertyAccessor;

import java.util.List;

/**
 * 存储和公开有关特定对象的数据绑定和验证错误的信息。
 * <p>字段名称可以是目标对象的属性（例如绑定到客户对象时的“名称”），或者子对象的嵌套字段（例如“address.street”）。
 * 通过 {@link #setNestedPath(String)} 支持子树导航：例如，{@code AddressValidator} 验证“地址”，但不知道这是客户的子对象。
 *
 * <p>注意: {@code Errors} 对象是单线程的
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:01 <br/>
 *
 * @see #setNestedPath
 * @see BindException
 * @see DataBinder
 * @see ValidationUtils
 */
public interface Errors {
    /**
     * 嵌套路径中路径元素之间的分隔符，例如“customer.name”或“customer.address.street”。
     */
    String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;

    /**
     * 返回绑定根对象的名称。
     */
    String getObjectName();

    /**
     * 允许更改上下文，以便标准验证器可以验证子树。拒绝呼叫将给定的路径添加到字符段名称之前。
     * <p>例如，地址验证器可以验证客户对象的子对象“地址”。
     *
     * @param nestedPath 此对象中的嵌套路径，例如“地址”（默认为“”，{@code null} 也是可以接受的）。可以以点结尾：既是“地址”又是“地址”。有效。
     */
    void setNestedPath(String nestedPath);

    /**
     * 返回此 {@link Errors} 对象的当前嵌套路径。
     * <p>返回带点的嵌套路径，即“地址”，以便轻松构建串联路径。默认为空字符串。
     */
    String getNestedPath();

    /**
     * 将给定的子路径推入嵌套路径堆栈。
     * <p>{@link #popNestedPath()} 调用将在相应的 {@code pushNestedPath(String)} 调用之前重置原始嵌套路径。
     * <p>使用嵌套路径堆栈允许为子对象设置临时嵌套路径，而不必担心临时路径持有者。
     * <p>例如：当前路径“spouse.”, pushNestedPath(“child”) → 结果路径“spouse.child.”； popNestedPath() → “配偶”。再次。
     *
     * @param subPath 推入嵌套路径堆栈的子路径
     * @see #popNestedPath
     */
    void pushNestedPath(String subPath);

    /**
     * 从嵌套路径堆栈中弹出前一个嵌套路径
     *
     * @throws IllegalStateException 如果堆栈上没有以前的嵌套路径
     * @see #pushNestedPath
     */
    void popNestedPath() throws IllegalStateException;

    /**
     * 使用给定的错误描述为整个目标对象注册一个全局错误。
     *
     * @param errorCode 错误代码，可解释为消息键
     */
    void reject(String errorCode);

    /**
     * 使用给定的错误描述为整个目标对象注册一个全局错误。
     *
     * @param errorCode      错误代码，可解释为消息键
     * @param defaultMessage 后备默认消息
     */
    void reject(String errorCode, String defaultMessage);

    /**
     * 使用给定的错误描述为整个目标对象注册一个全局错误。
     *
     * @param errorCode      错误代码，可解释为消息键
     * @param errorArgs      错误参数，用于通过 MessageFormat 进行参数绑定（可以是 {@code null}）
     * @param defaultMessage 后备默认消息
     */
    void reject(String errorCode, Object[] errorArgs, String defaultMessage);

    /**
     * 使用给定的错误描述为当前对象的指定字段注册一个字段错误（关于当前嵌套路径，如果有的话）。
     * <p>字段名称可以是 {@code null} 或空字符串来指示当前对象本身而不是它的一个字段。如果当前对象是顶级对象，这可能会导致嵌套对象图中的相应字段错误或全局错误。
     *
     * @param field     字段名称 （可能是 {@code null} 或空字符串）
     * @param errorCode 错误代码，可解释为消息键
     * @see #getNestedPath()
     */
    void rejectValue(String field, String errorCode);

    /**
     * 使用给定的错误描述为当前对象的指定字段注册一个字段错误（关于当前嵌套路径，如果有的话）。
     * <p>字段名称可以是 {@code null} 或空字符串来指示当前对象本身而不是它的一个字段。如果当前对象是顶级对象，这可能会导致嵌套对象图中的相应字段错误或全局错误。
     *
     * @param field          字段名称 （可能是 {@code null} 或空字符串）
     * @param errorCode      错误代码，可解释为消息键
     * @param defaultMessage 后备默认消息
     * @see #getNestedPath()
     */
    void rejectValue(String field, String errorCode, String defaultMessage);

    /**
     * 使用给定的错误描述为当前对象的指定字段注册一个字段错误（关于当前嵌套路径，如果有的话）。
     * <p>字段名称可以是 {@code null} 或空字符串来指示当前对象本身而不是它的一个字段。如果当前对象是顶级对象，这可能会导致嵌套对象图中的相应字段错误或全局错误。
     *
     * @param field          字段名称 （可能是 {@code null} 或空字符串）
     * @param errorCode      错误代码，可解释为消息键
     * @param errorArgs      错误参数，用于通过 MessageFormat 进行参数绑定（可以是 {@code null}）
     * @param defaultMessage 后备默认消息
     * @see #getNestedPath()
     */
    void rejectValue(String field, String errorCode, Object[] errorArgs, String defaultMessage);

    /**
     * 将给定 {@code Errors} 实例中的所有错误添加到此 {@code Errors} 实例。
     * <p>这是一种避免重复 {@code reject(..)} 调用以将 {@code Errors} 实例合并到另一个 {@code Errors} 实例的便捷方法。
     * <p>请注意，传入的 {@code Errors} 实例应该引用相同的目标对象，或者至少包含适用于此 {@code Errors} 实例的目标对象的兼容错误。
     *
     * @param errors 要合并的 {@code Errors} 实例
     */
    void addAllErrors(Errors errors);

    /**
     * 如果有任何错误，请返回
     */
    boolean hasErrors();

    /**
     * 返回错误总数
     */
    int getErrorCount();

    /**
     * 获取所有错误，包括全局错误和现场错误
     *
     * @return {@link ObjectError} 实例列表
     */
    List<ObjectError> getAllErrors();

    /**
     * 有没有全局错误
     *
     * @return {@code true} 如果有任何全局错误
     * @see #hasFieldErrors()
     */
    boolean hasGlobalErrors();

    /**
     * 返回全局错误的数量。
     *
     * @return 全局错误的数量
     * @see #getFieldErrorCount()
     */
    int getGlobalErrorCount();

    /**
     * 获取所有全局错误。
     *
     * @return {@link ObjectError} 实例列表
     */
    List<ObjectError> getGlobalErrors();

    /**
     * 获取<i>第一个</i> 全局错误，如果有的话。
     *
     * @return 全局错误，或 {@code null}
     */
    ObjectError getGlobalError();

    /**
     * 有没有字段错误
     *
     * @return {@code true} 如果有任何与字段相关的错误
     * @see #hasGlobalErrors()
     */
    boolean hasFieldErrors();

    /**
     * 返回与字段关联的错误数。
     *
     * @return 与字段关联的错误数
     * @see #getGlobalErrorCount()
     */
    int getFieldErrorCount();

    /**
     * 获取与字段关联的所有错误。
     *
     * @return {@link FieldError} 实例列表
     */
    List<FieldError> getFieldErrors();

    /**
     * 获取与字段关联的<i>第一个</i> 错误（如果有）。
     *
     * @return 特定于字段的错误，或 {@code null}
     */
    FieldError getFieldError();

    /**
     * 是否存在与给定字段相关的任何错误
     *
     * @param field 字段名称
     * @return {@code true} 如果给定字段有任何错误
     */
    boolean hasFieldErrors(String field);

    /**
     * 返回与给定字段关联的错误数。
     *
     * @param field 字段名称
     * @return 与给定字段关联的错误数
     */
    int getFieldErrorCount(String field);

    /**
     * 获取与给定字段关联的所有错误。
     * <p>实现不仅应支持完整的字段名称，如“name”，还应支持模式匹配，如“na*”或“address.*”。
     *
     * @param field 字段名称
     * @return {@link FieldError} 实例列表
     */
    List<FieldError> getFieldErrors(String field);

    /**
     * 获取与给定字段关联的第一个错误（如果有）。
     *
     * @param field 字段名称
     * @return 特定于字段的错误，或 {@code null}
     */
    FieldError getFieldError(String field);

    /**
     * 返回给定字段的当前值，当前 bean 属性值或上次绑定中拒绝的更新。
     * <p>允许方便地访问用户指定的字段值，即使存在类型不匹配。
     *
     * @param field 字段名称
     * @return 给定字段的当前值
     */
    Object getFieldValue(String field);

    /**
     * 返回给定字段的类型。
     * <p>即使字段值为 {@code null}，实现也应该能够确定类型，例如来自一些关联的描述符。
     *
     * @param field 字段名称
     * @return 字段的类型，如果无法确定，则为 {@code null}
     */
    Class<?> getFieldType(String field);
}
