package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.jdbc.meta.codegen.TemplateDataContext;
import org.clever.data.jdbc.meta.codegen.TemplateScope;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 17:41 <br/>
 */
public class CodegenJavaEntity extends AbstractCodegenHandler {
    @Override
    public Template getTemplate(Engine engine) {
        return engine.getTemplate("template/enjoy/java-entity.txt");
    }

    @Override
    public TemplateScope getScope() {
        return TemplateScope.TABLE;
    }

    @Override
    public Map<String, Object> getTemplateData(TemplateDataContext context) {
        return BeanCopyUtils.toMap(context.getEntityModel());
    }

    @Override
    public String getFileName(TemplateDataContext context) {
        return String.format("entity/%s.java", context.getEntityModel().getClassName());
    }

    @Override
    public String getPackageName(String packageName) {
        return packageName + ".entity";
    }
}
