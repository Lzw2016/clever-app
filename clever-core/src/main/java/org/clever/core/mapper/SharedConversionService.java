package org.clever.core.mapper;

import org.clever.format.support.DefaultFormattingConversionService;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/07/27 17:12 <br/>
 */
public class SharedConversionService {
    /**
     * 共享的转换器
     */
    public static final DefaultFormattingConversionService Instance = new DefaultFormattingConversionService();
}
