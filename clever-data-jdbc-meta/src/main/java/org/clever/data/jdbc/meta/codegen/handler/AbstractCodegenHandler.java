package org.clever.data.jdbc.meta.codegen.handler;

import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.jdbc.meta.AbstractMetaData;
import org.clever.data.jdbc.meta.codegen.CodegenCodeConfig;
import org.clever.data.jdbc.meta.codegen.EntityModel;

import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/04/29 17:45 <br/>
 */
public abstract class AbstractCodegenHandler implements CodegenHandler {
    @Override
    public Map<String, Object> getTemplateData(AbstractMetaData metaData, EntityModel entityModel, CodegenCodeConfig config) {
        return BeanCopyUtils.toMap(entityModel);
    }

    @Override
    public String getPackageName(String packageName) {
        return packageName;
    }
}
