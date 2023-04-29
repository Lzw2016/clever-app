package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.data.jdbc.meta.codegen.EntityModel;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 17:47 <br/>
 */
public class CodegenJavaQueryDSL extends AbstractCodegenHandler {
    @Override
    public Template getTemplate(Engine engine) {
        return engine.getTemplate("template/enjoy/java-query.txt");
    }

    @Override
    public String getFileName(EntityModel entityModel) {
        return String.format("Q%s.java", entityModel.getClassName());
    }
}
