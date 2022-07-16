package org.clever.boot.ansi;

import org.clever.core.env.PropertyResolver;
import org.clever.core.env.PropertySource;
import org.clever.util.StringUtils;

import java.util.*;
import java.util.function.IntFunction;

/**
 * {@link PropertyResolver} for {@link AnsiStyle}, {@link AnsiColor},
 * {@link AnsiBackground} and {@link Ansi8BitColor} elements. Supports properties of the
 * form {@code AnsiStyle.BOLD}, {@code AnsiColor.RED} or {@code AnsiBackground.GREEN}.
 * Also supports a prefix of {@code Ansi.} which is an aggregation of everything (with
 * background colors prefixed {@code BG_}).
 * <p>
 * ANSI 8-bit color codes can be used with {@code AnsiColor} and {@code AnsiBackground}.
 * For example, {@code AnsiColor.208} will render orange text.
 * <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">Wikipedia</a> has a complete
 * list of the 8-bit color codes that can be used.
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/14 22:17 <br/>
 */
public class AnsiPropertySource extends PropertySource<AnsiElement> {
    private static final Iterable<Mapping> MAPPINGS;

    static {
        List<Mapping> mappings = new ArrayList<>();
        mappings.add(new EnumMapping<>("AnsiStyle.", AnsiStyle.class));
        mappings.add(new EnumMapping<>("AnsiColor.", AnsiColor.class));
        mappings.add(new Ansi8BitColorMapping("AnsiColor.", Ansi8BitColor::foreground));
        mappings.add(new EnumMapping<>("AnsiBackground.", AnsiBackground.class));
        mappings.add(new Ansi8BitColorMapping("AnsiBackground.", Ansi8BitColor::background));
        mappings.add(new EnumMapping<>("Ansi.", AnsiStyle.class));
        mappings.add(new EnumMapping<>("Ansi.", AnsiColor.class));
        mappings.add(new EnumMapping<>("Ansi.BG_", AnsiBackground.class));
        MAPPINGS = Collections.unmodifiableList(mappings);
    }

    private final boolean encode;

    /**
     * 创建一个新的 {@link AnsiPropertySource} 实例。
     *
     * @param name   属性源的名称
     * @param encode 如果输出应该被编码
     */
    public AnsiPropertySource(String name, boolean encode) {
        super(name);
        this.encode = encode;
    }

    @Override
    public Object getProperty(String name) {
        if (StringUtils.hasLength(name)) {
            for (Mapping mapping : MAPPINGS) {
                String prefix = mapping.getPrefix();
                if (name.startsWith(prefix)) {
                    String postfix = name.substring(prefix.length());
                    AnsiElement element = mapping.getElement(postfix);
                    if (element != null) {
                        return (this.encode) ? AnsiOutput.encode(element) : element;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 名称和伪属性源之间的映射
     */
    private abstract static class Mapping {
        private final String prefix;

        Mapping(String prefix) {
            this.prefix = prefix;
        }

        String getPrefix() {
            return this.prefix;
        }

        abstract AnsiElement getElement(String postfix);
    }

    /**
     * {@link AnsiElement} 枚举的 {@link Mapping}
     */
    private static class EnumMapping<E extends Enum<E> & AnsiElement> extends Mapping {
        private final Set<E> enums;

        EnumMapping(String prefix, Class<E> enumType) {
            super(prefix);
            this.enums = EnumSet.allOf(enumType);
        }

        @Override
        AnsiElement getElement(String postfix) {
            for (Enum<?> candidate : this.enums) {
                if (candidate.name().equals(postfix)) {
                    return (AnsiElement) candidate;
                }
            }
            return null;
        }
    }

    /**
     * {@link Ansi8BitColor} 的 {@link Mapping}
     */
    private static class Ansi8BitColorMapping extends Mapping {
        private final IntFunction<Ansi8BitColor> factory;

        Ansi8BitColorMapping(String prefix, IntFunction<Ansi8BitColor> factory) {
            super(prefix);
            this.factory = factory;
        }

        @Override
        AnsiElement getElement(String postfix) {
            if (containsOnlyDigits(postfix)) {
                try {
                    return this.factory.apply(Integer.parseInt(postfix));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return null;
        }

        private boolean containsOnlyDigits(String postfix) {
            for (int i = 0; i < postfix.length(); i++) {
                if (!Character.isDigit(postfix.charAt(i))) {
                    return false;
                }
            }
            return !postfix.isEmpty();
        }
    }
}
