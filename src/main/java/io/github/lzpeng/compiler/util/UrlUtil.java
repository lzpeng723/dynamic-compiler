package io.github.lzpeng.compiler.util;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 *
 * @author lzpeng723
 * @since 1.0.0-M1
 */
public final class UrlUtil {

    /**
     * 文件缓存映射，用于存储文件路径与对应JarFile对象的映射关系。
     * 该缓存由UrlUtil工具类提供，用于提高文件访问效率。
     */
    private static final Map<String, JarFile> fileCache = UrlUtil.getFileCache();

    /**
     * URL缓存映射，用于存储JarFile对象与对应URL的映射关系。
     * 该缓存由UrlUtil工具类提供，用于快速获取JarFile对应的资源URL。
     */
    private static final Map<JarFile, URL> urlCache = UrlUtil.getUrlCache();

    /**
     * 临时文件缓存映射，用于存储临时文件路径与对应的File对象的映射关系。
     * 该缓存由UrlUtil工具类提供，用于提高临时文件访问效率。
     */
    private static final Map<String, File> tempFileCache = new HashMap<>();


    /**
     * 获得URL，常用于使用绝对路径时的情况
     *
     * @param uri URL对应的URI对象
     * @return URL
     * @throws IllegalArgumentException URL格式错误
     */
    public static URL getURL(final URI uri) {
        Objects.requireNonNull(uri, "uri is null !");
        try {
            return uri.toURL();
        } catch (final MalformedURLException e) {
            throw new IllegalArgumentException("Error occurred when get URL!", e);
        }
    }


    /**
     * 获得URL，常用于使用绝对路径时的情况
     *
     * @param file URL对应的文件对象
     * @return URL
     * @throws IllegalArgumentException URL格式错误
     */
    public static URL getURL(final File file) {
        Objects.requireNonNull(file, "File is null !");
        return getURL(file.toURI());
    }

    /**
     * 获得URL，常用于使用绝对路径时的情况
     *
     * @param path URL对应的文件对象
     * @return URL
     * @throws IllegalArgumentException URL格式错误
     */
    public static URL getURL(final Path path) {
        Objects.requireNonNull(path, "path is null !");
        return getURL(path.toUri());
    }

    /**
     * 获取string协议的URI，类似于string:///xxxxx
     *
     * @param content 正文
     * @return URL
     */
    public static URL getStringURL(final CharSequence content) {
        return getURL(UriUtil.getStringURI(content));
    }

    /**
     * 将给定的URL转换为File对象。
     *
     * @param url 要转换的URL对象
     * @return 与URL对应的File对象，如果无法转换则返回null
     */
    public static File toFile(URL url) {
        try {
            final String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                return new File(UriUtil.getURI(url));
            }
            final URLConnection urlConnection = UrlUtil.toJarUrl(url).openConnection();
            if (urlConnection instanceof JarURLConnection) {
                final JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
                final JarFile jarFile = jarURLConnection.getJarFile();
                if (JdkVersionUtil.isJdkPlus(9)) {
                    return new File(jarFile.getName());
                }
                return tempFileCache.computeIfAbsent(jarFile.getName(), path -> {
                    try {
                        final File tempJarFile = Files.createTempFile("dynamic-compiler", ".jar").toFile();
                        Files.copy(Paths.get(path), tempJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        tempJarFile.deleteOnExit();
                        return tempJarFile;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            throw new UnsupportedOperationException("不支持的协议: " + protocol);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将给定的URL转换为JAR URL格式。
     *
     * @param url 要转换的URL对象
     * @return 如果给定的URL已经是JAR协议，则直接返回该URL；否则，尝试将其转换为JAR URL格式后返回。如果转换过程中发生异常，则原样返回初始URL。
     */
    public static URL toJarUrl(URL url) {
        final String protocol = url.getProtocol();
        if ("jar".equals(protocol)) {
            return url;
        }
        try {
            return new URL("jar:" + url + "!/");
        } catch (MalformedURLException e) {
            return url;
        }
    }

    /**
     * 根据给定的依赖字符串获取URL。
     * 如果依赖字符串是一个存在的文件路径，则尝试将其转换为对应的URL；
     * 否则，直接将依赖字符串作为URL地址来创建URL对象。
     *
     * @param str 依赖字符串，可以是文件路径或URL地址
     * @return 对应的URL对象
     * @throws RuntimeException 如果提供的依赖字符串既不是有效的文件路径也不是有效的URL地址
     */
    public static URL getURL(String str) {
        try {
            return new URL(str);
        } catch (MalformedURLException e) {
            if (str.startsWith("http") || str.startsWith("jar") || str.startsWith("jfr")) {
                return getURL(URI.create(str));
            }
            final Path path = Paths.get(str);
            if (Files.exists(path)) {
                return getURL(path);
            }
            throw new UnsupportedOperationException("不支持的字符串: " + str);
        }
    }

    /**
     * 获取JarFileFactory类中的fileCache字段值。
     * 该字段是一个Map，键为字符串（可能是文件路径），值为JarFile对象。
     * 此方法通过反射机制访问私有静态字段fileCache。
     *
     * @return 返回一个Map<String, JarFile>类型的对象，表示文件缓存。
     */
    public static Map<String, JarFile> getFileCache() {
        if (JdkVersionUtil.isJdkMinus(8)) {
            try {
                // 加载JarFileFactory类
                final Class<?> clazz = ReflectUtil.loadClass("sun.net.www.protocol.jar.JarFileFactory");
                // 通过反射获取fileCache字段的值并返回
                return ReflectUtil.getFieldValue(clazz, "fileCache");
            } catch (ClassNotFoundException ignored) {
            }
        }
        return Collections.emptyMap();
    }

    /**
     * 获取JarFileFactory类中的urlCache字段值。
     * 该字段是一个Map，键为JarFile对象，值为URL对象。
     * 此方法通过反射机制访问私有静态字段urlCache。
     *
     * @return 返回一个Map<JarFile, URL>类型的对象，表示URL缓存。
     */
    public static Map<JarFile, URL> getUrlCache() {
        if (JdkVersionUtil.isJdkMinus(8)) {
            try {
                // 加载JarFileFactory类
                final Class<?> clazz = ReflectUtil.loadClass("sun.net.www.protocol.jar.JarFileFactory");
                // 通过反射获取urlCache字段的值并返回
                return ReflectUtil.getFieldValue(clazz, "urlCache");
            } catch (ClassNotFoundException ignored) {
            }
        }
        return Collections.emptyMap();
    }


    /**
     * 获取文件URL缓存映射。
     * <p>
     * 该方法遍历全局的URL缓存（UrlUtil.urlCache），将其中的Jar文件名作为键，
     * 对应的URL作为值，构建一个新的HashMap并返回。
     *
     * @return 返回一个Map，其中键为Jar文件名（String类型），值为对应的URL对象。
     */
    public static Map<String, URL> getFileUrlCache() {
        // 创建一个新的HashMap用于存储文件名到URL的映射
        final Map<String, URL> fileUrlCache = new HashMap<>();
        // 遍历全局URL缓存，将Jar文件名和URL放入新的Map中
        UrlUtil.urlCache.forEach((jarFile, url) -> fileUrlCache.put(jarFile.getName(), url));
        // 返回构建好的文件URL缓存映射
        return fileUrlCache;
    }

    /**
     * 从文件URL缓存中获取指定文件对应的URL。
     *
     * @param file 要查询的文件对象，不能为null
     * @return 返回与文件绝对路径关联的URL；如果缓存中不存在该文件的URL，则返回null
     */
    public static URL getUrlFromFileCache(File file) {
        // 获取全局文件URL缓存映射表
        final Map<String, URL> fileUrlCache = UrlUtil.getFileUrlCache();
        // 根据文件的绝对路径从缓存中查找对应的URL
        final URL url = fileUrlCache.get(file.getAbsolutePath());
        // 返回查找到的URL（可能为null）
        return url;
    }

}
