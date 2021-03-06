package org.clever.boot.context.config;

import org.clever.boot.context.config.LocationResourceLoader.ResourceType;
import org.clever.boot.context.properties.bind.Binder;
import org.clever.boot.env.PropertiesPropertySourceLoader;
import org.clever.boot.env.PropertySourceLoader;
import org.clever.boot.env.YamlPropertySourceLoader;
import org.clever.core.Ordered;
import org.clever.core.env.Environment;
import org.clever.core.io.ClassPathResource;
import org.clever.core.io.Resource;
import org.clever.core.io.ResourceLoader;
import org.clever.util.Assert;
import org.clever.util.ObjectUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 为标准位置 {@link ConfigDataLocationResolver}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/07 22:07 <br/>
 */
public class StandardConfigDataLocationResolver implements ConfigDataLocationResolver<StandardConfigDataResource>, Ordered {
    private static final String PREFIX = "resource:";
    static final String CONFIG_NAME_PROPERTY = "clever.config.name";
    private static final String[] DEFAULT_CONFIG_NAMES = {"app"};
    private static final Pattern URL_PREFIX = Pattern.compile("^([a-zA-Z][a-zA-Z\\d*]*?:)(.*$)");
    private static final Pattern EXTENSION_HINT_PATTERN = Pattern.compile("^(.*)\\[(\\.\\w+)](?!\\[)$");
    private static final String NO_PROFILE = null;

    private final Logger logger;
    private final List<PropertySourceLoader> propertySourceLoaders;
    private final String[] configNames;
    private final LocationResourceLoader resourceLoader;

    /**
     * 创建新的 {@link StandardConfigDataLocationResolver}
     *
     * @param logger         要使用的Logger
     * @param binder         由首字母 {@link Environment} 支持的 Binder
     * @param resourceLoader 用于加载资源的 {@link ResourceLoader}
     */
    public StandardConfigDataLocationResolver(Logger logger, Binder binder, ResourceLoader resourceLoader) {
        this.logger = logger;
        this.propertySourceLoaders = Arrays.asList(new YamlPropertySourceLoader(), new PropertiesPropertySourceLoader());
        this.configNames = getConfigNames(binder);
        this.resourceLoader = new LocationResourceLoader(resourceLoader);
    }

    private String[] getConfigNames(Binder binder) {
        String[] configNames = binder.bind(CONFIG_NAME_PROPERTY, String[].class).orElse(DEFAULT_CONFIG_NAMES);
        for (String configName : configNames) {
            validateConfigName(configName);
        }
        return configNames;
    }

    private void validateConfigName(String name) {
        Assert.state(!name.contains("*"), () -> "Config name '" + name + "' cannot contain '*'");
    }

    @Override
    public double getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public boolean isResolvable(ConfigDataLocationResolverContext context, ConfigDataLocation location) {
        return true;
    }

    @Override
    public List<StandardConfigDataResource> resolve(ConfigDataLocationResolverContext context, ConfigDataLocation location) throws ConfigDataNotFoundException {
        return resolve(getReferences(context, location.split()));
    }

    private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context, ConfigDataLocation[] configDataLocations) {
        Set<StandardConfigDataReference> references = new LinkedHashSet<>();
        for (ConfigDataLocation configDataLocation : configDataLocations) {
            references.addAll(getReferences(context, configDataLocation));
        }
        return references;
    }

    private Set<StandardConfigDataReference> getReferences(ConfigDataLocationResolverContext context, ConfigDataLocation configDataLocation) {
        String resourceLocation = getResourceLocation(context, configDataLocation);
        try {
            if (isDirectory(resourceLocation)) {
                return getReferencesForDirectory(configDataLocation, resourceLocation, NO_PROFILE);
            }
            return getReferencesForFile(configDataLocation, resourceLocation, NO_PROFILE);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Unable to load config data from '" + configDataLocation + "'", ex);
        }
    }

    @Override
    public List<StandardConfigDataResource> resolveProfileSpecific(ConfigDataLocationResolverContext context, ConfigDataLocation location, Profiles profiles) {
        return resolve(getProfileSpecificReferences(context, location.split(), profiles));
    }

    private Set<StandardConfigDataReference> getProfileSpecificReferences(ConfigDataLocationResolverContext context, ConfigDataLocation[] configDataLocations, Profiles profiles) {
        Set<StandardConfigDataReference> references = new LinkedHashSet<>();
        for (String profile : profiles) {
            for (ConfigDataLocation configDataLocation : configDataLocations) {
                String resourceLocation = getResourceLocation(context, configDataLocation);
                references.addAll(getReferences(configDataLocation, resourceLocation, profile));
            }
        }
        return references;
    }

    private String getResourceLocation(ConfigDataLocationResolverContext context, ConfigDataLocation configDataLocation) {
        String resourceLocation = configDataLocation.getNonPrefixedValue(PREFIX);
        boolean isAbsolute = resourceLocation.startsWith("/") || URL_PREFIX.matcher(resourceLocation).matches();
        if (isAbsolute) {
            return resourceLocation;
        }
        ConfigDataResource parent = context.getParent();
        if (parent instanceof StandardConfigDataResource) {
            String parentResourceLocation = ((StandardConfigDataResource) parent).getReference().getResourceLocation();
            String parentDirectory = parentResourceLocation.substring(0, parentResourceLocation.lastIndexOf("/") + 1);
            return parentDirectory + resourceLocation;
        }
        return resourceLocation;
    }

    private Set<StandardConfigDataReference> getReferences(ConfigDataLocation configDataLocation, String resourceLocation, String profile) {
        if (isDirectory(resourceLocation)) {
            return getReferencesForDirectory(configDataLocation, resourceLocation, profile);
        }
        return getReferencesForFile(configDataLocation, resourceLocation, profile);
    }

    private Set<StandardConfigDataReference> getReferencesForDirectory(ConfigDataLocation configDataLocation, String directory, String profile) {
        Set<StandardConfigDataReference> references = new LinkedHashSet<>();
        for (String name : this.configNames) {
            Deque<StandardConfigDataReference> referencesForName = getReferencesForConfigName(
                    name, configDataLocation, directory, profile
            );
            references.addAll(referencesForName);
        }
        return references;
    }

    private Deque<StandardConfigDataReference> getReferencesForConfigName(String name, ConfigDataLocation configDataLocation, String directory, String profile) {
        Deque<StandardConfigDataReference> references = new ArrayDeque<>();
        for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
            for (String extension : propertySourceLoader.getFileExtensions()) {
                StandardConfigDataReference reference = new StandardConfigDataReference(
                        configDataLocation, directory, directory + name, profile, extension, propertySourceLoader
                );
                if (!references.contains(reference)) {
                    references.addFirst(reference);
                }
            }
        }
        return references;
    }

    private Set<StandardConfigDataReference> getReferencesForFile(ConfigDataLocation configDataLocation, String file, String profile) {
        Matcher extensionHintMatcher = EXTENSION_HINT_PATTERN.matcher(file);
        boolean extensionHintLocation = extensionHintMatcher.matches();
        if (extensionHintLocation) {
            file = extensionHintMatcher.group(1) + extensionHintMatcher.group(2);
        }
        for (PropertySourceLoader propertySourceLoader : this.propertySourceLoaders) {
            String extension = getLoadableFileExtension(propertySourceLoader, file);
            if (extension != null) {
                String root = file.substring(0, file.length() - extension.length() - 1);
                StandardConfigDataReference reference = new StandardConfigDataReference(
                        configDataLocation, null, root, profile,
                        (!extensionHintLocation) ? extension : null, propertySourceLoader
                );
                return Collections.singleton(reference);
            }
        }
        throw new IllegalStateException(
                "File extension is not known to any PropertySourceLoader. "
                        + "If the location is meant to reference a directory, it must end in '/' or File.separator"
        );
    }

    private String getLoadableFileExtension(PropertySourceLoader loader, String file) {
        for (String fileExtension : loader.getFileExtensions()) {
            if (StringUtils.endsWithIgnoreCase(file, fileExtension)) {
                return fileExtension;
            }
        }
        return null;
    }

    private boolean isDirectory(String resourceLocation) {
        return resourceLocation.endsWith("/") || resourceLocation.endsWith(File.separator);
    }

    private List<StandardConfigDataResource> resolve(Set<StandardConfigDataReference> references) {
        List<StandardConfigDataResource> resolved = new ArrayList<>();
        for (StandardConfigDataReference reference : references) {
            resolved.addAll(resolve(reference));
        }
        if (resolved.isEmpty()) {
            resolved.addAll(resolveEmptyDirectories(references));
        }
        return resolved;
    }

    private Collection<StandardConfigDataResource> resolveEmptyDirectories(Set<StandardConfigDataReference> references) {
        Set<StandardConfigDataResource> empty = new LinkedHashSet<>();
        for (StandardConfigDataReference reference : references) {
            if (reference.getDirectory() != null) {
                empty.addAll(resolveEmptyDirectories(reference));
            }
        }
        return empty;
    }

    private Set<StandardConfigDataResource> resolveEmptyDirectories(StandardConfigDataReference reference) {
        if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
            return resolveNonPatternEmptyDirectories(reference);
        }
        return resolvePatternEmptyDirectories(reference);
    }

    private Set<StandardConfigDataResource> resolveNonPatternEmptyDirectories(StandardConfigDataReference reference) {
        Resource resource = this.resourceLoader.getResource(reference.getDirectory());
        return (resource instanceof ClassPathResource || !resource.exists()) ?
                Collections.emptySet()
                : Collections.singleton(new StandardConfigDataResource(reference, resource, true));
    }

    private Set<StandardConfigDataResource> resolvePatternEmptyDirectories(StandardConfigDataReference reference) {
        Resource[] subdirectories = this.resourceLoader.getResources(reference.getDirectory(), ResourceType.DIRECTORY);
        ConfigDataLocation location = reference.getConfigDataLocation();
        if (!location.isOptional() && ObjectUtils.isEmpty(subdirectories)) {
            String message = String.format("Config data location '%s' contains no subdirectories", location);
            throw new ConfigDataLocationNotFoundException(location, message, null);
        }
        return Arrays.stream(subdirectories).filter(Resource::exists)
                .map((resource) -> new StandardConfigDataResource(reference, resource, true))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<StandardConfigDataResource> resolve(StandardConfigDataReference reference) {
        if (!this.resourceLoader.isPattern(reference.getResourceLocation())) {
            return resolveNonPattern(reference);
        }
        return resolvePattern(reference);
    }

    private List<StandardConfigDataResource> resolveNonPattern(StandardConfigDataReference reference) {
        Resource resource = this.resourceLoader.getResource(reference.getResourceLocation());
        if (!resource.exists() && reference.isSkippable()) {
            logSkippingResource(reference);
            return Collections.emptyList();
        }
        return Collections.singletonList(createConfigResourceLocation(reference, resource));
    }

    private List<StandardConfigDataResource> resolvePattern(StandardConfigDataReference reference) {
        List<StandardConfigDataResource> resolved = new ArrayList<>();
        for (Resource resource : this.resourceLoader.getResources(reference.getResourceLocation(), ResourceType.FILE)) {
            if (!resource.exists() && reference.isSkippable()) {
                logSkippingResource(reference);
            } else {
                resolved.add(createConfigResourceLocation(reference, resource));
            }
        }
        return resolved;
    }

    private void logSkippingResource(StandardConfigDataReference reference) {
        this.logger.trace(String.format("Skipping missing resource %s", reference));
    }

    private StandardConfigDataResource createConfigResourceLocation(StandardConfigDataReference reference, Resource resource) {
        return new StandardConfigDataResource(reference, resource);
    }
}
