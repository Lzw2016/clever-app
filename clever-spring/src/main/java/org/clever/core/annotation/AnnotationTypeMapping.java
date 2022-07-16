package org.clever.core.annotation;

import org.clever.core.annotation.AnnotationTypeMapping.MirrorSets.MirrorSet;
import org.clever.util.ObjectUtils;
import org.clever.util.ReflectionUtils;
import org.clever.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * 提供root注解类型上下文中单个注解(或元注解)的映射信息
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 13:35 <br/>
 */
final class AnnotationTypeMapping {
    private static final MirrorSet[] EMPTY_MIRROR_SETS = new MirrorSet[0];

    private final AnnotationTypeMapping source;
    private final AnnotationTypeMapping root;
    private final int distance;
    private final Class<? extends Annotation> annotationType;
    private final List<Class<? extends Annotation>> metaTypes;
    private final Annotation annotation;
    private final AttributeMethods attributes;
    private final MirrorSets mirrorSets;
    private final int[] aliasMappings;
    private final int[] conventionMappings;
    private final int[] annotationValueMappings;
    private final AnnotationTypeMapping[] annotationValueSource;
    private final Map<Method, List<Method>> aliasedBy;
    private final boolean synthesizable;
    private final Set<Method> claimedAliases = new HashSet<>();

    AnnotationTypeMapping(AnnotationTypeMapping source,
                          Class<? extends Annotation> annotationType,
                          Annotation annotation,
                          Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        this.source = source;
        this.root = (source != null ? source.getRoot() : this);
        this.distance = (source == null ? 0 : source.getDistance() + 1);
        this.annotationType = annotationType;
        this.metaTypes = merge(source != null ? source.getMetaTypes() : null, annotationType);
        this.annotation = annotation;
        this.attributes = AttributeMethods.forAnnotationType(annotationType);
        this.mirrorSets = new MirrorSets();
        this.aliasMappings = filledIntArray(this.attributes.size());
        this.conventionMappings = filledIntArray(this.attributes.size());
        this.annotationValueMappings = filledIntArray(this.attributes.size());
        this.annotationValueSource = new AnnotationTypeMapping[this.attributes.size()];
        this.aliasedBy = resolveAliasedForTargets();
        processAliases();
        addConventionMappings();
        addConventionAnnotationValues();
        this.synthesizable = computeSynthesizableFlag(visitedAnnotationTypes);
    }

    private static <T> List<T> merge(List<T> existing, T element) {
        if (existing == null) {
            return Collections.singletonList(element);
        }
        List<T> merged = new ArrayList<>(existing.size() + 1);
        merged.addAll(existing);
        merged.add(element);
        return Collections.unmodifiableList(merged);
    }

    private Map<Method, List<Method>> resolveAliasedForTargets() {
        Map<Method, List<Method>> aliasedBy = new HashMap<>();
        for (int i = 0; i < this.attributes.size(); i++) {
            Method attribute = this.attributes.get(i);
            AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
            if (aliasFor != null) {
                Method target = resolveAliasTarget(attribute, aliasFor);
                aliasedBy.computeIfAbsent(target, key -> new ArrayList<>()).add(attribute);
            }
        }
        return Collections.unmodifiableMap(aliasedBy);
    }

    private Method resolveAliasTarget(Method attribute, AliasFor aliasFor) {
        return resolveAliasTarget(attribute, aliasFor, true);
    }

    private Method resolveAliasTarget(Method attribute, AliasFor aliasFor, boolean checkAliasPair) {
        if (StringUtils.hasText(aliasFor.value()) && StringUtils.hasText(aliasFor.attribute())) {
            throw new AnnotationConfigurationException(String.format(
                    "In @AliasFor declared on %s, attribute 'attribute' and its alias 'value' " +
                            "are present with values of '%s' and '%s', but only one is permitted.",
                    AttributeMethods.describe(attribute),
                    aliasFor.attribute(),
                    aliasFor.value())
            );
        }
        Class<? extends Annotation> targetAnnotation = aliasFor.annotation();
        if (targetAnnotation == Annotation.class) {
            targetAnnotation = this.annotationType;
        }
        String targetAttributeName = aliasFor.attribute();
        if (!StringUtils.hasLength(targetAttributeName)) {
            targetAttributeName = aliasFor.value();
        }
        if (!StringUtils.hasLength(targetAttributeName)) {
            targetAttributeName = attribute.getName();
        }
        Method target = AttributeMethods.forAnnotationType(targetAnnotation).get(targetAttributeName);
        if (target == null) {
            if (targetAnnotation == this.annotationType) {
                throw new AnnotationConfigurationException(String.format(
                        "@AliasFor declaration on %s declares an alias for '%s' which is not present.",
                        AttributeMethods.describe(attribute),
                        targetAttributeName
                ));
            }
            throw new AnnotationConfigurationException(String.format(
                    "%s is declared as an @AliasFor nonexistent %s.",
                    StringUtils.capitalize(AttributeMethods.describe(attribute)),
                    AttributeMethods.describe(targetAnnotation, targetAttributeName)
            ));
        }
        if (target.equals(attribute)) {
            throw new AnnotationConfigurationException(String.format(
                    "@AliasFor declaration on %s points to itself. " +
                            "Specify 'annotation' to point to a same-named attribute on a meta-annotation.",
                    AttributeMethods.describe(attribute)
            ));
        }
        if (!isCompatibleReturnType(attribute.getReturnType(), target.getReturnType())) {
            throw new AnnotationConfigurationException(String.format(
                    "Misconfigured aliases: %s and %s must declare the same return type.",
                    AttributeMethods.describe(attribute),
                    AttributeMethods.describe(target)
            ));
        }
        if (isAliasPair(target) && checkAliasPair) {
            AliasFor targetAliasFor = target.getAnnotation(AliasFor.class);
            if (targetAliasFor != null) {
                Method mirror = resolveAliasTarget(target, targetAliasFor, false);
                if (!mirror.equals(attribute)) {
                    throw new AnnotationConfigurationException(String.format(
                            "%s must be declared as an @AliasFor %s, not %s.",
                            StringUtils.capitalize(AttributeMethods.describe(target)),
                            AttributeMethods.describe(attribute),
                            AttributeMethods.describe(mirror)
                    ));
                }
            }
        }
        return target;
    }

    private boolean isAliasPair(Method target) {
        return (this.annotationType == target.getDeclaringClass());
    }

    private boolean isCompatibleReturnType(Class<?> attributeType, Class<?> targetType) {
        return (attributeType == targetType || attributeType == targetType.getComponentType());
    }

    private void processAliases() {
        List<Method> aliases = new ArrayList<>();
        for (int i = 0; i < this.attributes.size(); i++) {
            aliases.clear();
            aliases.add(this.attributes.get(i));
            collectAliases(aliases);
            if (aliases.size() > 1) {
                processAliases(i, aliases);
            }
        }
    }

    private void collectAliases(List<Method> aliases) {
        AnnotationTypeMapping mapping = this;
        while (mapping != null) {
            int size = aliases.size();
            for (int j = 0; j < size; j++) {
                List<Method> additional = mapping.aliasedBy.get(aliases.get(j));
                if (additional != null) {
                    aliases.addAll(additional);
                }
            }
            mapping = mapping.source;
        }
    }

    private void processAliases(int attributeIndex, List<Method> aliases) {
        int rootAttributeIndex = getFirstRootAttributeIndex(aliases);
        AnnotationTypeMapping mapping = this;
        while (mapping != null) {
            if (rootAttributeIndex != -1 && mapping != this.root) {
                for (int i = 0; i < mapping.attributes.size(); i++) {
                    if (aliases.contains(mapping.attributes.get(i))) {
                        mapping.aliasMappings[i] = rootAttributeIndex;
                    }
                }
            }
            mapping.mirrorSets.updateFrom(aliases);
            mapping.claimedAliases.addAll(aliases);
            if (mapping.annotation != null) {
                int[] resolvedMirrors = mapping.mirrorSets.resolve(null, mapping.annotation, ReflectionUtils::invokeMethod);
                for (int i = 0; i < mapping.attributes.size(); i++) {
                    if (aliases.contains(mapping.attributes.get(i))) {
                        this.annotationValueMappings[attributeIndex] = resolvedMirrors[i];
                        this.annotationValueSource[attributeIndex] = mapping;
                    }
                }
            }
            mapping = mapping.source;
        }
    }

    private int getFirstRootAttributeIndex(Collection<Method> aliases) {
        AttributeMethods rootAttributes = this.root.getAttributes();
        for (int i = 0; i < rootAttributes.size(); i++) {
            if (aliases.contains(rootAttributes.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private void addConventionMappings() {
        if (this.distance == 0) {
            return;
        }
        AttributeMethods rootAttributes = this.root.getAttributes();
        int[] mappings = this.conventionMappings;
        for (int i = 0; i < mappings.length; i++) {
            String name = this.attributes.get(i).getName();
            MirrorSet mirrors = getMirrorSets().getAssigned(i);
            int mapped = rootAttributes.indexOf(name);
            if (!MergedAnnotation.VALUE.equals(name) && mapped != -1) {
                mappings[i] = mapped;
                if (mirrors != null) {
                    for (int j = 0; j < mirrors.size(); j++) {
                        mappings[mirrors.getAttributeIndex(j)] = mapped;
                    }
                }
            }
        }
    }

    private void addConventionAnnotationValues() {
        for (int i = 0; i < this.attributes.size(); i++) {
            Method attribute = this.attributes.get(i);
            boolean isValueAttribute = MergedAnnotation.VALUE.equals(attribute.getName());
            AnnotationTypeMapping mapping = this;
            while (mapping != null && mapping.distance > 0) {
                int mapped = mapping.getAttributes().indexOf(attribute.getName());
                if (mapped != -1 && isBetterConventionAnnotationValue(i, isValueAttribute, mapping)) {
                    this.annotationValueMappings[i] = mapped;
                    this.annotationValueSource[i] = mapping;
                }
                mapping = mapping.source;
            }
        }
    }

    private boolean isBetterConventionAnnotationValue(int index, boolean isValueAttribute, AnnotationTypeMapping mapping) {
        if (this.annotationValueMappings[index] == -1) {
            return true;
        }
        int existingDistance = this.annotationValueSource[index].distance;
        return !isValueAttribute && existingDistance > mapping.distance;
    }

    @SuppressWarnings("unchecked")
    private boolean computeSynthesizableFlag(Set<Class<? extends Annotation>> visitedAnnotationTypes) {
        // Track that we have visited the current annotation type.
        visitedAnnotationTypes.add(this.annotationType);
        // Uses @AliasFor for local aliases?
        for (int index : this.aliasMappings) {
            if (index != -1) {
                return true;
            }
        }
        // Uses @AliasFor for attribute overrides in meta-annotations?
        if (!this.aliasedBy.isEmpty()) {
            return true;
        }
        // Uses convention-based attribute overrides in meta-annotations?
        for (int index : this.conventionMappings) {
            if (index != -1) {
                return true;
            }
        }
        // Has nested annotations or arrays of annotations that are synthesizable?
        if (getAttributes().hasNestedAnnotation()) {
            AttributeMethods attributeMethods = getAttributes();
            for (int i = 0; i < attributeMethods.size(); i++) {
                Method method = attributeMethods.get(i);
                Class<?> type = method.getReturnType();
                if (type.isAnnotation() || (type.isArray() && type.getComponentType().isAnnotation())) {
                    Class<? extends Annotation> annotationType = (Class<? extends Annotation>) (type.isAnnotation() ? type : type.getComponentType());
                    // Ensure we have not yet visited the current nested annotation type, in order
                    // to avoid infinite recursion for JVM languages other than Java that support
                    // recursive annotation definitions.
                    if (visitedAnnotationTypes.add(annotationType)) {
                        AnnotationTypeMapping mapping = AnnotationTypeMappings.forAnnotationType(annotationType, visitedAnnotationTypes).get(0);
                        if (mapping.isSynthesizable()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Method called after all mappings have been set. At this point no further
     * lookups from child mappings will occur.
     */
    void afterAllMappingsSet() {
        validateAllAliasesClaimed();
        for (int i = 0; i < this.mirrorSets.size(); i++) {
            validateMirrorSet(this.mirrorSets.get(i));
        }
        this.claimedAliases.clear();
    }

    private void validateAllAliasesClaimed() {
        for (int i = 0; i < this.attributes.size(); i++) {
            Method attribute = this.attributes.get(i);
            AliasFor aliasFor = AnnotationsScanner.getDeclaredAnnotation(attribute, AliasFor.class);
            if (aliasFor != null && !this.claimedAliases.contains(attribute)) {
                Method target = resolveAliasTarget(attribute, aliasFor);
                throw new AnnotationConfigurationException(String.format(
                        "@AliasFor declaration on %s declares an alias for %s which is not meta-present.",
                        AttributeMethods.describe(attribute),
                        AttributeMethods.describe(target)
                ));
            }
        }
    }

    private void validateMirrorSet(MirrorSet mirrorSet) {
        Method firstAttribute = mirrorSet.get(0);
        Object firstDefaultValue = firstAttribute.getDefaultValue();
        for (int i = 1; i <= mirrorSet.size() - 1; i++) {
            Method mirrorAttribute = mirrorSet.get(i);
            Object mirrorDefaultValue = mirrorAttribute.getDefaultValue();
            if (firstDefaultValue == null || mirrorDefaultValue == null) {
                throw new AnnotationConfigurationException(String.format(
                        "Misconfigured aliases: %s and %s must declare default values.",
                        AttributeMethods.describe(firstAttribute),
                        AttributeMethods.describe(mirrorAttribute)
                ));
            }
            if (!ObjectUtils.nullSafeEquals(firstDefaultValue, mirrorDefaultValue)) {
                throw new AnnotationConfigurationException(String.format(
                        "Misconfigured aliases: %s and %s must declare the same default value.",
                        AttributeMethods.describe(firstAttribute),
                        AttributeMethods.describe(mirrorAttribute)
                ));
            }
        }
    }

    /**
     * 返回 root mapping
     */
    AnnotationTypeMapping getRoot() {
        return this.root;
    }

    /**
     * 返回mapping的源对象
     */
    AnnotationTypeMapping getSource() {
        return this.source;
    }

    /**
     * 返回mapping的距离(对应注解的距离)
     */
    int getDistance() {
        return this.distance;
    }

    /**
     * 返回被映射的注解类型
     */
    Class<? extends Annotation> getAnnotationType() {
        return this.annotationType;
    }

    /**
     * 元类型
     */
    List<Class<? extends Annotation>> getMetaTypes() {
        return this.metaTypes;
    }

    /**
     * 获取此映射的源注解。如果这是root mapping，就返回元注解或null
     */
    Annotation getAnnotation() {
        return this.annotation;
    }

    /**
     * 获取mapping注解类型的注解属性
     */
    AttributeMethods getAttributes() {
        return this.attributes;
    }

    /**
     * 获取别名mapping属性的相关索引，如果没有mapping，则获取-1 <br/>
     * 结果值是root注解上属性的索引，可以调用该索引以获得实际值
     *
     * @param attributeIndex 源属性的属性索引
     * @return mapping的属性索引或-1
     */
    int getAliasMapping(int attributeIndex) {
        return this.aliasMappings[attributeIndex];
    }

    /**
     * 获取约定mapping属性的相关索引，如果没有mapping，返回-1 <br/>
     * 结果值是根注解上属性的索引，可以调用该索引以获得实际值
     *
     * @param attributeIndex 源属性的属性索引
     * @return mapped的属性索引或-1
     */
    int getConventionMapping(int attributeIndex) {
        return this.conventionMappings[attributeIndex];
    }

    /**
     * 从最合适的元注解中获取mapping属性值 <br/>
     * 结果值从最近的元注解中获得，同时考虑了约定和基于别名的mapping规则 <br/>
     * 对于root mapping，此方法将始终返回null
     *
     * @param attributeIndex      源属性的属性索引
     * @param metaAnnotationsOnly 如果此参数为false，则注解中的别名也将被考虑
     */
    Object getMappedAnnotationValue(int attributeIndex, boolean metaAnnotationsOnly) {
        int mappedIndex = this.annotationValueMappings[attributeIndex];
        if (mappedIndex == -1) {
            return null;
        }
        AnnotationTypeMapping source = this.annotationValueSource[attributeIndex];
        if (source == this && metaAnnotationsOnly) {
            return null;
        }
        return ReflectionUtils.invokeMethod(source.attributes.get(mappedIndex), source.annotation);
    }

    /**
     * 确定指定的值是否等于给定索引处属性的默认值
     *
     * @param attributeIndex 源属性的属性索引
     * @param value          要检查的值
     * @param valueExtractor 用于从任何嵌套注解中提取值的值提取器
     * @return 如果该值等于默认值，则为true
     */
    boolean isEquivalentToDefaultValue(int attributeIndex, Object value, ValueExtractor valueExtractor) {
        Method attribute = this.attributes.get(attributeIndex);
        return isEquivalentToDefaultValue(attribute, value, valueExtractor);
    }

    /**
     * 获取此类型映射的镜像集合
     */
    MirrorSets getMirrorSets() {
        return this.mirrorSets;
    }

    /**
     * 确定映射的注解是否可合成
     */
    boolean isSynthesizable() {
        return this.synthesizable;
    }
    
    private static int[] filledIntArray(int size) {
        int[] array = new int[size];
        Arrays.fill(array, -1);
        return array;
    }

    private static boolean isEquivalentToDefaultValue(Method attribute, Object value, ValueExtractor valueExtractor) {
        return areEquivalent(attribute.getDefaultValue(), value, valueExtractor);
    }

    private static boolean areEquivalent(Object value, Object extractedValue, ValueExtractor valueExtractor) {
        if (ObjectUtils.nullSafeEquals(value, extractedValue)) {
            return true;
        }
        if (value instanceof Class && extractedValue instanceof String) {
            return areEquivalent((Class<?>) value, (String) extractedValue);
        }
        if (value instanceof Class[] && extractedValue instanceof String[]) {
            return areEquivalent((Class<?>[]) value, (String[]) extractedValue);
        }
        if (value instanceof Annotation) {
            return areEquivalent((Annotation) value, extractedValue, valueExtractor);
        }
        return false;
    }

    private static boolean areEquivalent(Class<?>[] value, String[] extractedValue) {
        if (value.length != extractedValue.length) {
            return false;
        }
        for (int i = 0; i < value.length; i++) {
            if (!areEquivalent(value[i], extractedValue[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean areEquivalent(Class<?> value, String extractedValue) {
        return value.getName().equals(extractedValue);
    }

    private static boolean areEquivalent(Annotation annotation, Object extractedValue, ValueExtractor valueExtractor) {
        AttributeMethods attributes = AttributeMethods.forAnnotationType(annotation.annotationType());
        for (int i = 0; i < attributes.size(); i++) {
            Method attribute = attributes.get(i);
            Object value1 = ReflectionUtils.invokeMethod(attribute, annotation);
            Object value2;
            if (extractedValue instanceof TypeMappedAnnotation) {
                value2 = ((TypeMappedAnnotation<?>) extractedValue).getValue(attribute.getName()).orElse(null);
            } else {
                value2 = valueExtractor.extract(attribute, extractedValue);
            }
            if (!areEquivalent(value1, value2, valueExtractor)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 注解类型映射的集合。提供所有已定义{@link MirrorSet}的详细信息
     */
    class MirrorSets {
        private MirrorSet[] mirrorSets;
        private final MirrorSet[] assigned;

        MirrorSets() {
            this.assigned = new MirrorSet[attributes.size()];
            this.mirrorSets = EMPTY_MIRROR_SETS;
        }

        void updateFrom(Collection<Method> aliases) {
            MirrorSet mirrorSet = null;
            int size = 0;
            int last = -1;
            for (int i = 0; i < attributes.size(); i++) {
                Method attribute = attributes.get(i);
                if (aliases.contains(attribute)) {
                    size++;
                    if (size > 1) {
                        if (mirrorSet == null) {
                            mirrorSet = new MirrorSet();
                            this.assigned[last] = mirrorSet;
                        }
                        this.assigned[i] = mirrorSet;
                    }
                    last = i;
                }
            }
            if (mirrorSet != null) {
                mirrorSet.update();
                Set<MirrorSet> unique = new LinkedHashSet<>(Arrays.asList(this.assigned));
                unique.remove(null);
                this.mirrorSets = unique.toArray(EMPTY_MIRROR_SETS);
            }
        }

        int size() {
            return this.mirrorSets.length;
        }

        MirrorSet get(int index) {
            return this.mirrorSets[index];
        }

        MirrorSet getAssigned(int attributeIndex) {
            return this.assigned[attributeIndex];
        }

        int[] resolve(Object source, Object annotation, ValueExtractor valueExtractor) {
            int[] result = new int[attributes.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = i;
            }
            for (int i = 0; i < size(); i++) {
                MirrorSet mirrorSet = get(i);
                int resolved = mirrorSet.resolve(source, annotation, valueExtractor);
                for (int j = 0; j < mirrorSet.size; j++) {
                    result[mirrorSet.indexes[j]] = resolved;
                }
            }
            return result;
        }

        /**
         * 一组镜像属性
         */
        class MirrorSet {
            private int size;
            private final int[] indexes = new int[attributes.size()];

            void update() {
                this.size = 0;
                Arrays.fill(this.indexes, -1);
                for (int i = 0; i < MirrorSets.this.assigned.length; i++) {
                    if (MirrorSets.this.assigned[i] == this) {
                        this.indexes[this.size] = i;
                        this.size++;
                    }
                }
            }

            <A> int resolve(Object source, A annotation, ValueExtractor valueExtractor) {
                int result = -1;
                Object lastValue = null;
                for (int i = 0; i < this.size; i++) {
                    Method attribute = attributes.get(this.indexes[i]);
                    Object value = valueExtractor.extract(attribute, annotation);
                    boolean isDefaultValue = (value == null || isEquivalentToDefaultValue(attribute, value, valueExtractor));
                    if (isDefaultValue || ObjectUtils.nullSafeEquals(lastValue, value)) {
                        if (result == -1) {
                            result = this.indexes[i];
                        }
                        continue;
                    }
                    if (lastValue != null && !ObjectUtils.nullSafeEquals(lastValue, value)) {
                        String on = (source != null) ? " declared on " + source : "";
                        throw new AnnotationConfigurationException(String.format(
                                "Different @AliasFor mirror values for annotation [%s]%s; attribute '%s' "
                                        + "and its alias '%s' are declared with values of [%s] and [%s].",
                                getAnnotationType().getName(),
                                on,
                                attributes.get(result).getName(),
                                attribute.getName(),
                                ObjectUtils.nullSafeToString(lastValue),
                                ObjectUtils.nullSafeToString(value)
                        ));
                    }
                    result = this.indexes[i];
                    lastValue = value;
                }
                return result;
            }

            int size() {
                return this.size;
            }

            Method get(int index) {
                int attributeIndex = this.indexes[index];
                return attributes.get(attributeIndex);
            }

            int getAttributeIndex(int index) {
                return this.indexes[index];
            }
        }
    }
}
