package io.github.lzpeng.compiler.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

/**
 *
 * @author lzpeng723
 */
public final class UriUtil {

    /**
     * 获得URL，常用于使用绝对路径时的情况
     *
     * @param url URL对象
     * @return URI
     * @throws IllegalArgumentException URL格式错误
     */
    public static URI getURI(final URL url) {
        Objects.requireNonNull(url, "url is null !");
        try {
            return url.toURI();
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Error occurred when get URI!", e);
        }
    }

    /**
     * 获取string协议的URI，类似于string:///xxxxx
     *
     * @param content 正文
     * @return URL
     */
    public static URI getStringURI(final CharSequence content) {
        if (null == content) {
            return null;
        }
        final String contentStr = String.valueOf(content);
        if (contentStr.startsWith("string:///")) {
            return URI.create(contentStr);
        }
        return URI.create("string:///" + contentStr);
    }

    /**
     * 将给定的URI转换为File对象。
     *
     * @param uri 要转换的URI对象
     * @return 与URI对应的File对象，如果无法转换则返回null
     */
    public static File toFile(URI uri) {
        return new File(uri);
    }

}
