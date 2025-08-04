package me.ygy.pathfinder.code.analysis;

import org.eclipse.jdt.core.dom.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class MultiModuleCallGraphExtractor {

    // Custom class to hold method details
    static class MethodNode {
        String fullyQualifiedName;
        String comment;
        String mapping;

        public MethodNode(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodNode that = (MethodNode) o;
            return fullyQualifiedName.equals(that.fullyQualifiedName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fullyQualifiedName);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(fullyQualifiedName);
            if (mapping != null && !mapping.isEmpty()) {
                sb.append("\n\t[URL: ").append(mapping).append("]");
            }
            if (fullyQualifiedName.contains("Controller") && comment != null && !comment.isEmpty()) {
                sb.append("\n\t[Comment: ").append(comment.trim().replaceAll("\n", " ")).append("]");
            }
            return sb.toString();
        }
    }

    private static final DirectedGraph<MethodNode, DefaultEdge> callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private static final Map<String, MethodNode> methodNodeCache = new HashMap<>();
    private static final List<String> validPackages = List.of("com.csc108.etrade", "com.csc.wt.eagle", "com.linkstec.raptor");

    public static void main(String[] args) throws IOException {
        // ===== 1. 配置 =====
        String projectRoot = "/Users/yuguangyuan/code/csc/h5/eagle-maven-online/eagle-parent"; // 改成你的多模块项目根路径
        List<String> modules = List.of("eagle-common", "zxjt-baseModule", "csc-web-eagle-gmjj"); // 改成你的模块名列表

        List<String> sourcePaths = collectSourcePaths(projectRoot, modules);
        List<String> classPaths = collectMultiModuleClasspath(projectRoot, modules);

        // ===== 2. 遍历所有模块的源码文件 =====
        System.out.println("Starting analysis...");
        for (String sourceRootStr : sourcePaths) {
            Path sourceRoot = Paths.get(sourceRootStr);
            if (Files.exists(sourceRoot)) {
                Files.walk(sourceRoot)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(path -> processJavaFile(path, classPaths, sourcePaths));
            }
        }
        System.out.println("Analysis finished.");

        // ===== 3. 打印指定方法的调用链 =====
        String targetMethodName = "com.csc.wt.eagle.gmjj.service.KidmService.getD8000002";
        System.out.println("=== 调用链 for " + targetMethodName + " ===");
        printCallChain(targetMethodName);
    }

    private static List<String> collectSourcePaths(String projectRoot, List<String> modules) {
        return modules.stream()
                .map(module -> Paths.get(projectRoot, module, "src", "main", "java"))
                .filter(Files::exists)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static List<String> collectMultiModuleClasspath(String projectRoot, List<String> modules) throws IOException {
        Set<String> classpathEntries = new HashSet<>();
        for (String moduleName : modules) {
            Path modulePath = Paths.get(projectRoot, moduleName);

            Path classesPath = modulePath.resolve("target/classes");
            if (Files.exists(classesPath)) {
                classpathEntries.add(classesPath.toAbsolutePath().toString());
            }

            Path libPath = modulePath.resolve("target/lib");
            if (Files.exists(libPath) && Files.isDirectory(libPath)) {
                try (DirectoryStream<Path> jars = Files.newDirectoryStream(libPath, "*.jar")) {
                    for (Path jar : jars) {
                        classpathEntries.add(jar.toAbsolutePath().toString());
                    }
                }
            }

            Path moduleTargetPath = modulePath.resolve("target");
            if (Files.exists(moduleTargetPath) && Files.isDirectory(moduleTargetPath)) {
                try (DirectoryStream<Path> jars = Files.newDirectoryStream(moduleTargetPath, "*.jar")) {
                    for (Path jar : jars) {
                        classpathEntries.add(jar.toAbsolutePath().toString());
                    }
                }
            }
        }
        return new ArrayList<>(classpathEntries);
    }

    private static void processJavaFile(Path filePath, List<String> classpath, List<String> sourcepaths) {
        try {
            String code = Files.readString(filePath);
            ASTParser parser = ASTParser.newParser(AST.JLS17);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setUnitName(filePath.getFileName().toString());
            parser.setEnvironment(classpath.toArray(new String[0]), sourcepaths.toArray(new String[0]), null, true);
            parser.setSource(code.toCharArray());
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            cu.accept(new ASTVisitor() {
                private MethodNode currentMethodNode = null;
                private String classLevelPath = "";
                private boolean isController = false;

                @Override
                public boolean visit(TypeDeclaration node) {
                    isController = hasAnnotation(node.modifiers(), "Controller");
                    classLevelPath = getMappingValue(node.modifiers(), "RequestMapping");
                    return true;
                }

                @Override
                public boolean visit(MethodDeclaration node) {
                    IMethodBinding binding = node.resolveBinding();
                    if (binding != null) {
                        String fqn = binding.getDeclaringClass().getQualifiedName() + "." + binding.getName();
                        currentMethodNode = methodNodeCache.computeIfAbsent(fqn, MethodNode::new);
                        callGraph.addVertex(currentMethodNode);

                        Javadoc javadoc = node.getJavadoc();
                        if (javadoc != null) {
                            String docStr = javadoc.toString();
                            String[] docArr = docStr.split("\n");
                            String shortDoc = Arrays.stream(docArr).filter(line -> {
                                String s = line.strip();
                                return !s.startsWith("* @param") && !s.startsWith("* @return") && !s.startsWith("* @throws") && !s.startsWith("* @see") && !s.startsWith("*/") && !s.startsWith("/**") && !s.isBlank();
                            }).collect(Collectors.joining());
                            currentMethodNode.comment = shortDoc;
                        }

                        if (isController) {
                            String methodLevelPath = getMappingValue(node.modifiers(), "RequestMapping", "GetMapping", "PostMapping");
                            if (methodLevelPath != null && !methodLevelPath.isEmpty()) {
                                String combinedPath = (classLevelPath + "/" + methodLevelPath).replaceAll("/+", "/");
                                currentMethodNode.mapping = combinedPath;
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean visit(MethodInvocation node) {
                    if (currentMethodNode == null) return true;

                    IMethodBinding targetBinding = node.resolveMethodBinding();
                    if (targetBinding != null) {
                        String calleeFqn = targetBinding.getDeclaringClass().getQualifiedName() + "." + targetBinding.getName();
                        if (isValidPackage(calleeFqn)) {
                            MethodNode calleeNode = methodNodeCache.computeIfAbsent(calleeFqn, MethodNode::new);
                            callGraph.addVertex(calleeNode);
                            callGraph.addEdge(currentMethodNode, calleeNode);
                        }
                    }
                    return true;
                }

                private boolean hasAnnotation(List<?> modifiers, String annotationName) {
                    for (Object mod : modifiers) {
                        if (mod instanceof Annotation) {
                            Annotation annotation = (Annotation) mod;
                            IAnnotationBinding binding = annotation.resolveAnnotationBinding();
                            if (binding != null && binding.getAnnotationType().getName().equals(annotationName)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }

                private String getMappingValue(List<?> modifiers, String... annotationNames) {
                    for (Object modifier : modifiers) {
                        if (modifier instanceof Annotation) {
                            Annotation annotation = (Annotation) modifier;
                            IAnnotationBinding binding = annotation.resolveAnnotationBinding();
                            if (binding != null) {
                                String annotationSimpleName = binding.getAnnotationType().getName();
                                if (Arrays.asList(annotationNames).contains(annotationSimpleName)) {
                                    if (annotation.isSingleMemberAnnotation()) {
                                        return extractValueFromExpression(((SingleMemberAnnotation) annotation).getValue());
                                    } else if (annotation.isNormalAnnotation()) {
                                        for (Object pair : ((NormalAnnotation) annotation).values()) {
                                            if (pair instanceof MemberValuePair) {
                                                MemberValuePair mvp = (MemberValuePair) pair;
                                                String name = mvp.getName().getIdentifier();
                                                if (name.equals("value") || name.equals("path")) {
                                                    return extractValueFromExpression(mvp.getValue());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    return "";
                }

                private String extractValueFromExpression(Expression valueExpression) {
                    if (valueExpression instanceof StringLiteral) {
                        return ((StringLiteral) valueExpression).getLiteralValue();
                    }
                    if (valueExpression instanceof ArrayInitializer) {
                        List<?> expressions = ((ArrayInitializer) valueExpression).expressions();
                        if (!expressions.isEmpty() && expressions.get(0) instanceof StringLiteral) {
                            return ((StringLiteral) expressions.get(0)).getLiteralValue();
                        }
                    }
                    return "";
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Error processing " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isValidPackage(String className) {
        return validPackages.stream().anyMatch(className::startsWith);
    }

    private static void printCallChain(String targetMethodName) {
        MethodNode targetNode = methodNodeCache.get(targetMethodName);
        if (targetNode == null || !callGraph.containsVertex(targetNode)) {
            System.out.println("方法未找到或没有被调用。");
            return;
        }

        Queue<List<MethodNode>> queue = new LinkedList<>();
        queue.add(List.of(targetNode));

        System.out.println("----------------------------------------------------");
        while (!queue.isEmpty()) {
            List<MethodNode> path = queue.poll();
            MethodNode lastMethod = path.get(path.size() - 1);

            Set<DefaultEdge> incomingEdges = callGraph.incomingEdgesOf(lastMethod);

            if (incomingEdges.isEmpty()) {
                List<MethodNode> reversedPath = new ArrayList<>(path);
                Collections.reverse(reversedPath);
                String callChain = reversedPath.stream()
                        .map(MethodNode::toString)
                        .collect(Collectors.joining("\n  -> "));
                System.out.println(callChain);
                System.out.println("----------------------------------------------------");
            } else {
                for (DefaultEdge edge : incomingEdges) {
                    MethodNode source = callGraph.getEdgeSource(edge);
                    List<MethodNode> newPath = new ArrayList<>(path);
                    newPath.add(source);
                    queue.add(newPath);
                }
            }
        }
    }
}