package org.clever.validation;

import org.clever.beans.PropertyEditorRegistry;

import java.beans.PropertyEditor;
import java.util.Map;

/**
 * 表示绑定结果的通用接口。扩展 {@link Errors interface} 以实现错误注册功能，允许应用 {@link Validator}，并添加特定于绑定的分析和模型构建。
 * <p>作为 {@link DataBinder} 的结果持有者，通过 {@link DataBinder#getBindingResult()} 方法获得。
 * BindingResult 实现也可以直接使用，例如在其上调用 {@link Validator}（例如作为单元测试的一部分）。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 21:33 <br/>
 *
 * @see DataBinder
 * @see Errors
 * @see Validator
 */
public interface BindingResult extends Errors {
    /**
     * 模型中 BindingResult 实例名称的前缀，后跟对象名称。
     */
    String MODEL_KEY_PREFIX = BindingResult.class.getName() + ".";

    /**
     * 返回被包装的目标对象，它可能是一个 bean，一个具有公共字段的对象，一个 Map——取决于具体的绑定策略。
     */
    Object getTarget();

    /**
     * 为获得的状态返回模型映射，将 BindingResult 实例公开为“{@link #MODEL_KEY_PREFIX MODEL_KEY_PREFIX} + objectName”，将对象本身公开为“objectName”。
     * <p>请注意，每次调用此方法时都会构建 Map。向地图添加东西然后重新调用此方法将不起作用。
     *
     * @see #getObjectName()
     * @see #MODEL_KEY_PREFIX
     */
    Map<String, Object> getModel();

    /**
     * 提取给定字段的原始字段值。通常用于比较目的。
     *
     * @param field 要检查的字段
     * @return 原始形式的字段的当前值，如果不知道则为 {@code null}
     */
    Object getRawFieldValue(String field);

    /**
     * 查找给定类型和属性的自定义属性编辑器。
     *
     * @param field     属性的路径（名称或嵌套路径），如果为给定类型的所有属性寻找编辑器，则为 {@code null}
     * @param valueType 属性的类型（如果给出了属性，则可以是 {@code null} 但在任何情况下都应指定以进行一致性检查）
     * @return 注册编辑器，如果没有则为 {@code null}
     */
    PropertyEditor findEditor(String field, Class<?> valueType);

    /**
     * 返回基础 PropertyEditorRegistry。
     *
     * @return PropertyEditorRegistry，或 {@code null} 如果没有可用于此 BindingResult
     */
    PropertyEditorRegistry getPropertyEditorRegistry();

    /**
     * 将给定的错误代码解析为消息代码。
     * <p>使用适当的参数调用配置的 {@link MessageCodesResolver}。
     *
     * @param errorCode 解析为消息代码的错误代码
     * @return 已解析的消息代码
     */
    String[] resolveMessageCodes(String errorCode);

    /**
     * 将给定的错误代码解析为给定字段的消息代码。
     * <p>使用适当的参数调用配置的 {@link MessageCodesResolver}。
     *
     * @param errorCode 解析为消息代码的错误代码
     * @param field     解析消息代码的字段
     * @return 已解析的消息代码
     */
    String[] resolveMessageCodes(String errorCode, String field);

    /**
     * 将自定义 {@link ObjectError} 或 {@link FieldError} 添加到错误列表。
     * <p>旨在供协作策略使用，例如 {@link BindingErrorProcessor}。
     *
     * @see ObjectError
     * @see FieldError
     * @see BindingErrorProcessor
     */
    void addError(ObjectError error);

    /**
     * 记录指定字段的给定值。
     * <p>在无法构造目标对象时使用，通过 {@link #getFieldValue} 提供原始字段值。如果出现注册错误，将针对每个受影响的字段公开拒绝的值。
     *
     * @param field 记录值的字段
     * @param type  字段的类型
     * @param value 原始值
     */
    default void recordFieldValue(String field, Class<?> type, Object value) {
    }

    /**
     * 将指定的不允许字段标记为禁止。
     * <p>数据绑定器为检测到的每个字段值调用此方法以针对不允许的字段。
     *
     * @see DataBinder#setAllowedFields
     */
    default void recordSuppressedField(String field) {
    }

    /**
     * 返回在绑定过程中被抑制的字段列表。
     * <p>可用于确定是否有任何字段值针对不允许的字段。
     *
     * @see DataBinder#setAllowedFields
     */
    default String[] getSuppressedFields() {
        return new String[0];
    }
}
