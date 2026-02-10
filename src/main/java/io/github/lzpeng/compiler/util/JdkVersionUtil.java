package io.github.lzpeng.compiler.util;

/**
 * JDK版本判断工具类（兼容Java8+，适配动态编译、类加载器隔离等所有场景）
 *
 * @author lzpeng
 * @since 1.0.0-M5
 */
public class JdkVersionUtil {


    /**
     * 缓存JDK版本号，避免重复解析
     */
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
     * 判断当前JDK主版本号是否等于指定版本。
     *
     * @param version 指定的JDK版本号
     * @return 如果当前JDK主版本号等于指定版本，返回true；否则返回false
     */
    public static boolean isJdk(int version) {
        return getJdkMainVersion() == version;
    }

    /**
     * 判断当前JDK主版本号是否大于或等于指定版本。
     *
     * @param version 指定的JDK版本号
     * @return 如果当前JDK主版本号大于或等于指定版本，返回true；否则返回false
     */
    public static boolean isJdkPlus(int version) {
        return getJdkMainVersion() >= version;
    }

    /**
     * 判断当前JDK主版本号是否小于或等于指定版本。
     *
     * @param version 指定的JDK版本号
     * @return 如果当前JDK主版本号小于或等于指定版本，返回true；否则返回false
     */
    public static boolean isJdkMinus(int version) {
        return getJdkMainVersion() <= version;
    }


}