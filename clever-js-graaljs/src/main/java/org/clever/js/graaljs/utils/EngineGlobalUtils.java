package org.clever.js.graaljs.utils;

import org.clever.js.api.internal.LoggerConsole;
import org.clever.js.graaljs.internal.GraalInterop;
import org.clever.js.graaljs.internal.GraalLoggerFactory;
import org.clever.js.graaljs.support.GraalObjectToString;

import java.util.Map;

public class EngineGlobalUtils {

    public static void putGlobalObjects(Map<String, Object> registerGlobalVars) {
        LoggerConsole.INSTANCE.setObjectToString(GraalObjectToString.INSTANCE);
        registerGlobalVars.put("console", LoggerConsole.INSTANCE);
        registerGlobalVars.put("print", LoggerConsole.INSTANCE);
        registerGlobalVars.put("LoggerFactory", GraalLoggerFactory.INSTANCE);
        registerGlobalVars.put("Interop", GraalInterop.INSTANCE);
    }
}
