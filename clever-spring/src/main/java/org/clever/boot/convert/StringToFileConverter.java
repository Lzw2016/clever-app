package org.clever.boot.convert;

import org.clever.core.convert.converter.Converter;
import org.clever.core.io.DefaultResourceLoader;
import org.clever.core.io.Resource;
import org.clever.core.io.ResourceLoader;
import org.clever.util.ResourceUtils;

import java.io.File;
import java.io.IOException;

/**
 * {@link Converter}将{@link String}转换为{@link File}。支持基本文件转换和文件URL。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:54 <br/>
 */
class StringToFileConverter implements Converter<String, File> {
    private static final ResourceLoader resourceLoader = new DefaultResourceLoader(null);

    @Override
    public File convert(String source) {
        if (ResourceUtils.isUrl(source)) {
            return getFile(resourceLoader.getResource(source));
        }
        File file = new File(source);
        if (file.exists()) {
            return file;
        }
        Resource resource = resourceLoader.getResource(source);
        if (resource.exists()) {
            return getFile(resource);
        }
        return file;
    }

    private File getFile(Resource resource) {
        try {
            return resource.getFile();
        } catch (IOException ex) {
            throw new IllegalStateException("Could not retrieve file for " + resource + ": " + ex.getMessage());
        }
    }
}
