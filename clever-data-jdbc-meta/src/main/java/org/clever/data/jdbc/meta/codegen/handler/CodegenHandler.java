package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.data.jdbc.meta.AbstractMetaData;
import org.clever.data.jdbc.meta.codegen.EntityModel;

import java.util.Map;

/**
 * 代码生成接口
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 17:29 <br/>
 */
public interface CodegenHandler {
    /** 获取模版 */
    Template getTemplate(Engine engine);

    /** 获取模版数据 */
    Map<?, ?> getTemplateData(AbstractMetaData metaData, EntityModel entityModel);

    /** 获取生成的文件名 */
    String getFileName(EntityModel entityModel);
}
