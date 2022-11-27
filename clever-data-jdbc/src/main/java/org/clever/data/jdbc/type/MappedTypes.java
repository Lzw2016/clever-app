package org.clever.data.jdbc.type;

import java.lang.annotation.*;

/**
 * The annotation that specify java types to map {@link TypeHandler}.
 *
 * <p>
 * <b>How to use:</b>
 * <pre>
 * &#064;MappedTypes(String.class)
 * public class StringTrimmingTypeHandler implements TypeHandler&lt;String&gt; {
 *   // ...
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedTypes {
    /**
     * Returns java types to map {@link TypeHandler}.
     *
     * @return java types
     */
    Class<?>[] value();
}
