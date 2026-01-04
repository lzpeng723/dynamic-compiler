package io.github.lzpeng.compiler.resource;

import io.github.lzpeng.compiler.util.IoUtil;
import io.github.lzpeng.compiler.util.UrlUtil;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * 定义了资源的基本操作接口。实现了该接口的类可以表示各种类型的资源，如文件、字符串等。
 * 提供了获取资源名称、URI、URL、输入流以及读取资源内容的方法。
 */
public interface Resource {

    /**
     * 获取资源的名称。
     *
     * @return 资源的名称
     */
    String getName();

    /**
     * 获得解析后的{@link URL}，无对应URL的返回{@code null}
     *
     * @return 解析后的{@link URL}
     */
    default URI getUri() {
        throw new UnsupportedOperationException();
    }

    /**
     * 获得解析后的{@link URL}，无对应URL的返回{@code null}
     *
     * @return 解析后的{@link URL}
     */
    default URL getUrl() {
        return UrlUtil.getURL(this.getUri());
    }


    /**
     * 获取资源的输入流。
     *
     * @return 资源的输入流
     */
    default InputStream getInputStream() {
        try {
            return this.getUrl().openStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 将当前资源的URL转换为对应的File对象。
     *
     * @return 与资源URL对应的File对象，如果无法转换或资源无对应URL则返回null
     */
    default File getFile() {
        return UrlUtil.toFile(getUrl());
    }

    /**
     * 获取资源对应的路径。
     *
     * @return 资源的路径，如果无法转换或资源无对应URL则返回null
     */
    default Path getPath() {
        return getFile().toPath();
    }

    /**
     * 从资源中读取字节数据。
     *
     * @return 资源的字节数组
     */
    default byte[] readBytes() {
        try {
            return IoUtil.readAllBytes(getInputStream());
        } catch (IOException e) {
            return new byte[0];
        }
    }

    /**
     * 读取资源内容，读取完毕后会关闭流<br>
     * 关闭流并不影响下一次读取
     *
     * @return 读取资源内容
     */
    default String readUtf8Str() {
        return readStr(StandardCharsets.UTF_8);
    }

    /**
     * 读取资源内容，读取完毕后会关闭流<br>
     * 关闭流并不影响下一次读取
     *
     * @param charset 编码
     * @return 读取资源内容
     */
    default String readStr(Charset charset) {
        try (final BufferedReader reader = getReader(charset)) {
            return reader.lines().collect(Collectors.joining());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获得Reader
     *
     * @param charset 编码
     * @return {@link BufferedReader}
     */
    default BufferedReader getReader(Charset charset) {
        final InputStream in = this.getInputStream();
        if (null == in) {
            return null;
        }
        InputStreamReader reader;
        if (null == charset) {
            reader = new InputStreamReader(in);
        } else {
            reader = new InputStreamReader(in, charset);
        }
        return new BufferedReader(reader);
    }

    /**
     * 将资源内容写出到流，不关闭输出流，但是关闭资源流
     *
     * @param out 输出流
     */
    default void writeTo(OutputStream out) {
        try (InputStream in = getInputStream()) {
            //out.write(IoUtil.readAllBytes(in));
            in.transferTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
