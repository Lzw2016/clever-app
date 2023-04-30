package org.clever.data.jdbc.meta.codegen.handler;

import com.jfinal.template.Engine;
import com.jfinal.template.Template;
import org.clever.data.jdbc.meta.codegen.TemplateDataContext;
import org.clever.data.jdbc.meta.codegen.TemplateScope;

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

    /** 模版范围 */
    TemplateScope getScope();

    /** 获取模版数据 */
    Map<String, Object> getTemplateData(TemplateDataContext context);

    /** 获取生成的文件名(可以设置文件路径) */
    String getFileName(TemplateDataContext context);

    /** 设置 class package 名 */
    String getPackageName(String packageName);
}
