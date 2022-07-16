package org.clever.beans.propertyeditors;

import org.clever.core.io.Resource;
import org.clever.core.io.ResourceEditor;
import org.clever.util.Assert;
import org.xml.sax.InputSource;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * {@code org.xml.sax.InputSource} 编辑器
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:32 <br/>
 *
 * @see org.xml.sax.InputSource
 * @see org.clever.core.io.ResourceEditor
 * @see org.clever.core.io.ResourceLoader
 * @see URLEditor
 * @see FileEditor
 */
public class InputSourceEditor extends PropertyEditorSupport {
    private final ResourceEditor resourceEditor;

    /**
     * 使用下面的默认ResourceEditor创建一个新的InputSourceEditor
     */
    public InputSourceEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    /**
     * 使用下面给定的ResourceEditor创建一个新的InputSourceEditor
     *
     * @param resourceEditor 要使用的ResourceEditor
     */
    public InputSourceEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        try {
            setValue(resource != null ? new InputSource(resource.getURL().toString()) : null);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not retrieve URL for " + resource + ": " + ex.getMessage());
        }
    }

    @Override
    public String getAsText() {
        InputSource value = (InputSource) getValue();
        return (value != null ? value.getSystemId() : "");
    }
}
