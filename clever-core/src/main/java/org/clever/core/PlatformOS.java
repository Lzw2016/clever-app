package org.clever.core;

import java.util.Locale;

/**
 * 作者：lizw <br/>
 * 创建时间：2023/05/18 16:56 <br/>
 */
public class PlatformOS {
    private static final boolean IS_OS_WINDOWS = System.getProperty("os.name", "").toLowerCase(Locale.US).contains("win");
    private static final boolean IS_OS_MAC = System.getProperty("os.name", "").toLowerCase(Locale.US).contains("mac os");

    public static boolean isWindows() {
        return IS_OS_WINDOWS;
    }

    public static boolean isMacOS() {
        return IS_OS_MAC;
    }

    public static boolean isLinux() {
        return !IS_OS_WINDOWS && !IS_OS_MAC;
    }
}
