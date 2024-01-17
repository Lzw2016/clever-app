package org.clever.js.graaljs.utils;

import org.clever.js.api.internal.LoggerConsole;
import org.clever.js.graaljs.internal.GraalInterop;
import org.clever.js.graaljs.internal.GraalLoggerFactory;
import org.clever.js.graaljs.internal.support.GraalObjectToString;

import java.util.Map;

public class EngineGlobalUtils {

    public static void putGlobalObjects(Map<String, Object> contextMap) {
        LoggerConsole.Instance.setObjectToString(GraalObjectToString.Instance);
        contextMap.put("console", LoggerConsole.Instance);
        contextMap.put("print", LoggerConsole.Instance);
        contextMap.put("LoggerFactory", GraalLoggerFactory.Instance);
        contextMap.put("Interop", GraalInterop.Instance);
    }
}
