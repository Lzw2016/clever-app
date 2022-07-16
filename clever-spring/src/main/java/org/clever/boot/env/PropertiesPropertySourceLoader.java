package org.clever.boot.env;

import org.clever.core.env.PropertySource;
import org.clever.core.io.Resource;
import org.clever.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.clever.boot.env.OriginTrackedPropertiesLoader.Document;

/**
 * 将'.properties'文件加载到{@link PropertySource}的策略。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:42 <br/>
 */
public class PropertiesPropertySourceLoader implements PropertySourceLoader {
    private static final String XML_FILE_EXTENSION = ".xml";

    @Override
    public String[] getFileExtensions() {
        return new String[]{"properties", "xml"};
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public List<PropertySource<?>> load(String name, Resource resource) throws IOException {
        List<Map<String, ?>> properties = loadProperties(resource);
        if (properties.isEmpty()) {
            return Collections.emptyList();
        }
        List<PropertySource<?>> propertySources = new ArrayList<>(properties.size());
        for (int i = 0; i < properties.size(); i++) {
            String documentNumber = (properties.size() != 1) ? " (document #" + i + ")" : "";
            propertySources.add(new OriginTrackedMapPropertySource(
                    name + documentNumber,
                    Collections.unmodifiableMap(properties.get(i)), true)
            );
        }
        return propertySources;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, ?>> loadProperties(Resource resource) throws IOException {
        String filename = resource.getFilename();
        List<Map<String, ?>> result = new ArrayList<>();
        if (filename != null && filename.endsWith(XML_FILE_EXTENSION)) {
            result.add((Map) PropertiesLoaderUtils.loadProperties(resource));
        } else {
            List<Document> documents = new OriginTrackedPropertiesLoader(resource).load();
            documents.forEach((document) -> result.add(document.asMap()));
        }
        return result;
    }
}
