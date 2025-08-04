package me.ygy.pathfinder.code.analysis;

import org.eclipse.jdt.core.dom.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class CallGraphExtractor {

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
        String projectRoot = "/Users/yuguangyuan/code/csc/pc/csc108-etrade-licai-backend"; // 改成你的 WAR 项目路径
        String warName = "licai"; // 改成构建后生成的 WAR 名（不含 .war）

        List<String> sourcePaths = List.of(projectRoot + "/src/main/java");
        List<String> classPaths = collectWarClasspath(projectRoot, warName);

        // ===== 2. 遍历源码文件 =====
        Path sourceRoot = Paths.get(sourcePaths.get(0));
        Files.walk(sourceRoot)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(path -> processJavaFile(path, classPaths, sourcePaths));

        // ===== 3. 打印指定方法的调用链 =====
        String targetMethodName = "com.csc108.etrade.service.yingxiang.KidmService.getD8000001";
        System.out.println("=== 调用链 for " + targetMethodName + " ===");
        printCallChain(targetMethodName);
    }

    private static List<String> collectWarClasspath(String projectRoot, String warName) throws IOException {
        List<String> classpathEntries = new ArrayList<>();
        classpathEntries.add(projectRoot + "/target/classes");
        Path libPath = Paths.get(projectRoot, "target", warName, "WEB-INF", "lib");
        if (Files.exists(libPath)) {
            try (DirectoryStream<Path> jars = Files.newDirectoryStream(libPath, "*.jar")) {
                for (Path jar : jars) {
                    classpathEntries.add(jar.toAbsolutePath().toString());
                }
            }
        } else {
            System.err.println("⚠️ 依赖 jar 未找到，请确保已执行 mvn package");
        }
        return classpathEntries;
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

                        // Extract Javadoc
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

                        // Extract mapping annotations
                        if (isController) {
                            String methodLevelPath = getMappingValue(node.modifiers(), "RequestMapping", "GetMapping", "PostMapping");
                            if (!methodLevelPath.isEmpty()) {
                                currentMethodNode.mapping = (classLevelPath + methodLevelPath).replaceAll("//", "/");
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
                    return modifiers.stream()
                        .filter(m -> m instanceof IAnnotationBinding)
                        .map(m -> (IAnnotationBinding) m)
                        .anyMatch(a -> a.getAnnotationType().getName().equals(annotationName));
                }

                private String getMappingValue(List<?> modifiers, String... annotationNames) {
                    for (Object modifier : modifiers) {
                        if (modifier instanceof Annotation) {
                            Annotation annotation = (Annotation) modifier;
                            String typeName = annotation.getTypeName().getFullyQualifiedName();
                            if (Arrays.asList(annotationNames).contains(typeName)) {
                                if (annotation.isSingleMemberAnnotation()) {
                                    Expression value = ((SingleMemberAnnotation) annotation).getValue();
                                    if (value instanceof StringLiteral) {
                                        return ((StringLiteral) value).getLiteralValue();
                                    }
                                } else if (annotation.isNormalAnnotation()) {
                                    for (Object pair : ((NormalAnnotation) annotation).values()) {
                                        if (pair instanceof MemberValuePair) {
                                            MemberValuePair mvp = (MemberValuePair) pair;
                                            if (mvp.getName().getIdentifier().equals("value")) {
                                                Expression value = mvp.getValue();
                                                if (value instanceof StringLiteral) {
                                                    return ((StringLiteral) value).getLiteralValue();
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