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
                sb.append(" [URL: ").append(mapping).append("]");
            }
            if (fullyQualifiedName.contains("Controller") && comment != null && !comment.isEmpty()) {
                sb.append(" [Comment: ").append(comment.trim().replaceAll("\n", " ")).append("]");
            }
            return sb.toString();
        }
    }

    private static DirectedGraph<MethodNode, DefaultEdge> callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    private static final Map<String, MethodNode> methodNodeCache = new HashMap<>();
    private static final List<String> validPackages = List.of("com.csc108.etrade", "com.csc.wt.eagle", "com.linkstec.raptor");

    // ===== New fields for constant tracking =====
    private static final Map<String, List<MethodNode>> constantUsageMap = new HashMap<>();
    private static final List<String> TARGET_CONSTANT_PREFIXES = List.of(
            "CMD_B", "CMD_99", "CMD_00", "CMD_L", "CMD_WP", "CMD_4",
            "RZRQ_CMD_4", "RZRQ_CMD_4", "CMD_KUAS", "CMD_KFMS", "CMD_RZRQ_4","CMD_KIDM"
    );
    private static final String TARGET_CLASS_FQN = "com.linkstec.raptor.eagle.common.constant.EagleConstant";
    // local interface  constant file class
    private static final List<String> TARGET_LOCAL_CLASS_NAME_LIST = List.of("InterfaceConsts", "InterfaceCons", "RestInterfaceConsts");
    // ============================================

    public static void main(String[] args) throws IOException {
        // ===== 1. 配置 =====
        String projectRoot = "/Users/yuguangyuan/code/csc/h5/eagle-maven-online/eagle-parent"; // 改成你的多模块项目根路径
        List<String> baseModules = List.of("eagle-common", "zxjt-baseModule", "eagle-common-api");
//        List<String> targetModules = List.of(
//                "csc-web-eagle-wtportal", "csc-web-eagle-gmjj", "csc-web-eagle-mallcenter",
//                "csc-web-eagle-gmcrm", "csc-web-eagle-hyfw", "csc-web-eagle-finance",
//                "csc-web-eagle-xjgl", "csc-web-eagle-zhms"
//        );

        List<String> targetModules = List.of("csc-web-eagle-ywbl");

        for (String targetModule : targetModules) {
            System.out.println(" \n\nProcessing module: " + targetModule + " \n====================================");

            // 清理旧数据
            callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
            methodNodeCache.clear();
            constantUsageMap.clear();

            List<String> currentModules = new ArrayList<>(baseModules);
            currentModules.add(targetModule);

            List<String> sourcePaths = collectSourcePaths(projectRoot, currentModules);
            List<String> classPaths = collectMultiModuleClasspath(projectRoot, currentModules);

            // ===== 2. 遍历所有模块的源码文件 =====
            System.out.println("Starting analysis for " + targetModule + "...");
            for (String sourceRootStr : sourcePaths) {
                Path sourceRoot = Paths.get(sourceRootStr);
                if (Files.exists(sourceRoot)) {
                    Files.walk(sourceRoot)
                            .filter(p -> p.toString().endsWith(".java"))
                            .forEach(path -> processJavaFile(path, classPaths, sourcePaths));
                }
            }
            System.out.println("Analysis finished for " + targetModule + ".");

            // ===== 3. 将结果输出到文件 =====
            String moduleSuffix = targetModule.replace("csc-web-eagle-", "");
            String outputTxtFile = "constant_call_chains." + moduleSuffix + ".txt";
            String outputCsvFile = "constant_call_chains." + moduleSuffix + ".csv";

            writeConstantCallChainsToFile(outputTxtFile);
            writeConstantCallChainsToCsv(outputCsvFile);
        }
    }

    private static List<String> collectSourcePaths(String projectRoot, List<String> modules) {
        return modules.stream()
                .map(module -> Paths.get(projectRoot, module, "src", "main", "java"))
                .filter(Files::exists)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static boolean isLocalInterface(String qualifiedClassName) {
        for (String localClassName : TARGET_LOCAL_CLASS_NAME_LIST) {
            if (qualifiedClassName.contains(localClassName)) {
                return true;
            }
        }
        return false;
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
                    isController = hasAnnotation(node.modifiers(), "Controller") || hasAnnotation(node.modifiers(), "RestController");
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

                @Override
                public boolean visit(SimpleName node) {
                    if (currentMethodNode == null) return true;

                    IBinding binding = node.resolveBinding();
                    if (binding != null && binding.getKind() == IBinding.VARIABLE) {
                        IVariableBinding varBinding = (IVariableBinding) binding;
                        if (varBinding.isField() && java.lang.reflect.Modifier.isStatic(varBinding.getModifiers())) {
                            ITypeBinding declaringClass = varBinding.getDeclaringClass();
                            if (declaringClass != null && TARGET_CLASS_FQN.equals(declaringClass.getQualifiedName())) {
                                String fieldName = varBinding.getName();
                                if (TARGET_CONSTANT_PREFIXES.stream().anyMatch(fieldName::startsWith)) {
//                                    String constantFqn = declaringClass.getQualifiedName() + "." + fieldName;
                                    // fieldName去掉开头的CMD_
                                    fieldName = fieldName.replaceFirst("CMD_", "");
                                    List<MethodNode> users = constantUsageMap.computeIfAbsent(fieldName, k -> new ArrayList<>());
                                    if (!users.contains(currentMethodNode)) {
                                        users.add(currentMethodNode);
                                    }
                                }
                            } else if (isLocalInterface(declaringClass.getQualifiedName())) {
//                                String fieldName = varBinding.getName();
                                // 如果field引用的是static final String定义的变量，并且变量定义的值是以反斜杠开始的/
                                if (varBinding.getConstantValue() != null && varBinding.getConstantValue() instanceof String) {
                                    String fieldValue = (String) varBinding.getConstantValue();
                                    if (fieldValue.startsWith("/") && !fieldValue.endsWith("/")) {
                                        List<MethodNode> users = constantUsageMap.computeIfAbsent(fieldValue, k -> new ArrayList<>());
                                        if (!users.contains(currentMethodNode)) {
                                            users.add(currentMethodNode);
                                        }
                                    }
                                }
                            }
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

    private static void writeConstantCallChainsToFile(String outputPath) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Static Constant Call Chain Analysis =====\n");
        sb.append("Target Class: ").append(TARGET_CLASS_FQN).append("\n");
        sb.append("Constant Prefixes: ").append(TARGET_CONSTANT_PREFIXES).append("\n\n");

        // Invert the map to be method -> list of constants
        Map<MethodNode, List<String>> methodToConstantsMap = new HashMap<>();
        for (Map.Entry<String, List<MethodNode>> entry : constantUsageMap.entrySet()) {
            String constantName = entry.getKey();
            for (MethodNode method : entry.getValue()) {
                methodToConstantsMap.computeIfAbsent(method, k -> new ArrayList<>()).add(constantName);
            }
        }

        if (methodToConstantsMap.isEmpty()) {
            sb.append("No usages of target constants found in the analyzed scope. \n");
        } else {
            sb.append("Found ").append(methodToConstantsMap.size()).append(" methods using the target constants. \n\n");
            for (Map.Entry<MethodNode, List<String>> entry : methodToConstantsMap.entrySet()) {
                MethodNode usageMethod = entry.getKey();
                List<String> usedConstants = entry.getValue();

                sb.append("==================================================== \n");
                sb.append("Method: ").append(usageMethod.toString().replaceAll(" \n", " ")).append("\n");
                sb.append("Uses Constants: ").append(usedConstants.stream().distinct().collect(Collectors.joining(", "))).append(" \n");
                sb.append("---------------------------------------------------- \n");
                sb.append("Call Chains leading to this method: \n");

                findAndAppendCallChains(usageMethod, sb, "  ");
                sb.append(" \n");
            }
        }

        Files.writeString(Paths.get(outputPath), sb.toString());
        System.out.println("Results written to " + outputPath);
    }

    private static void findAndAppendCallChains(MethodNode targetNode, StringBuilder sb, String prefix) {
        Queue<List<MethodNode>> queue = new LinkedList<>();
        queue.add(List.of(targetNode));

        Set<String> uniqueChains = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            List<MethodNode> path = queue.poll();
            MethodNode lastMethod = path.get(path.size() - 1);

            Set<DefaultEdge> incomingEdges = callGraph.incomingEdgesOf(lastMethod);

            if (incomingEdges.isEmpty()) {
                List<MethodNode> reversedPath = new ArrayList<>(path);
                Collections.reverse(reversedPath);
                String callChainStr = reversedPath.stream()
                        .map(m -> m.toString().replaceAll(" \n", " "))
                        .collect(Collectors.joining(" -> "));
                uniqueChains.add(callChainStr);
            } else {
                for (DefaultEdge edge : incomingEdges) {
                    MethodNode source = callGraph.getEdgeSource(edge);
                    if (path.contains(source)) { // Cycle detected
                        List<MethodNode> reversedPath = new ArrayList<>(path);
                        Collections.reverse(reversedPath);
                        String callChainStr = reversedPath.stream()
                                .map(m -> m.toString().replaceAll(" \n", " "))
                                .collect(Collectors.joining(" -> "));
                        uniqueChains.add(callChainStr + " -> [Cycle detected with " + source.fullyQualifiedName + "]");
                        continue;
                    }
                    List<MethodNode> newPath = new ArrayList<>(path);
                    newPath.add(source);
                    queue.add(newPath);
                }
            }
        }

        if (uniqueChains.isEmpty()) {
            sb.append(prefix).append("[No callers found in the analyzed scope for this usage] \n");
        } else {
            for (String chain : uniqueChains) {
                sb.append(prefix).append("- ").append(chain).append(" \n");
            }
        }
    }

    private static void writeConstantCallChainsToCsv(String outputPath) throws IOException {
        StringBuilder csvSb = new StringBuilder();
        csvSb.append("constant,url,comment,controller_method\n");

        Set<String> seenConstantControllerPairs = new HashSet<>();

        for (Map.Entry<String, List<MethodNode>> entry : constantUsageMap.entrySet()) {
            String constantName = entry.getKey();
            for (MethodNode usageMethod : entry.getValue()) {
                findControllerCallChains(usageMethod, constantName, csvSb, seenConstantControllerPairs);
            }
        }

        Files.writeString(Paths.get(outputPath), csvSb.toString());
        System.out.println("CSV results written to " + outputPath);
    }

    private static void findControllerCallChains(MethodNode targetNode, String constantName, StringBuilder csvSb,
                                                 Set<String> seenConstantControllerPairs) {
        Queue<List<MethodNode>> queue = new LinkedList<>();
        queue.add(List.of(targetNode));

        while (!queue.isEmpty()) {
            List<MethodNode> path = queue.poll();
            MethodNode lastMethod = path.get(path.size() - 1);

            Set<DefaultEdge> incomingEdges = callGraph.incomingEdgesOf(lastMethod);

            if (incomingEdges.isEmpty()) {
                List<MethodNode> reversedPath = new ArrayList<>(path);
                Collections.reverse(reversedPath);

                MethodNode controllerMethod = reversedPath.get(0);
                if (controllerMethod.fullyQualifiedName.contains("Controller")) {
                    String url = controllerMethod.mapping != null ? controllerMethod.mapping.replaceAll("[\", \"]", "") : "";
                    String comment = controllerMethod.comment != null ? controllerMethod.comment.trim().replaceAll("\n", " ").replaceAll(",", ";") : "";
                    String controllerFqn = controllerMethod.fullyQualifiedName;

                    String dedupeKey = constantName + "\0" + controllerFqn;
                    if (!seenConstantControllerPairs.add(dedupeKey)) {
                        continue;
                    }

                    csvSb.append(String.format("%s,%s,%s,%s\n",
                            constantName,
                            url,
                            comment,
                            controllerFqn
                    ));
                }
            } else {
                for (DefaultEdge edge : incomingEdges) {
                    MethodNode source = callGraph.getEdgeSource(edge);
                    if (path.contains(source)) { // Cycle detected
                        continue; // Skip cycles in CSV output for simplicity
                    }
                    List<MethodNode> newPath = new ArrayList<>(path);
                    newPath.add(source);
                    queue.add(newPath);
                }
            }
        }
    }
}
