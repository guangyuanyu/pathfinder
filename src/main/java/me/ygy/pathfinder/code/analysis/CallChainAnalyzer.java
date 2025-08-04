package me.ygy.pathfinder.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CallChainAnalyzer {
    private final String baseDir;
    private final Map<String, Set<JavaDiffAnalyzer.MethodSignature>> changedMethods;
    private final Map<String, Set<String>> changedFields;
    private final Map<String, CompilationUnit> compilationUnits;
    private final Map<String, List<CallChain>> callChains;
    private final JavaSymbolSolver symbolSolver;

    public static class CallChain {
        private final List<MethodNode> chain;
        private final String controllerUrl;
        private final String controllerMethod;
        private final String controllerComment;

        public CallChain(List<MethodNode> chain, String controllerUrl,
                         String controllerMethod, String controllerComment) {
            this.chain = chain;
            this.controllerUrl = controllerUrl;
            this.controllerMethod = controllerMethod;
            this.controllerComment = controllerComment;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (controllerUrl != null) {
                sb.append("Controller URL: ").append(controllerUrl).append("\n");
                sb.append("Controller Method: ").append(controllerMethod).append("\n");
                if (controllerComment != null && !controllerComment.trim().isEmpty()) {
                    sb.append("Comment: ").append(controllerComment).append("\n");
                }
            }
            sb.append("Call Chain:\n");
            for (int i = 0; i < chain.size(); i++) {
                sb.append("  ".repeat(i)).append(chain.get(i)).append("\n");
            }
            return sb.toString();
        }
    }

    public static class MethodNode {
        private final String className;
        private final String methodSignature;
        private final boolean isController;

        public MethodNode(String className, String methodSignature, boolean isController) {
            this.className = className;
            this.methodSignature = methodSignature;
            this.isController = isController;
        }

        @Override
        public String toString() {
            return className + "#" + methodSignature;
        }
    }

    public CallChainAnalyzer(String baseDir,
                             Map<String, Set<JavaDiffAnalyzer.MethodSignature>> changedMethods,
                             Map<String, Set<String>> changedFields) throws IOException {
        this.baseDir = baseDir;
        this.changedMethods = changedMethods;
        this.changedFields = changedFields;
        this.compilationUnits = new HashMap<>();
        this.callChains = new HashMap<>();

        // 设置符号解析器
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        typeSolver.add(new JavaParserTypeSolver(new File(baseDir)));
        this.symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        // 加载所有Java文件
        loadAllJavaFiles();
    }

    private void loadAllJavaFiles() throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(baseDir))) {
            paths.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            CompilationUnit cu = StaticJavaParser.parse(path);
                            String packageName = cu.getPackageDeclaration()
                                    .map(pd -> pd.getNameAsString())
                                    .orElse("");
                            cu.findAll(ClassOrInterfaceDeclaration.class)
                                    .forEach(cls -> {
                                        String fullName = packageName.isEmpty() ?
                                                cls.getNameAsString() :
                                                packageName + "." + cls.getNameAsString();
                                        compilationUnits.put(fullName, cu);
                                    });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    public void analyze() {
        // 对每个变更的方法分析调用链
        changedMethods.forEach((className, methods) -> {
            methods.forEach(methodSig -> {
                List<CallChain> chains = findCallChains(className, methodSig);
                callChains.put(className + "#" + methodSig, chains);
            });
        });

        // 对每个变更的字段分析调用链
        changedFields.forEach((className, fields) -> {
            fields.forEach(field -> {
                List<CallChain> chains = findFieldCallChains(className, field);
                callChains.put(className + "#" + field, chains);
            });
        });
    }

    private String tryGetQualifiedClassName(MethodCallExpr methodCall) {
        try {
            // 尝试符号解析
            ResolvedMethodDeclaration resolvedMethod = methodCall.resolve();
            return resolvedMethod.declaringType().getQualifiedName();
        } catch (Exception e) {
            System.out.println("解析方法调用失败: " + methodCall + ", 尝试从作用域和导入声明中推断" + e.getMessage());
            // 如果解析失败，尝试从作用域和导入声明中推断
            Optional<Expression> scope = methodCall.getScope();
            if (scope.isPresent()) {
                // 如果有作用域，使用作用域的类型名
                return scope.get().toString();
            } else {
                // 如果没有作用域，查找当前类的导入声明
                CompilationUnit cu = methodCall.findAncestor(CompilationUnit.class).orElse(null);
                if (cu != null) {
                    // 获取方法所在的类
                    ClassOrInterfaceDeclaration currentClass = methodCall.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                    if (currentClass != null) {
                        // 获取当前包名
                        String packageName = cu.getPackageDeclaration()
                                .map(pd -> pd.getNameAsString() + ".")
                                .orElse("");
                        return packageName + currentClass.getNameAsString();
                    }
                }
            }
        }
        return "";
    }

    private String tryGetQualifiedClassNameForField(FieldAccessExpr fieldAccess) {
        try {
            return fieldAccess.resolve().asField().declaringType().getQualifiedName();
        } catch (Exception e) {
            Expression scope = fieldAccess.getScope();
            CompilationUnit cu = fieldAccess.findAncestor(CompilationUnit.class).orElse(null);
            if (cu != null) {
                // 获取字段所在的类
                ClassOrInterfaceDeclaration currentClass = fieldAccess.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                if (currentClass != null) {
                    String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString() + ".")
                            .orElse("");
                    return packageName + currentClass.getNameAsString();
                }
            }
            return scope.toString();
        }
    }

    private String tryGetQualifiedClassNameForName(NameExpr nameExpr) {
        try {
            return nameExpr.resolve().asField().declaringType().getQualifiedName();
        } catch (Exception e) {
            CompilationUnit cu = nameExpr.findAncestor(CompilationUnit.class).orElse(null);
            if (cu != null) {
                ClassOrInterfaceDeclaration currentClass = nameExpr.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                if (currentClass != null) {
                    String packageName = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString() + ".")
                            .orElse("");
                    return packageName + currentClass.getNameAsString();
                }
            }
            return "";
        }
    }
    private static class ClassNameResolver {
        private final Set<String> imports;
        private final Set<String> wildcardImports;
        private final String currentPackage;
        private final Map<String, Set<String>> superClassMap = new HashMap<>();

        public ClassNameResolver(CompilationUnit cu) {
            // 收集普通导入
            this.imports = cu.getImports().stream()
                    .filter(imp -> !imp.isAsterisk())
                    .map(ImportDeclaration::getNameAsString)
                    .collect(Collectors.toSet());

            // 收集通配符导入
            this.wildcardImports = cu.getImports().stream()
                    .filter(ImportDeclaration::isAsterisk)
                    .map(imp -> imp.getNameAsString().substring(0, imp.getNameAsString().length() - 2))
                    .collect(Collectors.toSet());

            this.currentPackage = cu.getPackageDeclaration()
                    .map(pd -> pd.getNameAsString() + ".")
                    .orElse("");

            // 收集类的继承关系
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(this::collectInheritanceInfo);
        }

        private void collectInheritanceInfo(ClassOrInterfaceDeclaration cls) {
            String className = cls.getFullyQualifiedName().orElse(currentPackage + cls.getNameAsString());
            Set<String> superTypes = new HashSet<>();

            // 添加继承的类
            cls.getExtendedTypes().forEach(type -> {
                String superClassName = resolveClassName(type.getNameAsString());
                superTypes.add(superClassName);
            });

            // 添加实现的接口
            cls.getImplementedTypes().forEach(type -> {
                String interfaceName = resolveClassName(type.getNameAsString());
                superTypes.add(interfaceName);
            });

            superClassMap.put(className, superTypes);
        }

        public Set<String> getAllPossibleClassNames(String simpleClassName) {
            Set<String> possibleNames = new HashSet<>();

            // 1. 如果已经是全限定名
            if (simpleClassName.contains(".")) {
                possibleNames.add(simpleClassName);
                return possibleNames;
            }

            // 2. 检查显式导入
            imports.stream()
                    .filter(imp -> imp.endsWith("." + simpleClassName))
                    .forEach(possibleNames::add);

            // 3. 检查通配符导入
            wildcardImports.forEach(basePackage ->
                    possibleNames.add(basePackage + "." + simpleClassName));

            // 4. 检查当前包
            possibleNames.add(currentPackage + simpleClassName);

            // 5. 默认加入 java.lang 包
            possibleNames.add("java.lang." + simpleClassName);

            return possibleNames;
        }

        public String resolveClassName(String simpleClassName) {
            Set<String> possibleNames = getAllPossibleClassNames(simpleClassName);
            return possibleNames.iterator().next(); // 返回第一个可能的名称
        }

        public boolean isAssignableFrom(String subClass, String superClass) {
            if (subClass.equals(superClass)) {
                return true;
            }

            Set<String> superTypes = superClassMap.get(subClass);
            if (superTypes != null) {
                if (superTypes.contains(superClass)) {
                    return true;
                }
                // 递归检查父类的父类
                for (String superType : superTypes) {
                    if (isAssignableFrom(superType, superClass)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    private List<CallChain> findCallChains(String targetClass, JavaDiffAnalyzer.MethodSignature targetMethod) {
        List<CallChain> chains = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            CompilationUnit cu = entry.getValue();
            ClassNameResolver resolver = new ClassNameResolver(cu);

            // 搜索所有可能的方法调用
            cu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                Optional<Expression> scope = methodCall.getScope();
                String methodName = methodCall.getNameAsString();

                if (methodName.equals(targetMethod.getName())) {
                    boolean matched = false;
                    String calledClassName = null;

                    // 1. 有作用域的调用 (例如: obj.method() 或 ClassName.method())
                    if (scope.isPresent()) {
                        Expression scopeExpr = scope.get();
                        if (scopeExpr instanceof NameExpr) {
                            Set<String> possibleClasses = resolver.getAllPossibleClassNames(scopeExpr.toString());
                            for (String className : possibleClasses) {
                                if (className.equals(targetClass) ||
                                        resolver.isAssignableFrom(className, targetClass)) {
                                    matched = true;
                                    calledClassName = className;
                                    break;
                                }
                            }
                        }
                    }
                    // 2. 无作用域的调用 (例如: method())
                    else {
                        ClassOrInterfaceDeclaration currentClass = methodCall.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                        if (currentClass != null) {
                            String currentClassName = resolver.resolveClassName(currentClass.getNameAsString());
                            if (currentClassName.equals(targetClass) ||
                                    resolver.isAssignableFrom(currentClassName, targetClass)) {
                                matched = true;
                                calledClassName = currentClassName;
                            }
                        }
                    }

                    if (matched && calledClassName != null) {
                        MethodDeclaration containingMethod = methodCall.findAncestor(MethodDeclaration.class).orElse(null);
                        if (containingMethod != null) {
                            buildCallChain(chains, containingMethod, methodCall, targetClass, targetMethod.toString(), visited);
                        }
                    }
                }
            });
        }

        return chains;
    }

    private String getMethodCallClassName(MethodCallExpr methodCall, ClassNameResolver resolver) {
        // 1. 检查方法调用是否有作用域
        if (methodCall.getScope().isPresent()) {
            Expression scope = methodCall.getScope().get();

            // 如果作用域是名称表达式（例如：MyClass.method()）
            if (scope instanceof NameExpr) {
                return resolver.resolveClassName(scope.toString());
            }

            // 如果作用域是字段访问（例如：this.field.method()）
            if (scope instanceof FieldAccessExpr) {
                FieldAccessExpr fieldAccess = (FieldAccessExpr) scope;
                return resolver.resolveClassName(fieldAccess.getScope().toString());
            }
        }

        // 2. 如果没有作用域，尝试从当前类上下文获取
        ClassOrInterfaceDeclaration currentClass = methodCall.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
        if (currentClass != null) {
            return resolver.resolveClassName(currentClass.getNameAsString());
        }

        return null;
    }

    private List<CallChain> findFieldCallChains(String targetClass, String targetField) {
        List<CallChain> chains = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            CompilationUnit cu = entry.getValue();
            ClassNameResolver resolver = new ClassNameResolver(cu);

            // 查找字段访问
            cu.findAll(FieldAccessExpr.class).forEach(fieldAccess -> {
                if (fieldAccess.getNameAsString().equals(targetField)) {
                    Expression scope = fieldAccess.getScope();
                    boolean matched = false;
                    String accessedClassName = null;

                    if (scope instanceof ThisExpr) {
                        ClassOrInterfaceDeclaration currentClass = fieldAccess.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                        if (currentClass != null) {
                            String className = resolver.resolveClassName(currentClass.getNameAsString());
                            if (className.equals(targetClass) ||
                                    resolver.isAssignableFrom(className, targetClass)) {
                                matched = true;
                                accessedClassName = className;
                            }
                        }
                    } else if (scope instanceof NameExpr) {
                        Set<String> possibleClasses = resolver.getAllPossibleClassNames(scope.toString());
                        for (String className : possibleClasses) {
                            if (className.equals(targetClass) ||
                                    resolver.isAssignableFrom(className, targetClass)) {
                                matched = true;
                                accessedClassName = className;
                                break;
                            }
                        }
                    }

                    if (matched && accessedClassName != null) {
                        MethodDeclaration containingMethod = fieldAccess.findAncestor(MethodDeclaration.class).orElse(null);
                        if (containingMethod != null) {
                            buildCallChain(chains, containingMethod, fieldAccess, targetClass, targetField, visited);
                        }
                    }
                }
            });

            // 查找简单名称访问
            cu.findAll(NameExpr.class).forEach(nameExpr -> {
                if (nameExpr.getNameAsString().equals(targetField)) {
                    ClassOrInterfaceDeclaration currentClass = nameExpr.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                    if (currentClass != null) {
                        String className = resolver.resolveClassName(currentClass.getNameAsString());
                        if (className.equals(targetClass) ||
                                resolver.isAssignableFrom(className, targetClass)) {
                            MethodDeclaration containingMethod = nameExpr.findAncestor(MethodDeclaration.class).orElse(null);
                            if (containingMethod != null) {
                                buildCallChain(chains, containingMethod, nameExpr, targetClass, targetField, visited);
                            }
                        }
                    }
                }
            });
        }

        return chains;
    }

    private String getFieldAccessClassName(FieldAccessExpr fieldAccess, ClassNameResolver resolver) {
        Expression scope = fieldAccess.getScope();
        if (scope instanceof ThisExpr) {
            // 如果是 this.field
            ClassOrInterfaceDeclaration currentClass = fieldAccess.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
            if (currentClass != null) {
                return resolver.resolveClassName(currentClass.getNameAsString());
            }
        } else if (scope instanceof NameExpr) {
            // 如果是 ClassName.field
            return resolver.resolveClassName(scope.toString());
        }
        return null;
    }

    private void buildCallChain(List<CallChain> chains, MethodDeclaration method,
                                Object callExpr, String targetClass, String targetSignature,
                                Set<String> visited) {
        ClassOrInterfaceDeclaration classDecl = method.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
        if (classDecl == null) return;

        CompilationUnit cu = method.findAncestor(CompilationUnit.class).orElse(null);
        if (cu == null) return;

        ClassNameResolver resolver = new ClassNameResolver(cu);
        String currentClass = resolver.resolveClassName(classDecl.getNameAsString());
        String currentMethod = method.getSignature().toString();
        String key = currentClass + "#" + currentMethod;

        if (!visited.add(key)) return;

        List<MethodNode> currentChain = new ArrayList<>();
        currentChain.add(new MethodNode(targetClass, targetSignature, false));

        boolean isController = isControllerClass(classDecl);
        if (isController) {
            String url = extractRequestMappingUrl(method);
            String comment = extractMethodComment(method);
            currentChain.add(new MethodNode(currentClass, currentMethod, true));
            chains.add(new CallChain(currentChain, url, currentMethod, comment));
            visited.remove(key);
            return;
        }

        currentChain.add(new MethodNode(currentClass, currentMethod, false));

        // 继续向上查找调用
        for (Map.Entry<String, CompilationUnit> entry : compilationUnits.entrySet()) {
            CompilationUnit callCu = entry.getValue();
            ClassNameResolver callResolver = new ClassNameResolver(callCu);

            callCu.findAll(MethodCallExpr.class).forEach(methodCall -> {
                String methodName = methodCall.getNameAsString();
                if (currentMethod.contains(methodName)) {
                    boolean matched = false;
                    String calledClassName = null;

                    Optional<Expression> scope = methodCall.getScope();
                    if (scope.isPresent()) {
                        Expression scopeExpr = scope.get();
                        if (scopeExpr instanceof NameExpr) {
                            Set<String> possibleClasses = callResolver.getAllPossibleClassNames(scopeExpr.toString());
                            for (String className : possibleClasses) {
                                if (className.equals(currentClass) ||
                                        callResolver.isAssignableFrom(className, currentClass)) {
                                    matched = true;
                                    calledClassName = className;
                                    break;
                                }
                            }
                        }
                    } else {
                        ClassOrInterfaceDeclaration callingClass = methodCall.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null);
                        if (callingClass != null) {
                            String className = callResolver.resolveClassName(callingClass.getNameAsString());
                            if (className.equals(currentClass) ||
                                    callResolver.isAssignableFrom(className, currentClass)) {
                                matched = true;
                                calledClassName = className;
                            }
                        }
                    }

                    if (matched && calledClassName != null) {
                        MethodDeclaration callingMethod = methodCall.findAncestor(MethodDeclaration.class).orElse(null);
                        if (callingMethod != null) {
                            buildCallChain(chains, callingMethod, methodCall, currentClass, currentMethod, visited);
                        }
                    }
                }
            });
        }

        visited.remove(key);
    }

    private boolean isControllerClass(ClassOrInterfaceDeclaration classDecl) {
        return classDecl.getAnnotations().stream()
                .anyMatch(a -> {
                    String name = a.getNameAsString();
                    return name.equals("Controller") || name.equals("RestController");
                });
    }

    private String extractRequestMappingUrl(MethodDeclaration method) {
        return method.getAnnotations().stream()
                .filter(a -> a.getNameAsString().contains("Mapping"))
                .map(a -> {
                    if (a instanceof MarkerAnnotationExpr) {
                        return "/";
                    } else if (a instanceof SingleMemberAnnotationExpr) {
                        return ((SingleMemberAnnotationExpr) a).getMemberValue().toString();
                    } else if (a instanceof NormalAnnotationExpr) {
                        return ((NormalAnnotationExpr) a).getPairs().stream()
                                .filter(p -> p.getNameAsString().equals("value") ||
                                        p.getNameAsString().equals("path"))
                                .map(p -> p.getValue().toString())
                                .findFirst()
                                .orElse("/");
                    }
                    return "/";
                })
                .findFirst()
                .orElse(null);
    }

    private String extractMethodComment(MethodDeclaration method) {
        return method.getJavadoc()
                .map(javadoc -> javadoc.getDescription().toText())
                .orElse("");
    }

    private boolean matchesSignature(String methodSignature, JavaDiffAnalyzer.MethodSignature targetSignature) {
        // 简单的签名匹配，可以根据需要扩展
        return methodSignature.contains(targetSignature.getName()) &&
                methodSignature.contains(String.join(",", targetSignature.getParameterTypes()));
    }

    public Map<String, List<CallChain>> getCallChains() {
        return new HashMap<>(callChains);
    }

    public void printCallChains() {
        System.out.println("Call Chains Analysis Results:");
        callChains.forEach((source, chains) -> {
            System.out.println("\nSource: " + source);
            if (chains.isEmpty()) {
                System.out.println("  No call chains found");
            } else {
                chains.forEach(chain -> System.out.println(chain));
            }
        });
    }
}