package me.ygy.pathfinder.code.analysis;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

import java.util.*;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;

@Deprecated
public class CallGraphWithSpoonJGraphT {

    private static DefaultDirectedGraph<String, DefaultEdge> callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    private static Map<String, String> methodAnnotations = new HashMap<>();


    public static void main(String[] args) {
//        String projectPath = "/Users/yuguangyuan/code/github/test-proj";
        String projectPath = "/Users/yuguangyuan/code/csc/pc/csc108-etrade-licai-backend";

        buildGraph(projectPath);

        // 示例：逆向遍历调用链
//        String targetMethod = "org.example.App.api_five(org.example.AuthType,java.util.List)";
//        String targetMethod = "com.csc108.etrade.util.DES.isNumeric(java.lang.String)";
//        String targetMethod = "com.csc108.etrade.support.util.EtradeCommonUtil.listIsNotEmpty(java.util.List)";
//        String targetMethod = "com.csc108.etrade.service.licai.GateOTCService.getL2620100(com.csc108.etrade.model.licai.fund.FundSubscriptionInModel,java.util.Map,javax.servlet.http.HttpServletRequest)";
        String targetMethod = "com.csc108.etrade.aspect.ConsumerLimiterAspect.ApiConsumerLimit(org.aspectj.lang.ProceedingJoinPoint,com.csc108.etrade.aspect.RedisIncrLimit)";
        System.out.println("Reverse call chain for " + targetMethod + ":");
        Set<String> visited = new HashSet<>();
        reverseTraverse(targetMethod, 0, visited);
    }

    private static void processClassWithLombok(CtClass<?> clazz) {
        // 添加类中的显式方法到调用图
        for (CtMethod<?> method : clazz.getMethods()) {
            String caller = getMethodSignature(method);
            callGraph.addVertex(caller);
            processMethodInvocations(method, caller);
        }

        // 检查 Lombok 注解并推断方法
        if (clazz.hasAnnotation(Data.class) || clazz.hasAnnotation(Getter.class)
                || clazz.hasAnnotation(Setter.class) || clazz.hasAnnotation(Builder.class)) {
            generateLombokMethods(clazz);
        }
    }

    private static void generateLombokMethods(CtClass<?> clazz) {
        String className = getFullyQualifiedClassName(clazz.getReference());

        for (CtField<?> field : clazz.getFields()) {
            String fieldName = field.getSimpleName();
            String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            CtTypeReference<?> fieldType = field.getType();

            // 生成 Getter 方法
            if (clazz.hasAnnotation(Getter.class) || clazz.hasAnnotation(Data.class)) {
                // 如何field类型是boolean，
                // 则生成 isFieldName() 方法
                // 否则生成 getFieldName() 方法
                String getterName = "get" + capitalizedFieldName;
                if (fieldType.isPrimitive() && (fieldType.getSimpleName().equals("boolean") || fieldType.getSimpleName().equals("Boolean"))) {
                    getterName = "is" + capitalizedFieldName;
                }
                if (clazz.getMethodsByName(getterName).isEmpty()) {
                    CtMethod<?> getterMethod = clazz.getFactory().Method().create(
                            clazz,
                            new HashSet<>(Collections.singletonList(ModifierKind.PUBLIC)),
                            fieldType,
                            getterName,
                            Collections.emptyList(),
                            Collections.emptySet()
                    );
                    getterMethod.setBody(clazz.getFactory().Code().createCtBlock(
                            clazz.getFactory().Code().createCodeSnippetStatement("return this." + fieldName)
                    ));
                    clazz.addMethod(getterMethod);
//                    System.out.println("Added getter: " + className + "." + getterName + "()");
                }
            }

            // 生成 Setter 方法
            if (clazz.hasAnnotation(Setter.class) || clazz.hasAnnotation(Data.class)) {
                String setterName = "set" + capitalizedFieldName;
                if (clazz.getMethodsByName(setterName).isEmpty()) {
                    CtParameter<?> parameter = clazz.getFactory().Core().createParameter();
                    parameter.setType(fieldType);
                    parameter.setSimpleName(fieldName);

                    CtMethod<Void> setterMethod = clazz.getFactory().Method().create(
                            clazz,
                            new HashSet<>(Collections.singletonList(ModifierKind.PUBLIC)),
                            clazz.getFactory().Type().voidPrimitiveType(),
                            setterName,
                            Collections.singletonList(parameter),
                            Collections.emptySet()
                    );
                    setterMethod.setBody(clazz.getFactory().Code().createCtBlock(
                            clazz.getFactory().Code().createCodeSnippetStatement("this." + fieldName + " = " + fieldName)
                    ));
                    clazz.addMethod(setterMethod);
//                    System.out.println("Added setter: " + className + "." + setterName + "(" + fieldType.getQualifiedName() + ")");
                }
            }
        }
    }


    private static void processMethodInvocations(CtMethod<?> method, String caller) {
        List<CtInvocation<?>> invocations = method.getElements(new TypeFilter<>(CtInvocation.class));
        for (CtInvocation<?> invocation : invocations) {
            CtExecutableReference<?> execRef = invocation.getExecutable();
            String callee = getExecutableSignature(execRef);

            if (callee != null) {
                callGraph.addVertex(callee);
                callGraph.addEdge(caller, callee);
            }
        }
    }

//    private static String getMethodSignature(CtMethod<?> method) {
//        CtElement parent = method.getParent();
//        if (parent instanceof CtClass) {
//            CtClass<?> cz = (CtClass<?>) parent;
//            String declaringClass = getFullyQualifiedClassName(cz.getReference());
//            return declaringClass + "." + method.getSignature();
//        }
//        return null;
//    }

    private static String getFullyQualifiedClassName(CtTypeReference<?> typeRef) {
        StringBuilder fullName = new StringBuilder(typeRef.getSimpleName());
        CtTypeReference<?> parent = typeRef.getDeclaringType();
        while (parent != null) {
            fullName.insert(0, parent.getSimpleName() + "$");
            parent = parent.getDeclaringType();
        }
        String packageName = typeRef.getPackage() != null ? typeRef.getPackage().getQualifiedName() : "";
        if (!packageName.isEmpty()) {
            fullName.insert(0, packageName + ".");
        }
        return fullName.toString();
    }

    public static void buildGraph(String projectPath) {
        MavenLauncher launcher = new MavenLauncher(projectPath,
                MavenLauncher.SOURCE_TYPE.APP_SOURCE, "/Users/yuguangyuan/soft/apache-maven-3.9.9/");

        launcher.getEnvironment().setComplianceLevel(17);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.buildModel();

        CtModel model = launcher.getModel();

        for (CtClass<?> clazz : model.getElements(new TypeFilter<>(CtClass.class))) {
            processClassWithLombok(clazz);
        }

        for (CtMethod<?> method : model.getElements(new TypeFilter<>(CtMethod.class))) {
            String caller = getMethodSignature(method);
            callGraph.addVertex(caller);

            List<CtInvocation<?>> invocations = method.getElements(new TypeFilter<>(CtInvocation.class));
            for (CtInvocation<?> invocation : invocations) {
                CtExecutableReference<?> execRef = invocation.getExecutable();
                String callee = getExecutableSignature(execRef);

                if (callee != null) {
                    callGraph.addVertex(callee);
                    callGraph.addEdge(caller, callee);
                }
            }
        }
    }

    private static String getFullyQualifiedClassName(CtClass<?> clazz) {
        StringBuilder fullName = new StringBuilder(clazz.getSimpleName());
        CtElement parent = clazz.getParent();
        while (parent instanceof CtClass) {
            CtClass<?> parentClass = (CtClass<?>) parent;
            fullName.insert(0, parentClass.getSimpleName() + "$");
            parent = parentClass.getParent();
        }
        return fullName.toString();
    }

    private static String getMethodSignature(CtMethod<?> method) {
        CtElement parent = method.getParent();
        if (parent instanceof CtClass) {
            CtClass<?> cz = (CtClass<?>) parent;
            String curClazz = getFullyQualifiedClassName(cz);
            String curPkg = cz.getPackage() != null ? cz.getPackage().getQualifiedName() : "-";

            String declaringClass = curPkg.isEmpty() ? curClazz : curPkg + "." + curClazz;
            String fullMethodName = declaringClass + "." + method.getSignature();

            // 收集注解和注释信息
            StringBuilder annotationDetails = new StringBuilder();
            for (CtAnnotation<?> annotation : method.getAnnotations()) {
                // 判断Annotation是不是@RequestMapping
                if (annotation.getAnnotationType().getSimpleName().equals("RequestMapping")) {
                    // 截取@RequestMapping的value字段
                    String annotationValue = annotation.getValue("value").toString();
                    annotationDetails.append("url: ").append(annotationValue).append("\t");

                    if (method.getComments() != null && !method.getComments().isEmpty()) {
                        String comments = method.getComments().get(0).toString();
                        // 按\n分成多行
                        String[] lines = comments.split("\n");
                        String co = Arrays.stream(lines).map(line -> line.strip()).
                                filter(line -> !line.startsWith("/**") &&
                                        !line.startsWith("*/") && !line.startsWith("* @param") &&
                                        !line.startsWith("* @return") && !line.startsWith("* @throws") &&
                                        !line.startsWith("* \t\t入参") && !line.startsWith("* \t\t请求") &&
                                        !line.startsWith("* \t\t响应") && !line.startsWith("* \t\t异常"))
                                .collect(Collectors.joining(" "));
                        annotationDetails.append("Comment: ").append(co).append("\n");
                    }
                    if (annotationDetails.length() > 0) {
                        methodAnnotations.put(fullMethodName, annotationDetails.toString());
                    }
                }
            }

            return fullMethodName;
        } else if (parent instanceof CtAnnotationType<?>) {
           CtAnnotationType<?> at = (CtAnnotationType<?>) parent;
           String curClazz = at.getSimpleName();
           String curPkg = null != at.getPackage() ? at.getPackage().getQualifiedName() : "-";

           String declaringClass = curPkg + "." + curClazz;
           return declaringClass + "." + method.getSignature();
        } else if (parent instanceof CtInterface<?>) {
            CtInterface <?> itf = (CtInterface<?>) parent;
            String curClazz = itf.getSimpleName();
            String curPkg = null != itf.getPackage() ? itf.getPackage().getQualifiedName() : "-";

            String declaringClass = curPkg + "." + curClazz;
            return declaringClass + "." + method.getSignature();
        } else {
            return null;
        }
    }

    private static String getExecutableSignature(CtExecutableReference<?> execRef) {
        if (execRef.getDeclaringType() != null) {
            String declaringTypeName = getFullyQualifiedClassName(execRef.getDeclaringType());
            return declaringTypeName + "." + execRef.getSignature();
        }
        else {
            // 如果声明类型为空，尝试推断
            String methodSignature = inferLombokMethodSignature(execRef);
            if (methodSignature == null) {
                // todo 是链式调用时，如何推断签名

            }
            return methodSignature;
        }
    }

    private static String inferLombokMethodSignature(CtExecutableReference<?> execRef) {
        // 获取方法名
        String methodName = execRef.getSimpleName();
        // 获取可能的调用目标
        CtElement parent = execRef.getParent();
        if (parent instanceof CtInvocation<?>) {
            CtInvocation<?> invocation = (CtInvocation<?>) parent;
            CtExpression<?> target = invocation.getTarget();
            if (target != null && target.getType() != null) {
                String declaringType = target.getType().getQualifiedName();
                // 推断 Getter 方法
                if (methodName.startsWith("get") && methodName.length() > 3) {
                    return declaringType + "." + methodName + "()";
                }
                // 推断 Setter 方法
                if (methodName.startsWith("set") && methodName.length() > 3) {
                    String parameterType = execRef.getParameters().isEmpty() ? "?" : execRef.getParameters().get(0).getQualifiedName();
                    return declaringType + "." + methodName + "(" + parameterType + ")";
                }
                // 推断 Builder 方法
                if (methodName.equals("builder")) {
                    return declaringType + ".builder()";
                }
            }else if (target instanceof CtInvocation<?>) {
                // 递归向上查找链式调用的父级返回类型
//                String parentSignature = getExecutableSignature(parentInvocation.getExecutable());
//                if (parentSignature != null) {
//                    // 使用父级返回类型作为当前声明类型
//                    String parentReturnType = inferReturnTypeFromModel(parentInvocation.getExecutable());
//                    if (parentReturnType != null) {
//                        return parentReturnType + "." + execRef.getSignature();
//                    }
//                }
            }
        }
        System.out.println("Failed to infer method call, method name: " + methodName + ", parent: " + parent);
        return null; // 如果推断失败，返回 null
    }

    private static String inferReturnTypeFromModel(CtExecutableReference<?> execRef) {
        if (execRef.getType() != null) {
            // 如果返回类型在引用中明确
            return execRef.getType().getQualifiedName();
        }

        // 如果返回类型不明确，可以尝试从全局方法模型中查找
        List<CtMethod<?>> methods = execRef.getFactory()
                .getModel()
                .getElements(new TypeFilter<>(CtMethod.class));

        for (CtMethod<?> method : methods) {
            if (method.getSimpleName().equals(execRef.getSimpleName()) &&
                    method.getParameters().size() == execRef.getParameters().size()) {
                // 方法名和参数个数匹配时，返回声明类型
                CtTypeReference<?> declaringType = (CtTypeReference<?>) method.getDeclaringType();
                return declaringType != null ? declaringType.getQualifiedName() : null;
            }
        }

        return null; // 如果无法推断，返回 null
    }


//    private static String getFullyQualifiedClassName(CtTypeReference<?> typeRef) {
//        StringBuilder fullName = new StringBuilder(typeRef.getSimpleName());
//        CtTypeReference<?> parent = typeRef.getDeclaringType();
//        while (parent != null) {
//            fullName.insert(0, parent.getSimpleName() + "$");
//            parent = parent.getDeclaringType();
//        }
//        String packageName = typeRef.getPackage() != null ? typeRef.getPackage().getQualifiedName() : "";
//        if (!packageName.isEmpty()) {
//            fullName.insert(0, packageName + ".");
//        }
//        return fullName.toString();
//    }

    public static void reverseTraverse(String targetMethod, int indent, Set<String> visited) {
        if (!callGraph.containsVertex(targetMethod)) {
            System.out.println("Method not found in the call graph: " + targetMethod);
            return;
        }


        String message = targetMethod;
        // 打印方法的注解和注释（如果存在）
        if (methodAnnotations.containsKey(targetMethod)) {
            message += "  " + methodAnnotations.get(targetMethod);
        }

        // 如果已经访问过，标记为递归调用
        if (visited.contains(targetMethod)) {
            printWithIndent(message, indent);
            return;
        }

        // 将当前方法标记为已访问
        visited.add(targetMethod);

        printWithIndent(message, indent);
        callGraph.incomingEdgesOf(targetMethod).forEach(edge -> {
            String caller = callGraph.getEdgeSource(edge);
            reverseTraverse(caller, indent + 1, visited);
        });

        // 递归结束后移除标记，允许后续其他路径重新访问
        visited.remove(targetMethod);
    }

    private static void printWithIndent(String message, int indent) {
        for (int i = 0; i < indent; i++) {
            System.out.print("    ");
        }
        System.out.println(message);
    }

    private static void reverseDFS(String method, Set<String> visited, List<String> result) {
        if (visited.contains(method)) {
            return;
        }
        visited.add(method);
        result.add(method);

        callGraph.incomingEdgesOf(method).forEach(edge -> {
            String caller = (String) callGraph.getEdgeSource(edge);
            reverseDFS(caller, visited, result);
        });
    }
}
