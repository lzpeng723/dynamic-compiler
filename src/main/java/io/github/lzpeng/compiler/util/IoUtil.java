package io.github.lzpeng.compiler.util;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author lzpeng723
 */
public final class IoUtil {

    /**
     * 从输入流中读取所有字节并返回一个包含这些字节的数组。
     *
     * @param inputStream 输入流，从中读取所有字节
     * @return 包含从输入流中读取的所有字节的数组
     * @throws IOException 如果在读取过程中发生I/O错误
     */
    public static byte[] readAllBytes(InputStream inputStream) throws IOException {
        // jdk 8
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        byte[] buffer = new byte[1024];
//        int n;
//        while ((n = inputStream.read(buffer)) != -1) {
//            outputStream.write(buffer, 0, n);
//        }
//        return outputStream.toByteArray();
        // jdk 17
        return inputStream.readAllBytes();
    }

}
