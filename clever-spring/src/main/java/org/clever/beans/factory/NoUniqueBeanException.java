package org.clever.beans.factory;

import org.clever.core.ResolvableType;
import org.clever.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * 参考 {@code NoUniqueBeanDefinitionException}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 10:49 <br/>
 */
public class NoUniqueBeanException extends NoSuchBeanException {
    private final int numberOfBeansFound;
    private final Collection<String> beanNamesFound;

    /**
     * @param type               所需的非唯一bean类型
     * @param numberOfBeansFound 匹配bean的数量
     * @param message            描述问题的详细消息
     */
    public NoUniqueBeanException(Class<?> type, int numberOfBeansFound, String message) {
        super(type, message);
        this.numberOfBeansFound = numberOfBeansFound;
        this.beanNamesFound = null;
    }

    /**
     * @param type           所需的非唯一bean类型
     * @param beanNamesFound 所有匹配bean的名称（作为集合）
     */
    public NoUniqueBeanException(Class<?> type, Collection<String> beanNamesFound) {
        super(
                type,
                "expected single matching bean but found "
                        + beanNamesFound.size() + ": "
                        + StringUtils.collectionToCommaDelimitedString(beanNamesFound)
        );
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    /**
     * @param type           所需的非唯一bean类型
     * @param beanNamesFound 所有匹配bean的名称（作为数组）
     */
    public NoUniqueBeanException(Class<?> type, String... beanNamesFound) {
        this(type, Arrays.asList(beanNamesFound));
    }

    /**
     * @param type           所需的非唯一bean类型
     * @param beanNamesFound 所有匹配bean的名称（作为集合）
     */
    public NoUniqueBeanException(ResolvableType type, Collection<String> beanNamesFound) {
        super(
                type,
                "expected single matching bean but found "
                        + beanNamesFound.size() + ": " +
                        StringUtils.collectionToCommaDelimitedString(beanNamesFound)
        );
        this.numberOfBeansFound = beanNamesFound.size();
        this.beanNamesFound = beanNamesFound;
    }

    /**
     * @param type           所需的非唯一bean类型
     * @param beanNamesFound 所有匹配bean的名称（作为数组）
     */
    public NoUniqueBeanException(ResolvableType type, String... beanNamesFound) {
        this(type, Arrays.asList(beanNamesFound));
    }

    /**
     * 返回当只需要一个匹配bean时找到的bean数。
     * 对于NoUniqueBeanException，该值通常大于1。
     *
     * @see #getBeanType()
     */
    @Override
    public int getNumberOfBeansFound() {
        return this.numberOfBeansFound;
    }

    /**
     * 返回当只需要一个匹配bean时找到的所有bean的名称。
     * 请注意，如果在施工时未指定，则该值可能为空。
     *
     * @see #getBeanType()
     */
    public Collection<String> getBeanNamesFound() {
        return this.beanNamesFound;
    }
}
