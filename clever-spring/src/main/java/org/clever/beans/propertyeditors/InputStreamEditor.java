package org.clever.beans.propertyeditors;

import org.clever.core.io.Resource;
import org.clever.core.io.ResourceEditor;
import org.clever.util.Assert;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

/**
 * 单向PropertyEditor，可将文本字符串转换为{@code java.io.InputStream}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/04 13:32 <br/>
 *
 * @see java.io.InputStream
 * @see org.clever.core.io.ResourceEditor
 * @see org.clever.core.io.ResourceLoader
 * @see URLEditor
 * @see FileEditor
 */
public class InputStreamEditor extends PropertyEditorSupport {
    private final ResourceEditor resourceEditor;

    /**
     * 使用下面的默认ResourceEditor创建新的InputStreamEditor
     */
    public InputStreamEditor() {
        this.resourceEditor = new ResourceEditor();
    }

    /**
     * 使用下面给定的ResourceEditor创建一个新的InputStreamEditor
     *
     * @param resourceEditor 要使用的ResourceEditor
     */
    public InputStreamEditor(ResourceEditor resourceEditor) {
        Assert.notNull(resourceEditor, "ResourceEditor must not be null");
        this.resourceEditor = resourceEditor;
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException {
        this.resourceEditor.setAsText(text);
        Resource resource = (Resource) this.resourceEditor.getValue();
        try {
            setValue(resource != null ? resource.getInputStream() : null);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to retrieve InputStream for " + resource, ex);
        }
    }

    /**
     * 此实现返回null，表示没有适当的文本表示
     */
    @Override
    public String getAsText() {
        return null;
    }
}
