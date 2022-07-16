package org.clever.asm;

/**
 * Utility class exposing constants related to Spring's internal repackaging
 * of the ASM bytecode library: currently based on ASM 9.x plus minor patches.
 *
 * <p>See <a href="package-summary.html">package-level javadocs</a> for more
 * information on {@code org.clever.asm}.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 13:03 <br/>
 */
public final class SpringAsmInfo {
    /**
     * The ASM compatibility version for Spring's ASM visitor implementations:
     * currently {@link Opcodes#ASM10_EXPERIMENTAL}, as of Spring Framework 5.3.
     */
    public static final int ASM_VERSION = Opcodes.ASM10_EXPERIMENTAL;
}
