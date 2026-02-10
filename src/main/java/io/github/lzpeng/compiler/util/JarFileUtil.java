package io.github.lzpeng.compiler.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author lzpeng723
 * @since 1.0.0-M5
 */
public class JarFileUtil {

    /**
     *
     */
    private static final Map<File, Collection<File>> fileCache = new WeakHashMap<>();

    /**
     * 根据给定的文件，生成一个包含该文件及其依赖的类路径文件的流。
     * 如果文件不存在或无法读取，则返回空流。
     * 如果文件是JAR文件且包含MANIFEST.MF，则解析其中的Class-Path属性，
     * 并递归地将依赖的文件加入到结果流中。
     *
     * @param file 输入的文件对象，可以是普通文件、目录或JAR文件
     * @return 包含文件及其依赖文件的流
     * @see com.sun.tools.javac.file.FSInfo#getJarClassPath(java.io.File)
     */
    public static Collection<File> list(File file) {
        return JarFileUtil.fileCache.computeIfAbsent(file, __ -> {
            // 如果文件为空或不存在，直接返回空流
            if (file == null || !file.exists()) {
                return Collections.emptySet();
            }
            // 获取文件的MANIFEST信息
            final Manifest manifest = JarFileUtil.getManifest(file);
            if (manifest == null) {
                // 如果没有MANIFEST信息，仅返回当前文件的流
                return Collections.singleton(file);
            }

            // 初始化结果流列表，并将当前文件加入
            final List<File> fileList = new ArrayList<>();
            fileList.add(file);
            // 解析MANIFEST中的Class-Path属性
            final String classPathAttr = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
            if (classPathAttr != null && !classPathAttr.isEmpty()) {
                // 按空格分割Class-Path字符串
                final String[] classPathStrs = classPathAttr.split(" +");
                for (String classPathStr : classPathStrs) {
                    File cpFile;
                    // 处理以"file:"开头的URL路径
                    if (classPathStr.startsWith("file:")) {
                        final URL url = UrlUtil.getURL(classPathStr);
                        cpFile = UrlUtil.toFile(url);
                    } else {
                        // 处理相对路径或绝对路径
                        cpFile = new File(classPathStr);
                        if (!cpFile.isAbsolute()) {
                            cpFile = new File(file.getParentFile(), classPathStr);
                        }
                    }
                    // 递归获取依赖文件的流并加入结果列表
                    fileList.addAll(JarFileUtil.list(cpFile));
                }
            }
            return fileList;
        });
    }


    /**
     * 从指定文件中提取MANIFEST信息。
     * 支持从普通目录中的MANIFEST.MF文件或JAR包内的MANIFEST.MF文件中读取。
     *
     * @param file 输入的文件对象，可以是普通目录或JAR文件
     * @return MANIFEST对象，如果无法读取则返回null
     */
    private static Manifest getManifest(File file) {
        try {
            // 如果文件不存在，直接返回null
            if (!file.exists()) {
                return null;
            }

            // 如果是目录，尝试读取其中的MANIFEST.MF文件
            if (file.isDirectory()) {
                File manifestFile = new File(file, JarFile.MANIFEST_NAME);
                if (manifestFile.exists()) {
                    return new Manifest(Files.newInputStream(manifestFile.toPath()));
                }
            }
            // 如果是JAR文件，尝试从中读取MANIFEST
            else if (file.getName().endsWith(".jar")) {
                try (JarFile jarFile = new JarFile(file)) {
                    return jarFile.getManifest();
                } catch (IOException ignore) {
                    // 忽略IO异常
                }
            }

            // 其他情况返回null
            return null;
        } catch (IOException e) {
            // 发生异常时返回null
            return null;
        }
    }
}
