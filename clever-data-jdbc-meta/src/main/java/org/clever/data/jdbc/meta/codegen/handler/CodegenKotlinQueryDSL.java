package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.data.jdbc.meta.AbstractMetaData;
import org.clever.data.jdbc.meta.codegen.CodegenCodeConfig;
import org.clever.data.jdbc.meta.codegen.EntityModel;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 17:51 <br/>
 */
public class CodegenKotlinQueryDSL extends AbstractCodegenHandler {
    @Override
    public Template getTemplate(Engine engine) {
        return engine.getTemplate("template/enjoy/kotlin-query.txt");
    }

    @Override
    public Map<String, Object> getTemplateData(AbstractMetaData metaData, EntityModel entityModel, CodegenCodeConfig config) {
        Map<String, Object> data = super.getTemplateData(metaData, entityModel, config);
        data.put("importQueryEntity", String.format("%s.entity.%s", config.getPackageName(), entityModel.getClassName()));
        return data;
    }

    @Override
    public String getFileName(EntityModel entityModel) {
        return String.format("query/Q%s.kt", entityModel.getClassName());
    }

    @Override
    public String getPackageName(String packageName) {
        return packageName + ".query";
    }
}
