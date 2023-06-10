package org.clever.validation;

import org.clever.beans.PropertyAccessException;

/**
 * 处理 {@code DataBinder} 的缺失字段错误以及将 {@code PropertyAccessException} 转换为 {@code FieldError} 的策略。
 * <p>错误处理器是可插入的，因此您可以根据需要以不同方式处理错误。为典型需求提供默认实现。
 *
 * <p>注意：此接口对给定的 BindingResult 进行操作，以与任何绑定策略（bean 属性、直接字段访问等）兼容。
 * 它仍然可以接收 BindException 作为参数（因为 BindException 也实现了 BindingResult 接口）但不再直接对其进行操作。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/07 22:14 <br/>
 *
 * @see DataBinder#setBindingErrorProcessor
 * @see DefaultBindingErrorProcessor
 * @see BindingResult
 * @see BindException
 */
public interface BindingErrorProcessor {
    /**
     * 将缺少的字段错误应用于给定的 BindException。
     * <p>通常，会为缺少必填字段创建字段错误。
     *
     * @param missingField  绑定期间丢失的字段
     * @param bindingResult 要添加错误的错误对象。您可以添加不止一个错误，甚至可以忽略它。{@code BindingResult} 对象具有便利的实用程序，例如用于解析错误代码的 {@code resolveMessageCodes} 方法。
     * @see BeanPropertyBindingResult#addError
     * @see BeanPropertyBindingResult#resolveMessageCodes
     */
    void processMissingFieldError(String missingField, BindingResult bindingResult);

    /**
     * 将给定的 {@code PropertyAccessException} 转换为在给定的 {@code Errors} 实例上注册的适当错误。
     * <p>请注意，有两种错误类型可用：{@code FieldError} 和 {@code ObjectError}。通常，会创建字段错误，但在某些情况下，您可能希望创建一个全局的 {@code ObjectError}。
     *
     * @param ex            要翻译的 {@code PropertyAccessException}
     * @param bindingResult 要添加错误的错误对象。您可以添加不止一个错误，甚至可以忽略它。 {@code BindingResult} 对象具有便利的实用程序，例如用于解析错误代码的 {@code resolveMessageCodes} 方法。
     * @see Errors
     * @see FieldError
     * @see ObjectError
     * @see MessageCodesResolver
     * @see BeanPropertyBindingResult#addError
     * @see BeanPropertyBindingResult#resolveMessageCodes
     */
    void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult);
}
