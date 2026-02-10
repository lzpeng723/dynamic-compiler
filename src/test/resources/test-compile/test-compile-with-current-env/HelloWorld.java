package test;

import com.squareup.javapoet.JavaFile;
import io.github.lzpeng.compiler.util.CompilerUtil;

public class HelloWorld {
    public static void hello() {
        System.out.println("Compiler is " + CompilerUtil.create());
        System.out.println("com.squareup.javapoet.JavaFile in " + JavaFile.class.getProtectionDomain().getCodeSource().getLocation());
    }
}