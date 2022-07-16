package org.clever.beans.propertyeditors;

import org.clever.core.io.Resource;
import org.clever.core.io.ResourceEditor;
import org.clever.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.net.URL;

/**
 * {@code java.net.URL} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:40 <br/>
 *
 * @see java.net.URL
 * @see org.clever.core.io.ResourceEditor
 * @see org.clever.core.io.ResourceLoader
 * @see FileEditor
 * @see InputStreamEditor
 */
public class URLEditor extends PropertyEditorSupport {
    private final ResourceEditor resourceEditor;

    /**
     * 使用下面的默认ResourceEditor创建一个新的URLEditor
     */
    public URLEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    /**
     * 使用下面给定的ResourceEditor创建一个新的URLEditor
     *
     * @param resourceEditor 要使用的ResourceEditor
     */
    public URLEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        try {
            setValue(resource != null ? resource.getURL() : null);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
        }
    }

    @Override
    public String getAsText() {
        URL value = (URL) getValue();
        return (value != null ? value.toExternalForm() : "");
    }
}
