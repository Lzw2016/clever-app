package org.clever.boot.origin;

import org.clever.core.io.ClassPathResource;
import org.clever.core.io.Resource;
import org.clever.util.ObjectUtils;

import java.io.IOException;

/**
 * {@link Origin}用于从文本资源加载的项目。
 * 提供对加载文本的原始{@link Resource}和其中的{@link Location}的访问。
 * 如果提供的资源提供了{@link Origin}（例如，它是一个{@link OriginTrackedResource}），那么它将被用作{@link Origin#getParent() 源父资源}。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:55 <br/>
 *
 * @see OriginTrackedResource
 */
public class TextResourceOrigin implements Origin {
    private final Resource resource;
    private final Location location;

    public TextResourceOrigin(Resource resource, Location location) {
        this.resource = resource;
        this.location = location;
    }

    /**
     * 返回属性起源的资源。
     *
     * @return 文本资源或 {@code null}
     */
    public Resource getResource() {
        return this.resource;
    }

    /**
     * 返回属性在源中的位置（如果已知）。
     *
     * @return 位置或 {@code null}
     */
    public Location getLocation() {
        return this.location;
    }

    @Override
    public Origin getParent() {
        return Origin.from(this.resource);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof TextResourceOrigin) {
            TextResourceOrigin other = (TextResourceOrigin) obj;
            boolean result;
            result = ObjectUtils.nullSafeEquals(this.resource, other.resource);
            result = result && ObjectUtils.nullSafeEquals(this.location, other.location);
            return result;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.resource);
        result = 31 * result + ObjectUtils.nullSafeHashCode(this.location);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getResourceDescription(this.resource));
        if (this.location != null) {
            result.append(" - ").append(this.location);
        }
        return result.toString();
    }

    private String getResourceDescription(Resource resource) {
        if (resource instanceof OriginTrackedResource) {
            return getResourceDescription(((OriginTrackedResource) resource).getResource());
        }
        if (resource == null) {
            return "unknown resource [?]";
        }
        if (resource instanceof ClassPathResource) {
            return getResourceDescription((ClassPathResource) resource);
        }
        return resource.getDescription();
    }

    private String getResourceDescription(ClassPathResource resource) {
        try {
            JarUri jarUri = JarUri.from(resource.getURI());
            if (jarUri != null) {
                return jarUri.getDescription(resource.getDescription());
            }
        } catch (IOException ignored) {
        }
        return resource.getDescription();
    }

    /**
     * 资源中的位置（行号和列号）。
     */
    public static final class Location {
        private final int line;
        private final int column;

        /**
         * 创建新的 {@link Location}
         *
         * @param line   行号（零索引）
         * @param column 列号（零索引）
         */
        public Location(int line, int column) {
            this.line = line;
            this.column = column;
        }

        /**
         * 返回属性起源的文本资源行。
         *
         * @return 行号（零索引）
         */
        public int getLine() {
            return this.line;
        }

        /**
         * 返回属性起源的文本资源的列。
         *
         * @return 列号（零索引）
         */
        public int getColumn() {
            return this.column;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Location other = (Location) obj;
            boolean result;
            result = this.line == other.line;
            result = result && this.column == other.column;
            return result;
        }

        @Override
        public int hashCode() {
            return (31 * this.line) + this.column;
        }

        @Override
        public String toString() {
            return (this.line + 1) + ":" + (this.column + 1);
        }
    }
}
