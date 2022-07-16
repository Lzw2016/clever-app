package org.clever.boot.system;

import org.clever.util.ClassUtils;
import org.clever.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 提供对应用程序主目录的访问。
 * 尝试为Jar文件、Exploded Archives和直接运行的应用程序选择一个合理的位置。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/16 22:27 <br/>
 */
public class ApplicationHome {
    private final File source;
    private final File dir;

    /**
     * 创建一个新的 {@link ApplicationHome} 实例。
     */
    public ApplicationHome() {
        this(null);
    }

    /**
     * 为指定的源类创建一个新的 {@link ApplicationHome} 实例。
     *
     * @param sourceClass the source class or null
     */
    public ApplicationHome(Class<?> sourceClass) {
        this.source = findSource((sourceClass != null) ? sourceClass : getStartClass());
        this.dir = findHomeDir(this.source);
    }

    private Class<?> getStartClass() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            return getStartClass(classLoader.getResources("META-INF/MANIFEST.MF"));
        } catch (Exception ex) {
            return null;
        }
    }

    private Class<?> getStartClass(Enumeration<URL> manifestResources) {
        while (manifestResources.hasMoreElements()) {
            try (InputStream inputStream = manifestResources.nextElement().openStream()) {
                Manifest manifest = new Manifest(inputStream);
                String startClass = manifest.getMainAttributes().getValue("Start-Class");
                if (startClass != null) {
                    return ClassUtils.forName(startClass, getClass().getClassLoader());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private File findSource(Class<?> sourceClass) {
        try {
            ProtectionDomain domain = (sourceClass != null) ? sourceClass.getProtectionDomain() : null;
            CodeSource codeSource = (domain != null) ? domain.getCodeSource() : null;
            URL location = (codeSource != null) ? codeSource.getLocation() : null;
            File source = (location != null) ? findSource(location) : null;
            if (source != null && source.exists() && !isUnitTest()) {
                return source.getAbsoluteFile();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isUnitTest() {
        try {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = stackTrace.length - 1; i >= 0; i--) {
                if (stackTrace[i].getClassName().startsWith("org.junit.")) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private File findSource(URL location) throws IOException, URISyntaxException {
        URLConnection connection = location.openConnection();
        if (connection instanceof JarURLConnection) {
            return getRootJarFile(((JarURLConnection) connection).getJarFile());
        }
        return new File(location.toURI());
    }

    private File getRootJarFile(JarFile jarFile) {
        String name = jarFile.getName();
        int separator = name.indexOf("!/");
        if (separator > 0) {
            name = name.substring(0, separator);
        }
        return new File(name);
    }

    private File findHomeDir(File source) {
        File homeDir = source;
        homeDir = (homeDir != null) ? homeDir : findDefaultHomeDir();
        if (homeDir.isFile()) {
            homeDir = homeDir.getParentFile();
        }
        homeDir = homeDir.exists() ? homeDir : new File(".");
        return homeDir.getAbsoluteFile();
    }

    private File findDefaultHomeDir() {
        String userDir = System.getProperty("user.dir");
        return new File(StringUtils.hasLength(userDir) ? userDir : ".");
    }

    /**
     * 返回用于查找主目录的底层源。
     * 这通常是 jar 文件或目录。
     * 如果无法确定来源，则可以返回 null。
     *
     * @return 基础来源或 null
     */
    public File getSource() {
        return this.source;
    }

    /**
     * 返回应用程序主目录。
     *
     * @return 主目录（从不为空）
     */
    public File getDir() {
        return this.dir;
    }

    @Override
    public String toString() {
        return getDir().toString();
    }
}
