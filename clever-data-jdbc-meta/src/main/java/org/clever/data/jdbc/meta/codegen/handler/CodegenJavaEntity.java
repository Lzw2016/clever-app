package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.data.jdbc.meta.codegen.EntityModel;

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
    public String getFileName(EntityModel entityModel) {
        return String.format("%s.java", entityModel.getClassName());
    }
}
