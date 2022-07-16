package org.clever.boot.ansi;

import org.clever.util.Assert;

import java.util.Locale;

/**
 * 生成 ANSI 编码输出，自动尝试检测终端是否支持 ANSI
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:07 <br/>
 */
public abstract class AnsiOutput {
    private static final String ENCODE_JOIN = ";";
    private static Enabled enabled = Enabled.DETECT;
    private static Boolean consoleAvailable;
    private static Boolean ansiCapable;
    private static final String OPERATING_SYSTEM_NAME = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
    private static final String ENCODE_START = "\033[";
    private static final String ENCODE_END = "m";
    private static final String RESET = "0;" + AnsiColor.DEFAULT;

    /**
     * 设置是否启用 ANSI 输出
     *
     * @param enabled 如果启用、禁用或检测到 ANSI
     */
    public static void setEnabled(Enabled enabled) {
        Assert.notNull(enabled, "Enabled must not be null");
        AnsiOutput.enabled = enabled;
    }

    /**
     * 如果启用 ANSI 输出，则返回
     *
     * @return 如果启用、禁用或检测到 ANSI
     */
    public static Enabled getEnabled() {
        return AnsiOutput.enabled;
    }

    /**
     * 设置 System.console() 是否已知可用
     *
     * @param consoleAvailable 如果已知控制台可用或为 null 以使用标准检测逻辑
     */
    public static void setConsoleAvailable(Boolean consoleAvailable) {
        AnsiOutput.consoleAvailable = consoleAvailable;
    }

    /**
     * 如果启用了输出，则编码单个 {@link AnsiElement}
     *
     * @param element 要编码的元素
     * @return 编码元素或空字符串
     */
    public static String encode(AnsiElement element) {
        if (isEnabled()) {
            return ENCODE_START + element + ENCODE_END;
        }
        return "";
    }

    /**
     * 从指定的元素创建一个新的 ANSI 字符串。任何 {@link AnsiElement} 都将根据需要进行编码
     *
     * @param elements 要编码的元素
     * @return 一串编码元素
     */
    public static String toString(Object... elements) {
        StringBuilder sb = new StringBuilder();
        if (isEnabled()) {
            buildEnabled(sb, elements);
        } else {
            buildDisabled(sb, elements);
        }
        return sb.toString();
    }

    private static void buildEnabled(StringBuilder sb, Object[] elements) {
        boolean writingAnsi = false;
        boolean containsEncoding = false;
        for (Object element : elements) {
            if (element instanceof AnsiElement) {
                containsEncoding = true;
                if (!writingAnsi) {
                    sb.append(ENCODE_START);
                    writingAnsi = true;
                } else {
                    sb.append(ENCODE_JOIN);
                }
            } else {
                if (writingAnsi) {
                    sb.append(ENCODE_END);
                    writingAnsi = false;
                }
            }
            sb.append(element);
        }
        if (containsEncoding) {
            sb.append(writingAnsi ? ENCODE_JOIN : ENCODE_START);
            sb.append(RESET);
            sb.append(ENCODE_END);
        }
    }

    private static void buildDisabled(StringBuilder sb, Object[] elements) {
        for (Object element : elements) {
            if (!(element instanceof AnsiElement) && element != null) {
                sb.append(element);
            }
        }
    }

    private static boolean isEnabled() {
        if (enabled == Enabled.DETECT) {
            if (ansiCapable == null) {
                ansiCapable = detectIfAnsiCapable();
            }
            return ansiCapable;
        }
        return enabled == Enabled.ALWAYS;
    }

    private static boolean detectIfAnsiCapable() {
        try {
            if (Boolean.FALSE.equals(consoleAvailable)) {
                return false;
            }
            if ((consoleAvailable == null) && (System.console() == null)) {
                return false;
            }
            return !(OPERATING_SYSTEM_NAME.contains("win"));
        } catch (Throwable ex) {
            return false;
        }
    }

    /**
     * 要传递给 {@link AnsiOutput#setEnabled} 的可能值。确定何时输出用于着色应用程序输出的 ANSI 转义序列。
     */
    public enum Enabled {
        /**
         * 尝试检测 ANSI 着色功能是否可用。 {@link AnsiOutput} 的默认值
         */
        DETECT,
        /**
         * 启用 ANSI 彩色输出。
         */
        ALWAYS,
        /**
         * 禁用 ANSI 彩色输出
         */
        NEVER
    }
}
