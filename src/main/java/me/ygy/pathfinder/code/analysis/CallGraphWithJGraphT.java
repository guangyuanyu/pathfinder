package me.ygy.pathfinder.code.analysis;

import com.github.javaparser.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.*;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametrized;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;
import org.jgrapht.*;
import org.jgrapht.graph.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CallGraphWithJGraphT {

    private static JavaParserCache javaParserCache = null;

    // 创建有向图 (方法签名为节点)
    private static DefaultDirectedGraph<String, DefaultEdge> callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

    public static void main(String[] args) throws IOException {
        // Maven 项目的根路径
        String projectPath = "/Users/yuguangyuan/code/csc/pc/csc108-etrade-licai-backend";
//        String projectPath = "/Users/yuguangyuan/code/github/test-proj";
        javaParserCache = JavaParserCache.getParser(projectPath);
        // 目标方法列表（完整签名）
        List<String> targetMethods = Arrays.asList(
//                "com.csc108.etrade.support.util.EtradeCommonUtil.listIsNotEmpty(java.util.List)",
//                "com.csc108.etrade.common.exception.EtradeTraceSupportException.Signature.getMethodName()"
//                "com.csc108.etrade.util.DES.isNumeric(java.lang.String)",
//                "org.example.App.api_two(java.lang.String, org.example.Auth)"
//                "com.csc108.etrade.aspect.LuaScriptLimit.buildLuaScriptByList()"
//                "com.csc108.etrade.aspect.ConsumerLimiterAspect.ApiConsumerLimit(org.aspectj.lang.ProceedingJoinPoint, com.csc108.etrade.aspect.RedisIncrLimit)",
//                "org.apache.commons.lang.StringUtils.equals(java.lang.String, java.lang.String)"
//                "com.csc108.etrade.service.cfzh.AccountService.getL2620237(java.util.Map, java.util.Map, javax.servlet.http.HttpServletRequest)",
//                "com.csc108.etrade.service.CaCommonService.getLshAndYslb(com.csc108.etrade.support.session.LSession, java.util.Map, java.lang.String, java.lang.String, java.lang.String, java.lang.String)",
//                "com.csc108.etrade.aspect.ConsumerLimiterAspect.getDeclaredMethodFor(java.lang.Class, java.lang.String, java.lang.Class[])",
                "com.csc108.etrade.service.licai.GateOTCService.getL2620100(com.csc108.etrade.model.licai.fund.FundSubscriptionInModel, java.util.Map, javax.servlet.http.HttpServletRequest)"
        );

        // 遍历项目并解析所有 Java 文件
        try {
            List<File> javaFiles = listJavaFiles(new File(projectPath + "/src/main/java"));
            for (File file : javaFiles) {
                parseJavaFile(file);
            }

            for (File file : javaFiles) {
                parseJavaFile(file, callGraph);
            }

            // 查找每个目标方法的完整反向调用链
            for (String targetMethod : targetMethods) {
                System.out.println("Reverse call chain for method: " + targetMethod);
//                List<String> reverseCallChain = findReverseCallChain(callGraph, targetMethod);
//                reverseCallChain.forEach(System.out::println);
                Set<String> visited = new HashSet<>();
                reverseTraverse(targetMethod, 0, visited);
                System.out.println("--------------------");
            }

        } catch (IOException e) {
//            e.printStackTrace();
        }
    }

    public static void reverseTraverse(String targetMethod, int indent, Set<String> visited) {
        if (!callGraph.containsVertex(targetMethod)) {
            System.out.println("Method not found in the call graph: " + targetMethod);
            return;
        }


        String message = targetMethod;
        // 打印方法的注解和注释（如果存在）
//        if (methodAnnotations.containsKey(targetMethod)) {
//            message += "  " + methodAnnotations.get(targetMethod);
//        }

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



    private static void processLombokAnnotations(ClassOrInterfaceDeclaration clazz, boolean hasDataAnnotation,
                                                 boolean hasValueAnnotation, boolean hasAllArgsConstructor,
                                                 boolean hasNoArgsConstructor, boolean hasClassGetterAnnotation,
                                                 boolean hasClassSetterAnnotation) {
        // 添加 @Value 的特性
        if (hasValueAnnotation) {
            clazz.getFields().forEach(field -> field.setFinal(true)); // 设置字段为 final
        }

        boolean isChanged = false;

        // 添加 @Getter/@Setter/@Data 的行为
        for (FieldDeclaration field : clazz.getFields()) {
            boolean hasFieldGetterAnnotation = field.getAnnotationByName("Getter").isPresent();
            boolean hasFieldSetterAnnotation = field.getAnnotationByName("Setter").isPresent();

            String fieldName = field.getVariables().get(0).getNameAsString();
            String fieldType = field.getVariables().get(0).getType().toString();

            boolean shouldGenerateGetter = hasFieldGetterAnnotation || hasDataAnnotation || hasValueAnnotation || hasClassGetterAnnotation;
            boolean shouldGenerateSetter = (hasFieldSetterAnnotation || hasDataAnnotation || hasClassSetterAnnotation) && !hasValueAnnotation;

            if (shouldGenerateGetter) {
                // 判断field类型是不是boolean或Boolean类型，是的话生成is方法，否则生成get方法
                if (fieldType.equals("boolean") || fieldType.equals("java.lang.Boolean")) {
                    clazz.addMethod("is" + capitalize(fieldName))
                            .setType(fieldType)
                            .setBody(null);
                    isChanged = true;
                } else {
                    clazz.addMethod("get" + capitalize(fieldName))
                            .setType(fieldType)
                            .setBody(null);
                    isChanged = true;
                }
            }

            if (shouldGenerateSetter) {
                clazz.addMethod("set" + capitalize(fieldName))
                        .addParameter(fieldType, fieldName)
                        .setBody(null);
                isChanged = true;

            }
        }

        // 添加 @AllArgsConstructor 构造器
        if (hasAllArgsConstructor) {
            var constructor = clazz.addConstructor();
            clazz.getFields().forEach(field -> {
                String fieldName = field.getVariables().get(0).getNameAsString();
                String fieldType = field.getVariables().get(0).getType().toString();
                constructor.addParameter(new Parameter(StaticJavaParser.parseType(fieldType), fieldName));
                constructor.setBody(null); // 可填充逻辑
            });
            isChanged = true;
        }

        // 添加 @NoArgsConstructor 构造器
        if (hasNoArgsConstructor) {
            clazz.addConstructor().setBody(null); // 空构造器
            isChanged = true;
        }

        if (isChanged) {
            String clazzName = clazz.getFullyQualifiedName().get();
            javaParserCache.addToMemoryTypeSolver(clazzName, clazz.resolve());
        }
    }

    private static String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }


    /**
     * 递归列出目录中的所有 Java 文件
     */
    private static List<File> listJavaFiles(File directory) throws IOException {
        List<File> javaFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                javaFiles.addAll(listJavaFiles(file));
            }
        } else if (directory.isFile() && directory.getName().endsWith(".java")) {
            javaFiles.add(directory);
        }
        return javaFiles;
    }

    private static void parseJavaFile(File file) {
        try {
            CompilationUnit cu = javaParserCache.parse(file);
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {
                processClass(clazz);
            });
        } catch (Exception e) {
            System.err.println("Error parsing file: " + file.getPath());
            e.printStackTrace();
        }
    }

    private static void processClass(ClassOrInterfaceDeclaration clazz) {
        // 检查类级别注解
        boolean hasDataAnnotation = clazz.getAnnotationByName("Data").isPresent();
        boolean hasValueAnnotation = clazz.getAnnotationByName("Value").isPresent();
        boolean hasAllArgsConstructor = clazz.getAnnotationByName("AllArgsConstructor").isPresent();
        boolean hasNoArgsConstructor = clazz.getAnnotationByName("NoArgsConstructor").isPresent();
        boolean hasClassGetterAnnotation = clazz.getAnnotationByName("Getter").isPresent();
        boolean hasClassSetterAnnotation = clazz.getAnnotationByName("Setter").isPresent();

        processLombokAnnotations(clazz, hasDataAnnotation, hasValueAnnotation,
                hasAllArgsConstructor, hasNoArgsConstructor,
                hasClassGetterAnnotation, hasClassSetterAnnotation);

        if (clazz.getAnnotationByName("Slf4j").isPresent()) {
            addLoggerField(clazz);
        }
    }

    private static void addLoggerField(ClassOrInterfaceDeclaration clazz) {
        // 检查是否已存在名为 "log" 的字段
        boolean hasLogField = clazz.getFields().stream()
                .anyMatch(field -> field.getVariables().stream()
                        .anyMatch(variable -> variable.getNameAsString().equals("log")));

        if (!hasLogField) {
            // 创建 Logger 字段
            FieldDeclaration logField = new FieldDeclaration();
            logField.addModifier(Modifier.Keyword.PRIVATE, Modifier.Keyword.STATIC, Modifier.Keyword.FINAL);

            // 设置类型为 org.slf4j.Logger
            ClassOrInterfaceType loggerType = StaticJavaParser.parseClassOrInterfaceType("org.slf4j.Logger");
            VariableDeclarator logVariable = new VariableDeclarator(loggerType, "log");

            // 设置初始化为 LoggerFactory.getLogger(CurrentClassName.class)
            logVariable.setInitializer("org.slf4j.LoggerFactory.getLogger(" + clazz.getNameAsString() + ".class)");

            logField.addVariable(logVariable);

            // 添加到类中
            clazz.addMember(logField);

            String clazzName = clazz.getFullyQualifiedName().get();
            javaParserCache.addToMemoryTypeSolver(clazzName, clazz.resolve());
        }
    }

    /**
     * 解析单个 Java 文件，构建方法调用图
     */
    private static void parseJavaFile(File file, DefaultDirectedGraph<String, DefaultEdge> callGraph) {
        try {
            CompilationUnit cu = javaParserCache.parse(file);

            cu.findAll(MethodDeclaration.class).forEach(method -> {
                addMethod2Graph(callGraph, method, cu);
            });
        } catch (Exception e) {
            System.err.println("Error parsing file: " + file.getPath());
//            e.printStackTrace();
        }
    }

    private static void addMethod2Graph(DefaultDirectedGraph<String, DefaultEdge> callGraph, MethodDeclaration method, CompilationUnit cu) {
        String qualifiedMethodName = getQualifiedMethodSignature(method);

        // 添加方法到图中
//        if (qualifiedMethodName.contains("getDeclaredMethodFor")) {
//            System.out.println("---------" + qualifiedMethodName);
//        }
        callGraph.addVertex(qualifiedMethodName);

        // 提取方法体中的方法调用
        List<String> calledMethods = new ArrayList<>();
        method.accept(new VoidVisitorAdapter<List<String>>() {
            @Override
            public void visit(MethodCallExpr methodCall, List<String> collector) {
                super.visit(methodCall, collector);
                String calledMethod = getQualifiedMethodSignature(qualifiedMethodName, methodCall, cu);
                if (calledMethod != null) {
                    collector.add(calledMethod);
                }
            }
        }, calledMethods);

        // 添加调用关系 (边)
        for (String calledMethod : calledMethods) {
            callGraph.addVertex(calledMethod);
            callGraph.addEdge(qualifiedMethodName, calledMethod);
        }
    }

    /**
     * 查找目标方法的反向调用链
     */
    private static List<String> findReverseCallChain(DefaultDirectedGraph<Object, DefaultEdge> callGraph, String targetMethod) {
        List<String> reverseCallChain = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        findReverseCallChainRecursive(callGraph, targetMethod, visited, reverseCallChain);
        return reverseCallChain;
    }

    /**
     * 递归查找反向调用链
     */
    private static void findReverseCallChainRecursive(DefaultDirectedGraph<Object, DefaultEdge> callGraph,
                                                      String targetMethod,
                                                      Set<String> visited,
                                                      List<String> reverseCallChain) {
        if (visited.contains(targetMethod)) {
            return;
        }
        visited.add(targetMethod);
        reverseCallChain.add(targetMethod);

        // 查找所有调用者 (入边)
        for (DefaultEdge edge : callGraph.incomingEdgesOf(targetMethod)) {
            Object caller = callGraph.getEdgeSource(edge);
            findReverseCallChainRecursive(callGraph, caller.toString(), visited, reverseCallChain);
        }
    }

    /**
     * 获取方法的全限定签名 (类名.方法名(参数类型列表))
     */
    private static String getQualifiedMethodSignature(MethodDeclaration method) {
        String className = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .map(clazz -> clazz.getFullyQualifiedName().orElse(clazz.getNameAsString()))
                .orElse("");
        if (className.isEmpty()) {
            className = method.findAncestor(EnumDeclaration.class)
                    .map(clazz -> clazz.getFullyQualifiedName().orElse(clazz.getNameAsString()))
                    .orElse("");
        }
        String methodName = method.getNameAsString();
        String paramTypes = method.getParameters().stream()
                .map(param -> getParamRawTypeName(param))
                .reduce((a, b) -> a + ", " + b).orElse("");
        String qualifiedMethodName =  String.format("%s.%s(%s)", className, methodName, paramTypes);
        // 去掉qualifiedMethodName中的泛型
        qualifiedMethodName = qualifiedMethodName.replaceAll("<.*?>", "");
        return qualifiedMethodName;
    }

    /**
     * 获取方法调用的全限定签名 (类名.方法名(参数类型列表))
     */
    private static String getQualifiedMethodSignature(String methodCaller, MethodCallExpr methodCall, CompilationUnit cu) {
        String className = "";
        try {
            if (methodCall.getScope().isPresent()) {
                className = methodCall.getScope().get().calculateResolvedType().describe();
            }
//            className = methodCall.resolve().declaringType().asReferenceType().getQualifiedName();
        } catch (Throwable ex) {
            System.out.println("resolve failed: " + methodCall.toString());
        }
        if (className.isEmpty()) {
            try {
//                className = methodCall.getScope().get().calculateResolvedType().describe();
                className = methodCall.resolve().declaringType().asReferenceType().getQualifiedName();
            } catch (Throwable ex) {
                System.out.println("calculateResolvedType failed: " + methodCall.toString());
            }
        }

        if (className.isEmpty()) {
            className = methodCall.findAncestor(ClassOrInterfaceDeclaration.class)
                    .map(clazz -> clazz.getFullyQualifiedName().orElse(clazz.getNameAsString()))
                    .orElse("");
            System.out.println("apply failed but found class name: "+ className);
        }
        if (className.isEmpty()) {
            className = methodCall.findAncestor(EnumDeclaration.class)
                    .map(clazz -> clazz.getFullyQualifiedName().orElse(clazz.getNameAsString()))
                    .orElse("");
            System.out.println("apply failed but found enum name: "+ className);
        }
        String methodName = methodCall.getNameAsString();
        List<String> paramTypes = new ArrayList<>();
//        int numberOfParams = methodCall.resolve().getNumberOfParams();
        for (int i = 0; i < methodCall.getArguments().size(); i++) {
            Expression arg = methodCall.getArguments().get(i);
            String param = "";
            if (arg instanceof MethodCallExpr) {
                param = getReturnTypeOfMethodCall((MethodCallExpr) arg, cu);
            } else if (arg.isMethodReferenceExpr()) {
                MethodReferenceExpr methodRef = arg.asMethodReferenceExpr();
                // 查找包含该 MethodReferenceExpr 的方法调用
                if (methodRef.getParentNode().isPresent() && methodRef.getParentNode().get() instanceof MethodCallExpr) {
                    param = methodCall.getArguments().get(i).calculateResolvedType().describe();
                }
                // 将MethodReferenceExpr加入callGraph
                processMethodReferenceExpr(methodCaller, methodRef); // 新方法处理 MethodReferenceExpr
            }
            if (param.equals("")) {
                try {
                    ResolvedType resolvedType = arg.calculateResolvedType();
                    param = getRawTypeName(resolvedType);
                } catch (Exception e) {
                    System.out.println("Error 1: " + e.getMessage() + "arg: " + arg.toString() + "method: " + methodCaller.toString());
//                    e.printStackTrace();
                    param = "java.lang.Object";
                }
            }
            paramTypes.add(param);
        }

        String params = paramTypes.stream().reduce((a, b) -> a + ", " + b).orElse("");
        String callee = String.format("%s.%s(%s)", className, methodName, params);
        // 去掉qualifiedMethodName中的泛型
        callee = callee.replaceAll("<.*?>", "");
        return callee;
    }

    private static String processMethodReferenceExpr(String methodCaller, MethodReferenceExpr methodRef) {
        try {
            // 获取方法引用的全限定签名
            ResolvedMethodDeclaration resolvedMethod = methodRef.resolve();
            String callee = getQualifiedMethodSignature(((JavaParserMethodDeclaration) resolvedMethod).getWrappedNode());
            // 将方法引用加入调用图
            callGraph.addVertex(callee); // 确保方法引用作为节点加入
            callGraph.addEdge(methodCaller, callee);
            return callee;
        } catch (Exception e) {
            System.out.println("Error processing MethodReferenceExpr: " + e.getMessage());
            return null;
        }
    }

    private static String getReturnTypeOfMethodCall(MethodCallExpr methodCall, CompilationUnit cu) {
        try {
            ResolvedType resolvedType = methodCall.calculateResolvedType();
            return getRawTypeName(resolvedType);
        } catch (Exception e) {
            System.out.println("Error resolving method call type: " + e.getMessage());
//            e.printStackTrace();
            return "";
        }
    }

    private static String getParamRawTypeName(Parameter param) {
        try {
            ResolvedType resolvedType = param.getType().resolve();
            boolean varArgs = param.isVarArgs();
            if (!varArgs) {
                return getRawTypeName(resolvedType);
            } else {
                return getRawTypeName(resolvedType) + "[]";
            }
        } catch (Exception e) {
            System.out.println("Error 2: " + e.getMessage());
            return "java.lang.Object";
        }
    }

    private static String getRawTypeName(ResolvedType resolvedType) {
        if (resolvedType.isReferenceType()) {
            ResolvedReferenceType refType = resolvedType.asReferenceType();
            return refType.getQualifiedName(); // 返回原始类型，例如 java.util.List
        }
        return resolvedType.describe(); // 对于非引用类型，直接返回描述
    }
}
