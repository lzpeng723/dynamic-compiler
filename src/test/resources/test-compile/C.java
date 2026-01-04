/**
 * @author Lzpeng723
 * @description $END$
 * @since 2025/12/11 16:39
 */
public record C(ClassLoader classLoader) {

    public C(){
        this(C.class.getClassLoader());
    }

    public C {
        new B(classLoader);
    }
}
