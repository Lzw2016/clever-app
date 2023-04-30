package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.jdbc.meta.codegen.CodegenConstant;
import org.clever.data.jdbc.meta.codegen.TemplateDataContext;
import org.clever.data.jdbc.meta.codegen.TemplateScope;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/30 09:11 <br/>
 */
public class CodegenDbDocMarkdown extends AbstractCodegenHandler {
    @Override
    public Template getTemplate(Engine engine) {
        return engine.getTemplate("template/enjoy/db_doc_md.txt");
    }

    @Override
    public TemplateScope getScope() {
        return TemplateScope.SCHEMA;
    }

    @Override
    public Map<String, Object> getTemplateData(TemplateDataContext context) {
        Map<String, Object> data = BeanCopyUtils.toMap(context.getSchema());
        data.put("title", CodegenConstant.TITLE);
        data.put("description", CodegenConstant.DESCRIPTION);
        data.put("version", CodegenConstant.VERSION);
        return data;
    }

    @Override
    public String getFileName(TemplateDataContext context) {
        String fileName = "数据库设计文档";
        if (StringUtils.isNotBlank(CodegenConstant.TITLE)) {
            fileName = CodegenConstant.TITLE;
        }
        return String.format("%s(%s).md", fileName, context.getSchema().getName());
    }

    @Override
    public String getPackageName(String packageName) {
        return packageName;
    }
}
