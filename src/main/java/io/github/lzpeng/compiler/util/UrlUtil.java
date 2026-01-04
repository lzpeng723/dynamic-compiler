package io.github.lzpeng.compiler.util;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 *
 * @author lzpeng723
 * @since 1.0.0-M1
 */
public final class UrlUtil {

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
                return new File(jarFile.getName());
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
