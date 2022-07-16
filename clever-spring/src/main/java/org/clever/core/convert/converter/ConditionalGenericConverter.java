package org.clever.core.convert.converter;

/**
 * 一种GenericConverter，可以根据源和目标TypeDescriptor的属性有条件地执行<br/>
 * 有关详细信息，请参阅{@link ConditionalConverter}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 16:14 <br/>
 */
public interface ConditionalGenericConverter extends GenericConverter, ConditionalConverter {
}
