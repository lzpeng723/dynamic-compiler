package io.github.lzpeng.compiler.util;

import java.util.Date;
import java.util.function.Supplier;

/**
 * 工具类，用于执行耗时操作并打印相关信息。
 *
 * @author lzpeng723
 * @since  1.0.0-M5
 */
public class PrintUtil {

    /**
     * 执行给定的 Supplier 操作，并打印其启动时间、结束时间和总耗时。
     *
     * <p>该方法会记录操作开始和结束的时间戳，并计算操作的总耗时（以秒为单位），
     * 同时将这些信息输出到控制台。</p>
     *
     * @param supplier 需要执行的操作，类型为 {@link Supplier}
     * @param <T>      操作返回值的类型
     * @return 执行操作后返回的结果
     */
    public static <T> T timeout(Supplier<T> supplier) {
        // 记录操作启动时间
        final long currentTimeMillis = System.currentTimeMillis();
        System.out.println(supplier + " 启动时间 " + new Date(currentTimeMillis));

        // 执行操作并获取结果
        final T t = supplier.get();

        // 记录操作结束时间并计算耗时
        final long endTimeMillis = System.currentTimeMillis();
        System.out.println(supplier + " 结束时间 " + new Date(endTimeMillis));
        final String message = supplier + "  耗时" + (endTimeMillis - currentTimeMillis) / 1000.0 + "s";
        System.out.println(message);

        // 返回操作结果
        return t;
    }

}
