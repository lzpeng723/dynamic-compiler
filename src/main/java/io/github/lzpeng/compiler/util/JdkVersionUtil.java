package io.github.lzpeng.compiler.util;

import java.net.URLClassLoader;

/**
 * JDK版本判断工具类（兼容Java8+，适配动态编译、类加载器隔离等所有场景）
 */
public class JdkVersionUtil {
    // 缓存JDK主版本号，避免重复解析
    private static Integer JDK_MAIN_VERSION = null;

    /**
     * 获取JDK核心主版本号（8/9/11/21），缓存结果，仅解析一次
     *
     * @return JDK主版本号
     */
    public static int getJdkMainVersion() {
        if (JDK_MAIN_VERSION != null) {
            return JDK_MAIN_VERSION;
        }
        // 优先使用Java9+原生API，异常则降级为系统属性解析
        try {
            final Object versionObj = ReflectUtil.invokeStaticMethod(Runtime.class, "version");
            // 调用Version.major()获取主版本号
            JDK_MAIN_VERSION = ReflectUtil.invokeMethod(versionObj, "major");
        } catch (Exception e) {
            // 降级为java.version系统属性解析（Java8及以下）
            String jdkVersion = System.getProperty("java.version");
            if (jdkVersion.startsWith("1.")) {
                JDK_MAIN_VERSION = Integer.parseInt(jdkVersion.split("\\.")[1]);
            } else {
                JDK_MAIN_VERSION = Integer.parseInt(jdkVersion.split("[^0-9]")[0]);
            }
        }
        return JDK_MAIN_VERSION;
    }

    /**
     * 判断是否为Java8环境
     */
    public static boolean isJdk8() {
        return getJdkMainVersion() == 8;
    }

    /**
     * 判断是否为Java9+环境（无tools.jar、支持模块化）
     */
    public static boolean isJdk9Plus() {
        return getJdkMainVersion() >= 9;
    }

    /**
     * 判断是否为Java11+长期支持版本
     */
    public static boolean isJdk11Plus() {
        return getJdkMainVersion() >= 11;
    }

    /**
     * 判断URLClassLoader是否有内置close()方法（Java9+有，Java8无）
     */
    public static boolean isURLClassLoaderSupportClose() {
        try {
            URLClassLoader.class.getMethod("close");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * 判断是否需要加载tools.jar（仅Java8需要，Java9+无此文件）
     */
    public static boolean needLoadToolsJar() {
        return isJdk8();
    }

    // 测试主方法
    public static void main(String[] args) {
        System.out.println("当前JDK主版本号：" + getJdkMainVersion());
        System.out.println("是否为Java8：" + isJdk8());
        System.out.println("是否为Java9+：" + isJdk9Plus());
        System.out.println("是否需要加载tools.jar：" + needLoadToolsJar());
        System.out.println("URLClassLoader是否支持close：" + isURLClassLoaderSupportClose());
    }
}