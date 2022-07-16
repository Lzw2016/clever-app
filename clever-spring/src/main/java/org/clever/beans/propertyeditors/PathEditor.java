package org.clever.beans.propertyeditors;

import org.clever.core.io.Resource;
import org.clever.core.io.ResourceEditor;
import org.clever.util.Assert;
import org.clever.util.ResourceUtils;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * {@code java.nio.file.Path} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:35 <br/>
 *
 * @see java.nio.file.Path
 * @see Paths#get(URI)
 * @see ResourceEditor
 * @see org.clever.core.io.ResourceLoader
 * @see FileEditor
 * @see URLEditor
 */
public class PathEditor extends PropertyEditorSupport {
    private final ResourceEditor resourceEditor;

    /**
     * 使用下面的默认ResourceEditor创建新的PathEditor
     */
    public PathEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    /**
     * 使用下面给定的ResourceEditor创建一个新的PathEditor
     *
     * @param resourceEditor 要使用的ResourceEditor
     */
    public PathEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        boolean nioPathCandidate = !text.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX);
        if (nioPathCandidate && !text.startsWith("/")) {
            try {
                URI uri = new URI(text);
                if (uri.getScheme() != null) {
                    nioPathCandidate = false;
                    // Let's try NIO file system providers via Paths.get(URI)
                    setValue(Paths.get(uri).normalize());
                    return;
                }
            } catch (URISyntaxException ex) {
                // Not a valid URI; potentially a Windows-style path after
                // a file prefix (let's try as clever resource location)
                nioPathCandidate = !text.startsWith(ResourceUtils.FILE_URL_PREFIX);
            } catch (FileSystemNotFoundException ex) {
                // URI scheme not registered for NIO (let's try URL
                // protocol handlers via clever's resource mechanism).
            }
        }
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        if (resource == null) {
            setValue(null);
        } else if (nioPathCandidate && !resource.exists()) {
            setValue(Paths.get(text).normalize());
        } else {
            try {
                setValue(resource.getFile().toPath());
            } catch (IOException ex) {
                throw new IllegalArgumentException("Failed to retrieve file for " + resource, ex);
            }
        }
    }

    @Override
    public String getAsText() {
        Path value = (Path) getValue();
        return (value != null ? value.toString() : "");
    }
}
