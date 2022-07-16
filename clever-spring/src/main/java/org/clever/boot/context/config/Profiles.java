package org.clever.boot.context.config;

import org.clever.boot.context.properties.bind.BindResult;
import org.clever.boot.context.properties.bind.Bindable;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.core.ResolvableType;
import org.clever.core.env.AbstractEnvironment;
import org.clever.core.env.Environment;
import org.clever.core.style.ToStringCreator;
import org.clever.util.CollectionUtils;
import org.clever.util.LinkedMultiValueMap;
import org.clever.util.MultiValueMap;
import org.clever.util.StringUtils;

import java.util.*;
import java.util.function.Function;

/**
 * 提供对已直接在 {@link Environment} 上设置或将基于配置数据属性值设置的环境配置文件的访问。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:14 <br/>
 */
public class Profiles implements Iterable<String> {
    /**
     * 要设置以指定额外包含的活动配置文件的属性名称。
     */
    public static final String INCLUDE_PROFILES_PROPERTY_NAME = "clever.profiles.include";
    static final ConfigurationPropertyName INCLUDE_PROFILES = ConfigurationPropertyName.of(Profiles.INCLUDE_PROFILES_PROPERTY_NAME);
    private static final Bindable<MultiValueMap<String, String>> STRING_STRINGS_MAP = Bindable.of(
            ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class)
    );
    private static final Bindable<Set<String>> STRING_SET = Bindable.setOf(String.class);

    private final MultiValueMap<String, String> groups;
    private final List<String> activeProfiles;
    private final List<String> defaultProfiles;

    /**
     * 基于{@link Environment} 和 {@link Binder}创建一个新的 {@link Profiles} 实例。
     *
     * @param environment        源环境
     * @param binder             配置文件属性的 Binder
     * @param additionalProfiles 任何其他活动配置文件
     */
    Profiles(Environment environment, Binder binder, Collection<String> additionalProfiles) {
        this.groups = binder.bind("clever.profiles.group", STRING_STRINGS_MAP).orElseGet(LinkedMultiValueMap::new);
        this.activeProfiles = expandProfiles(getActivatedProfiles(environment, binder, additionalProfiles));
        this.defaultProfiles = expandProfiles(getDefaultProfiles(environment, binder));
    }

    private List<String> getActivatedProfiles(Environment environment, Binder binder, Collection<String> additionalProfiles) {
        return asUniqueItemList(getProfiles(environment, binder, Type.ACTIVE), additionalProfiles);
    }

    private List<String> getDefaultProfiles(Environment environment, Binder binder) {
        return asUniqueItemList(getProfiles(environment, binder, Type.DEFAULT));
    }

    private Collection<String> getProfiles(Environment environment, Binder binder, Type type) {
        String environmentPropertyValue = environment.getProperty(type.getName());
        Set<String> environmentPropertyProfiles = (!StringUtils.hasLength(environmentPropertyValue)) ?
                Collections.emptySet()
                : StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(environmentPropertyValue));
        Set<String> environmentProfiles = new LinkedHashSet<>(Arrays.asList(type.get(environment)));
        BindResult<Set<String>> boundProfiles = binder.bind(type.getName(), STRING_SET);
        if (hasProgrammaticallySetProfiles(type, environmentPropertyValue, environmentPropertyProfiles, environmentProfiles)) {
            if (!type.isMergeWithEnvironmentProfiles() || !boundProfiles.isBound()) {
                return environmentProfiles;
            }
            return boundProfiles.map((bound) -> merge(environmentProfiles, bound)).get();
        }
        return boundProfiles.orElse(type.getDefaultValue());
    }

    private boolean hasProgrammaticallySetProfiles(Type type, String environmentPropertyValue, Set<String> environmentPropertyProfiles, Set<String> environmentProfiles) {
        if (!StringUtils.hasLength(environmentPropertyValue)) {
            return !type.getDefaultValue().equals(environmentProfiles);
        }
        if (type.getDefaultValue().equals(environmentProfiles)) {
            return false;
        }
        return !environmentPropertyProfiles.equals(environmentProfiles);
    }

    private Set<String> merge(Set<String> environmentProfiles, Set<String> bound) {
        Set<String> result = new LinkedHashSet<>(environmentProfiles);
        result.addAll(bound);
        return result;
    }

    private List<String> expandProfiles(List<String> profiles) {
        Deque<String> stack = new ArrayDeque<>();
        asReversedList(profiles).forEach(stack::push);
        Set<String> expandedProfiles = new LinkedHashSet<>();
        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (expandedProfiles.add(current)) {
                asReversedList(this.groups.get(current)).forEach(stack::push);
            }
        }
        return asUniqueItemList(expandedProfiles);
    }

    private List<String> asReversedList(List<String> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<String> reversed = new ArrayList<>(list);
        Collections.reverse(reversed);
        return reversed;
    }

    private List<String> asUniqueItemList(Collection<String> profiles) {
        return asUniqueItemList(profiles, null);
    }

    private List<String> asUniqueItemList(Collection<String> profiles, Collection<String> additional) {
        LinkedHashSet<String> uniqueItems = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(additional)) {
            uniqueItems.addAll(additional);
        }
        uniqueItems.addAll(profiles);
        return Collections.unmodifiableList(new ArrayList<>(uniqueItems));
    }

    /**
     * 为所有{@link #getAccepted() 接受的配置文件}返回迭代器。
     */
    @Override
    public Iterator<String> iterator() {
        return getAccepted().iterator();
    }

    /**
     * 返回活动配置文件。
     */
    public List<String> getActive() {
        return this.activeProfiles;
    }

    /**
     * 返回默认配置文件。
     */
    public List<String> getDefault() {
        return this.defaultProfiles;
    }

    /**
     * 返回接受的配置文件。
     */
    public List<String> getAccepted() {
        return (!this.activeProfiles.isEmpty()) ? this.activeProfiles : this.defaultProfiles;
    }

    /**
     * 如果给定配置文件处于活动状态，则返回。
     *
     * @param profile 要测试的配置文件
     * @return 如果配置文件处于活动状态
     */
    public boolean isAccepted(String profile) {
        return getAccepted().contains(profile);
    }

    @Override
    public String toString() {
        ToStringCreator creator = new ToStringCreator(this);
        creator.append("active", getActive().toString());
        creator.append("default", getDefault().toString());
        creator.append("accepted", getAccepted().toString());
        return creator.toString();
    }

    /**
     * 可以获得的配置文件类型。
     */
    private enum Type {
        ACTIVE(AbstractEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, Environment::getActiveProfiles, true, Collections.emptySet()),
        DEFAULT(AbstractEnvironment.DEFAULT_PROFILES_PROPERTY_NAME, Environment::getDefaultProfiles, false, Collections.singleton("default"));

        private final Function<Environment, String[]> getter;
        private final boolean mergeWithEnvironmentProfiles;
        private final String name;
        private final Set<String> defaultValue;

        Type(String name, Function<Environment, String[]> getter, boolean mergeWithEnvironmentProfiles,
             Set<String> defaultValue) {
            this.name = name;
            this.getter = getter;
            this.mergeWithEnvironmentProfiles = mergeWithEnvironmentProfiles;
            this.defaultValue = defaultValue;
        }

        String getName() {
            return this.name;
        }

        String[] get(Environment environment) {
            return this.getter.apply(environment);
        }

        Set<String> getDefaultValue() {
            return this.defaultValue;
        }

        boolean isMergeWithEnvironmentProfiles() {
            return this.mergeWithEnvironmentProfiles;
        }
    }
}
