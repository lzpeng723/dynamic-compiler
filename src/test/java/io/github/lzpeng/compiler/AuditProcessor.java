package io.github.lzpeng.compiler;

import com.squareup.javapoet.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

//@AutoService(Processor.class)
@SupportedAnnotationTypes(AuditProcessor.WITH_AUDIT)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class AuditProcessor extends AbstractProcessor {

    /**
     * 常量字符串，表示需要审计的类名。此常量用于在编译时识别哪些类应当被添加审计相关的字段和方法。
     * 该值指定了一个测试用例中的类名"test.WithAudit"，在实际应用中应替换为具体的类名或作为配置项使用。
     */
    static final String WITH_AUDIT = "test.WithAudit";

    /**
     * 定义用于表示字符串类型的TypeName常量，主要用于在生成源代码时引用字符串类型。
     * 使用JDK8原生的ClassName工具类获取String类的类型名称。
     */
    private static final TypeName STRING_TYPE = ClassName.get(String.class);
    /**
     * 表示LocalDateTime类型的TypeName对象，用于在代码生成过程中引用LocalDateTime类型。
     * 该变量是静态且不可变的，确保了在整个应用中对LocalDateTime类型的引用一致性。
     */
    private static final TypeName LOCAL_DATE_TIME_TYPE = ClassName.get(LocalDateTime.class);
    /**
     * 表示用于生成toString方法时所需的StringBuilder类型。
     * 该变量定义为静态不可变的TypeName实例，专用于在处理审计字段和生成toString方法时指定类型。
     */
    private static final TypeName STRING_BUILDER_TYPE = ClassName.get(StringBuilder.class);
    /**
     * 代表{@code Override}注解的类名对象，用于在代码生成过程中引用该注解。
     * 此常量通过ClassName工具类获取，确保了类型安全和正确的全限定名使用。
     */
    private static final ClassName OVERRIDE_ANNOTATION = ClassName.get(Override.class);

    /**
     * 处理标注了特定注解的类型元素，为这些元素生成包含审计字段和toString方法的新类。
     *
     * @param annotations 当前轮次中被处理的注解集合
     * @param roundEnv    用于获取当前轮次中所有被指定注解标记的元素
     * @return 总是返回true，表示继续处理后续的注解处理器
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        final Elements elementUtils = this.processingEnv.getElementUtils();
        final Set<String> supportedAnnotationTypeSet = this.getSupportedAnnotationTypes();
        for (String supportedAnnotationType : supportedAnnotationTypeSet) {
            if (AuditProcessor.WITH_AUDIT.equals(supportedAnnotationType)) {
                final TypeElement typeElement = elementUtils.getTypeElement(supportedAnnotationType);
                for (TypeElement classElement : ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(typeElement))) {
                    try {
                        processClass(classElement);
                    } catch (IOException e) {
                        processingEnv.getMessager().printMessage(
                                javax.tools.Diagnostic.Kind.ERROR,
                                "生成审计字段和toString失败：" + e.getMessage(),
                                classElement
                        );
                    }
                }
            }
        }
        return true;
    }

    /**
     * 处理给定的类型元素，为其生成一个新的包含审计字段和toString方法的类。
     * 该方法执行以下步骤：
     * <ol>
     *     <li>从给定的类型元素中提取基础信息，如类名、包名等。</li>
     *     <li>基于这些信息构建一个新的类结构，其中包含了原始类对象的引用以及额外的审计相关字段（创建人、创建时间、更新人、更新时间）。</li>
     *     <li>为新类中的每个字段添加getter和setter方法。</li>
     *     <li>为新类添加一个构造器，用于初始化创建时间字段为当前日期。</li>
     *     <li>最后，将这个新类写入到指定的文件中，并打印一条消息通知用户。</li>
     * </ol>
     *
     * @param classElement 需要处理以生成新类的类型元素
     * @throws IOException 如果在尝试写入新生成的Java文件时遇到问题，则抛出此异常
     */
    private void processClass(TypeElement classElement) throws IOException {
        // 1. 获取类基础信息
        String className = classElement.getSimpleName() + "Audit";
        String packageName = processingEnv.getElementUtils()
                .getPackageOf(classElement).getQualifiedName().toString();
        final ClassName dataClassName = ClassName.get(classElement);
        // 2. 构建类结构
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                // 保留原有类对象
                .addField(createAuditField("data", dataClassName, "数据"))
                // 添加审计字段
                .addField(createAuditField("createBy", STRING_TYPE, "创建人"))
                .addField(createAuditField("createTime", LOCAL_DATE_TIME_TYPE, "创建时间"))
                .addField(createAuditField("updateBy", STRING_TYPE, "更新人"))
                .addField(createAuditField("updateTime", LOCAL_DATE_TIME_TYPE, "更新时间"))
                // 生成原有类对象的getter/setter
                .addMethod(createGetter("data", dataClassName))
                .addMethod(createSetter("data", dataClassName))
                // 生成审计字段的getter/setter
                .addMethod(createGetter("createBy", STRING_TYPE))
                .addMethod(createSetter("createBy", STRING_TYPE))
                .addMethod(createGetter("createTime", LOCAL_DATE_TIME_TYPE))
                .addMethod(createSetter("createTime", LOCAL_DATE_TIME_TYPE))
                .addMethod(createGetter("updateBy", STRING_TYPE))
                .addMethod(createSetter("updateBy", STRING_TYPE))
                .addMethod(createGetter("updateTime", LOCAL_DATE_TIME_TYPE))
                .addMethod(createSetter("updateTime", LOCAL_DATE_TIME_TYPE))
                // 核心新增：生成包含所有字段的toString()方法
                .addMethod(createToStringMethod(classElement, className));

        // 3. 为createTime添加默认值
        classBuilder.addMethod(createConstructorWithCreateTime(classElement));

        // 4. 生成Java文件
        JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build())
                .indent("    ")
                .build();
        javaFile.writeTo(processingEnv.getFiler());

        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.NOTE, "已为 " + className + " 自动添加审计字段和toString()方法");
    }

    /**
     * 核心新增：生成包含所有字段的toString()方法
     */
    private MethodSpec createToStringMethod(TypeElement classElement, String className) {
        MethodSpec.Builder toStringBuilder = MethodSpec.methodBuilder("toString")
                // 重写Object的toString
                .addAnnotation(OVERRIDE_ANNOTATION)
                .addJavadoc("重写 toString")
                .addModifiers(Modifier.PUBLIC)
                .returns(STRING_TYPE);

        // 1. 初始化StringBuilder，拼接类名前缀
        toStringBuilder.addStatement("$T sb = new $T($S + \"[\")", STRING_BUILDER_TYPE, STRING_BUILDER_TYPE, className);

        // 2. 拼接原有类的所有字段
//        List<VariableElement> originalFields = ElementFilter.fieldsIn(classElement.getEnclosedElements());
//        for (VariableElement field : originalFields) {
//            String fieldName = field.getSimpleName().toString();
//            // 跳过static字段（可选，根据需求调整）
//            if (field.getModifiers().contains(Modifier.STATIC)) {
//                continue;
//            }
//            toStringBuilder.addStatement("sb.append($S + \"=\").append($N)", fieldName, fieldName);
//            toStringBuilder.addStatement("sb.append(\", \")");
//        }

        // 3. 拼接审计字段
        String[] auditFields = {"data", "createBy", "createTime", "updateBy", "updateTime"};
        for (String fieldName : auditFields) {
            toStringBuilder.addStatement("sb.append($S + \"=\").append($N)", fieldName, fieldName);
            toStringBuilder.addStatement("sb.append(\", \")");
        }

        // 4. 处理最后一个多余的", "
        toStringBuilder.addStatement("if (sb.length() > $L) { sb.setLength(sb.length() - 2); }", className.length() + 1);
        toStringBuilder.addStatement("sb.append(\"]\")");
        toStringBuilder.addStatement("return sb.toString()");

        return toStringBuilder.build();
    }

    /**
     * 保留原有类的字段（转为FieldSpec）
     */
    private Iterable<FieldSpec> getOriginalFields(TypeElement classElement) {
        return ElementFilter.fieldsIn(classElement.getEnclosedElements()).stream()
                .map(field -> FieldSpec.builder(
                        TypeName.get(field.asType()),
                        field.getSimpleName().toString(),
                        field.getModifiers().toArray(new Modifier[0])
                ).build())
                .toList();
    }

    /**
     * 创建审计字段（private + 注释）
     */
    private FieldSpec createAuditField(String fieldName, TypeName fieldType, String comment) {
        return FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE)
                .addJavadoc("@return " + comment + "\n")
                .build();
    }

    /**
     * 生成getter方法
     */
    private MethodSpec createGetter(String fieldName, TypeName fieldType) {
        String methodName = "get" + capitalize(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return this.$N", fieldName)
                .addJavadoc("获取 " + fieldName + "\n")
                .addJavadoc("@return " + fieldName + "\n")
                .build();
    }

    /**
     * 生成setter方法
     */
    private MethodSpec createSetter(String fieldName, TypeName fieldType) {
        String methodName = "set" + capitalize(fieldName);
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC)
                .returns(void.class)
                .addParameter(ParameterSpec.builder(fieldType, fieldName).build())
                .addStatement("this.$N = $N", fieldName, fieldName)
                .addJavadoc("设置 " + fieldName + "\n")
                .build();
    }

    /**
     * 构造器：初始化createTime为当前日期
     */
    private MethodSpec createConstructorWithCreateTime(TypeElement classElement) {
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        // 保留原有构造器参数
        for (ExecutableElement constructor : ElementFilter.constructorsIn(classElement.getEnclosedElements())) {
            if (constructor.getModifiers().contains(Modifier.PUBLIC)) {
                for (VariableElement param : constructor.getParameters()) {
                    constructorBuilder.addParameter(
                            TypeName.get(param.asType()),
                            param.getSimpleName().toString()
                    );
                }
                constructorBuilder.addStatement("this($L)",
                        constructor.getParameters().stream()
                                .map(VariableElement::getSimpleName)
                                .map(String::valueOf)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("")
                );
                break;
            }
        }

        // 初始化createTime为当前日期
        constructorBuilder.addStatement("this.createTime = $T.now()", LocalDateTime.class);
        return constructorBuilder.build();
    }

    /**
     * 首字母大写（适配JavaBean命名规范）
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}