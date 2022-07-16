package org.clever.boot.origin;

import java.net.URI;

/**
 * 理解Jar URL的简单类可以提供简短的描述。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/03 17:57 <br/>
 */
final class JarUri {
    private static final String JAR_SCHEME = "jar:";
    private static final String JAR_EXTENSION = ".jar";

    private final String uri;
    private final String description;

    private JarUri(String uri) {
        this.uri = uri;
        this.description = extractDescription(uri);
    }

    private String extractDescription(String uri) {
        uri = uri.substring(JAR_SCHEME.length());
        int firstDotJar = uri.indexOf(JAR_EXTENSION);
        String firstJar = getFilename(uri.substring(0, firstDotJar + JAR_EXTENSION.length()));
        uri = uri.substring(firstDotJar + JAR_EXTENSION.length());
        int lastDotJar = uri.lastIndexOf(JAR_EXTENSION);
        if (lastDotJar == -1) {
            return firstJar;
        }
        return firstJar + uri.substring(0, lastDotJar + JAR_EXTENSION.length());
    }

    private String getFilename(String string) {
        int lastSlash = string.lastIndexOf('/');
        return (lastSlash == -1) ? string : string.substring(lastSlash + 1);
    }

    String getDescription() {
        return this.description;
    }

    String getDescription(String existing) {
        return existing + " from " + this.description;
    }

    @Override
    public String toString() {
        return this.uri;
    }

    static JarUri from(URI uri) {
        return from(uri.toString());
    }

    static JarUri from(String uri) {
        if (uri.startsWith(JAR_SCHEME) && uri.contains(JAR_EXTENSION)) {
            return new JarUri(uri);
        }
        return null;
    }
}
