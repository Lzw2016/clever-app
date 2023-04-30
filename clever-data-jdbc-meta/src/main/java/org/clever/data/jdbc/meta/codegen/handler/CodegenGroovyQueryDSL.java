package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.jdbc.meta.codegen.TemplateDataContext;
import org.clever.data.jdbc.meta.codegen.TemplateScope;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 17:48 <br/>
 */
public class CodegenGroovyQueryDSL extends AbstractCodegenHandler {
    @Override
    public Template getTemplate(Engine engine) {
        return engine.getTemplate("template/enjoy/groovy-query.txt");
    }

    @Override
    public TemplateScope getScope() {
        return TemplateScope.TABLE;
    }

    @Override
    public Map<String, Object> getTemplateData(TemplateDataContext context) {
        Map<String, Object> data = BeanCopyUtils.toMap(context.getEntityModel());
        data.put("importQueryEntity", String.format("%s.entity.%s", context.getConfig().getPackageName(), context.getEntityModel().getClassName()));
        return data;
    }

    @Override
    public String getFileName(TemplateDataContext context) {
        return String.format("query/Q%s.groovy", context.getEntityModel().getClassName());
    }

    @Override
    public String getPackageName(String packageName) {
        return packageName + ".query";
    }
}
