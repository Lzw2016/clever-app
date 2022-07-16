package org.clever.boot.env;

import org.clever.core.env.PropertySource;
import org.clever.core.io.Resource;
import org.clever.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 将'.yml'文件(或 '.yaml')加载到{@link PropertySource}的策略。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:42 <br/>
 */
public class YamlPropertySourceLoader implements PropertySourceLoader {
    @Override
    public String[] getFileExtensions() {
        return new String[]{"yml", "yaml"};
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        if (!ClassUtils.isPresent("org.yaml.snakeyaml.Yaml", getClass().getClassLoader())) {
            throw new IllegalStateException(
                    "Attempted to load " + name + " but snakeyaml was not found on the classpath"
            );
        }
        List<Map<String, Object>> loaded = new OriginTrackedYamlLoader(resource).load();
        if (loaded.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertySource<?>> propertySources = new ArrayList<>(loaded.size());
        for (int i = 0; i < loaded.size(); i++) {
            String documentNumber = (loaded.size() != 1) ? " (document #" + i + ")" : "";
            propertySources.add(new OriginTrackedMapPropertySource(
                    name + documentNumber,
                    Collections.unmodifiableMap(loaded.get(i)), true)
            );
        }
        return propertySources;
    }
}
