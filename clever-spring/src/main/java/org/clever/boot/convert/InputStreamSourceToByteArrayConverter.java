package org.clever.boot.convert;

import org.clever.boot.origin.Origin;
import org.clever.core.convert.converter.Converter;
import org.clever.core.io.InputStreamSource;
import org.clever.core.io.Resource;
import org.clever.util.FileCopyUtils;

import java.io.IOException;

/**
 * {@link Converter} 把 {@link InputStreamSource} 转换为 {@code byte[]}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:54 <br/>
 */
class InputStreamSourceToByteArrayConverter implements Converter<InputStreamSource, byte[]> {
    @Override
    public byte[] convert(InputStreamSource source) {
        try {
            return FileCopyUtils.copyToByteArray(source.getInputStream());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read from " + getName(source), ex);
        }
    }

    private String getName(InputStreamSource source) {
        Origin origin = Origin.from(source);
        if (origin != null) {
            return origin.toString();
        }
        if (source instanceof Resource) {
            return ((Resource) source).getDescription();
        }
        return "input stream source";
    }
}
