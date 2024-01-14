package org.clever.app.mapper;

import java.util.List;
import java.util.Map;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/14 16:11 <br/>
 */
public interface MapperTest {

    List<Map<String, Object>> q01(Long id);

    List<Map<String, Object>> q02(String a);
}
