package org.clever.data.jdbc.meta.codegen;

import lombok.Data;
import org.clever.data.jdbc.meta.AbstractMetaData;
import org.clever.data.jdbc.meta.model.Schema;

import java.util.List;

/**
 * 模版数据上下文
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/04/30 10:44 <br/>
 */
@Data
public class TemplateDataContext {
    private final CodegenCodeConfig config;
    private final AbstractMetaData metaData;
    private final List<Schema> schemas;
    private Schema schema;
    private EntityModel entityModel;

    public TemplateDataContext(CodegenCodeConfig config, AbstractMetaData metaData, List<Schema> schemas) {
        this.config = config;
        this.metaData = metaData;
        this.schemas = schemas;
    }
}
