package org.clever.js.graaljs.utils;

import org.clever.js.api.internal.LoggerConsole;
import org.clever.js.graaljs.internal.GraalInterop;
import org.clever.js.graaljs.internal.GraalLoggerFactory;
import org.clever.js.graaljs.support.GraalObjectToString;

import java.util.Map;

public class EngineGlobalUtils {

    public static void putGlobalObjects(Map<String, Object> registerGlobalVars) {
        LoggerConsole.Instance.setObjectToString(GraalObjectToString.Instance);
        registerGlobalVars.put("console", LoggerConsole.Instance);
        registerGlobalVars.put("print", LoggerConsole.Instance);
        registerGlobalVars.put("LoggerFactory", GraalLoggerFactory.Instance);
        registerGlobalVars.put("Interop", GraalInterop.Instance);
    }
}
