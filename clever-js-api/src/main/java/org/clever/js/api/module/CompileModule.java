package org.clever.js.api.module;

import org.clever.js.api.folder.Folder;

/**
 * 编译脚本成ScriptModule
 *
 * @param <T> script引擎对象类型
 */
public interface CompileModule<T> {
    /**
     * 编译 Json Module
     *
     * @return 直接返回exports
     */
    T compileJsonModule(Folder path) throws Exception;

    /**
     * 编译 JavaScript Module
     *
     * @return 返回script对象
     */
    T compileScriptModule(Folder path) throws Exception;
}
