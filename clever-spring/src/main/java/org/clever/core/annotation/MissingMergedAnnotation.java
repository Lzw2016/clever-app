package org.clever.core.annotation;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 表示缺失的(不存在的)合并注解，在{@link MergedAnnotation#missing()}中使用(单例类)
 *
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:28 <br/>
 */
final class MissingMergedAnnotation<A extends Annotation> extends AbstractMergedAnnotation<A> {
    private static final MissingMergedAnnotation<?> INSTANCE = new MissingMergedAnnotation<>();

    private MissingMergedAnnotation() {
    }

    @Override
    public Class<A> getType() {
        throw new NoSuchElementException("Unable to get type for missing annotation");
    }

    @Override
    public boolean isPresent() {
        return false;
    }

    @Override
    public Object getSource() {
        return null;
    }

    @Override
    public MergedAnnotation<?> getMetaSource() {
        return null;
    }

    @Override
    public MergedAnnotation<?> getRoot() {
        return this;
    }

    @Override
    public List<Class<? extends Annotation>> getMetaTypes() {
        return Collections.emptyList();
    }

    @Override
    public int getDistance() {
        return -1;
    }

    @Override
    public int getAggregateIndex() {
        return -1;
    }

    @Override
    public boolean hasNonDefaultValue(String attributeName) {
        throw new NoSuchElementException("Unable to check non-default value for missing annotation");
    }

    @Override
    public boolean hasDefaultValue(String attributeName) {
        throw new NoSuchElementException("Unable to check default value for missing annotation");
    }

    @Override
    public <T> Optional<T> getValue(String attributeName, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public <T> Optional<T> getDefaultValue(String attributeName, Class<T> type) {
        return Optional.empty();
    }

    @Override
    public MergedAnnotation<A> filterAttributes(Predicate<String> predicate) {
        return this;
    }

    @Override
    public MergedAnnotation<A> withNonMergedAttributes() {
        return this;
    }

    @Override
    public AnnotationAttributes asAnnotationAttributes(Adapt... adaptations) {
        return new AnnotationAttributes();
    }

    @Override
    public Map<String, Object> asMap(Adapt... adaptations) {
        return Collections.emptyMap();
    }

    @Override
    public <T extends Map<String, Object>> T asMap(Function<MergedAnnotation<?>, T> factory, Adapt... adaptations) {
        return factory.apply(this);
    }

    @Override
    public String toString() {
        return "(missing)";
    }

    @Override
    public <T extends Annotation> MergedAnnotation<T> getAnnotation(String attributeName, Class<T> type) throws NoSuchElementException {
        throw new NoSuchElementException("Unable to get attribute value for missing annotation");
    }

    @Override
    public <T extends Annotation> MergedAnnotation<T>[] getAnnotationArray(String attributeName, Class<T> type) throws NoSuchElementException {
        throw new NoSuchElementException("Unable to get attribute value for missing annotation");
    }

    @Override
    protected <T> T getAttributeValue(String attributeName, Class<T> type) {
        throw new NoSuchElementException("Unable to get attribute value for missing annotation");
    }

    @Override
    protected A createSynthesized() {
        throw new NoSuchElementException("Unable to synthesize missing annotation");
    }

    @SuppressWarnings("unchecked")
    static <A extends Annotation> MergedAnnotation<A> getInstance() {
        return (MergedAnnotation<A>) INSTANCE;
    }
}
