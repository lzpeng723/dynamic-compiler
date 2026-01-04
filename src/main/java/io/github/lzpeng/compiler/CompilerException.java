package io.github.lzpeng.compiler;


import java.io.Serial;

/**
 * 编译异常
 *
 * @author lzpeng723
 * @since 1.0.0-M1
 */
public final class CompilerException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;


    /**
     * 构造一个新的CompilerException实例，使用给定的消息。
     *
     * @param message 详细描述异常原因的字符串
     */
    public CompilerException(String message) {
        super(message);
    }

    /**
     * 构造一个新的CompilerException实例，使用给定的消息模板和参数。
     *
     * @param messageTemplate 消息模板字符串
     * @param params          用于填充消息模板的参数
     */
    public CompilerException(String messageTemplate, Object... params) {
        super(String.format(messageTemplate, params));
    }

    /**
     * 构造一个新的CompilerException实例，使用给定的消息和异常原因。
     *
     * @param message   详细描述异常原因的字符串
     * @param throwable 引发此异常的底层原因
     */
    public CompilerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * 构造一个新的CompilerException实例，使用给定的异常原因、消息模板和参数。
     *
     * @param throwable       引发此异常的底层原因
     * @param messageTemplate 消息模板字符串
     * @param params          用于填充消息模板的参数
     */
    public CompilerException(Throwable throwable, String messageTemplate, Object... params) {
        super(String.format(messageTemplate, params), throwable);
    }
}
