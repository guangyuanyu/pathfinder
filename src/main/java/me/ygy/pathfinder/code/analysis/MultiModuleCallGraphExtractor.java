package me.ygy.pathfinder.code.analysis;

import org.eclipse.jdt.core.dom.*;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    /** 与 {@link #constantUsageMap} 的 key 一致：EagleConstant 为去掉 CMD_ 后的名；本地 Interface 常量为路径字符串。 */
    private static final Map<String, String> constantKeyToConstantComment = new HashMap<>();
    /** {@link IVariableBinding#getKey()}（variableDeclaration）→ 由字面量或同类型内 static final 拼接求出的 URL。 */
    private static final Map<String, String> localInterfaceFieldKeyToResolvedUrl = new HashMap<>();
    /** 绑定缺失时的兜底键：类名（优先全限定名） + # + 字段名。 */
    private static final Map<String, String> localInterfaceFieldNameToResolvedUrl = new HashMap<>();
    /** 临时调试开关：打印本地接口常量 URL 解析过程。 */
    private static final boolean DEBUG_LOCAL_INTERFACE_RESOLUTION = true;
    private static final List<String> TARGET_CONSTANT_PREFIXES = List.of(
            "CMD_B", "CMD_99", "CMD_00", "CMD_L", "CMD_WP", "CMD_4",
            "RZRQ_CMD_4", "RZRQ_CMD_4", "CMD_KUAS", "CMD_KFMS", "CMD_RZRQ_4","CMD_KIDM"
    );
    private static final String TARGET_CLASS_FQN = "com.linkstec.raptor.eagle.common.constant.EagleConstant";
    // local interface  constant file class
    private static final List<String> TARGET_LOCAL_CLASS_NAME_LIST = List.of("InterfaceConsts", "InterfaceCons", "RestInterfaceConsts");
    // ============================================

    /** 与 pathfinder 模块同级的 {@code data/service-usage-analyzer.db}（运行目录一般为 pathfinder 根目录）。 */
    private static Path resolveServiceUsageAnalyzerDbPath() {
        return Paths.get(System.getProperty("user.dir"))
                .resolve("../data/service-usage-analyzer.db")
                .normalize()
                .toAbsolutePath();
    }

    private static final String UPSERT_BACKEND_SERVICE_DEFINE_SQL = """
            INSERT INTO backend_service_define (project_name, zhisheng_interface, service_url, comment, controller_method, zhisheng_comment)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(project_name, service_url, zhisheng_interface) DO UPDATE SET
              comment = excluded.comment,
              controller_method = excluded.controller_method,
              zhisheng_comment = excluded.zhisheng_comment
            """;

    public static void main(String[] args) throws Exception {
        // ===== 1. 配置 =====
        String projectRoot = "/Users/yuguangyuan/code/csc/h5/eagle-maven-online/eagle-parent"; // 改成你的多模块项目根路径
        List<String> baseModules = List.of("eagle-common", "zxjt-baseModule", "eagle-common-api");
        List<String> targetModules = List.of(
                "csc-web-eagle-wtportal", "csc-web-eagle-gmjj", "csc-web-eagle-mallcenter",
                "csc-web-eagle-gmcrm", "csc-web-eagle-hyfw", "csc-web-eagle-finance",
                "csc-web-eagle-xjgl", "csc-web-eagle-zhms", "csc-web-eagle-ywbl", "csc-web-eagle-activity"
        );

//        List<String> targetModules = List.of("csc-web-eagle-ywbl");

        for (String targetModule : targetModules) {
            System.out.println(" \n\nProcessing module: " + targetModule + " \n====================================");

            // 清理旧数据
            callGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
            methodNodeCache.clear();
            constantUsageMap.clear();
            constantKeyToConstantComment.clear();
            localInterfaceFieldKeyToResolvedUrl.clear();
            localInterfaceFieldNameToResolvedUrl.clear();

            List<String> currentModules = new ArrayList<>(baseModules);
            currentModules.add(targetModule);

            List<String> sourcePaths = collectSourcePaths(projectRoot, currentModules);
            List<String> classPaths = collectMultiModuleClasspath(projectRoot, currentModules);

            // ===== 2. 先索引常量定义（含 RestInterfaceConsts 中 A + "/path" 拼接），再分析引用与调用图 =====
            System.out.println("Indexing constant definitions for " + targetModule + "...");
            for (String sourceRootStr : sourcePaths) {
                Path sourceRoot = Paths.get(sourceRootStr);
                if (Files.exists(sourceRoot)) {
                    Files.walk(sourceRoot)
                            .filter(p -> p.toString().endsWith(".java"))
                            .forEach(path -> indexConstantDefinitions(path, classPaths, sourcePaths));
                }
            }
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
            writeConstantCallChainsToCsvAndSqlite(outputCsvFile, targetModule, resolveServiceUsageAnalyzerDbPath());
        }
    }

    static final class BackendServiceRow {
        final String zhishengInterface;
        final String serviceUrl;
        final String comment;
        final String controllerMethod;
        final String zhishengComment;

        BackendServiceRow(String zhishengInterface, String serviceUrl, String comment, String controllerMethod,
                           String zhishengComment) {
            this.zhishengInterface = zhishengInterface;
            this.serviceUrl = serviceUrl;
            this.comment = comment;
            this.controllerMethod = controllerMethod;
            this.zhishengComment = zhishengComment;
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
        if (qualifiedClassName == null || qualifiedClassName.isBlank()) {
            return false;
        }
        for (String localClassName : TARGET_LOCAL_CLASS_NAME_LIST) {
            if (qualifiedClassName.contains(localClassName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLocalInterfaceTypeName(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        for (String localClassName : TARGET_LOCAL_CLASS_NAME_LIST) {
            if (typeName.contains(localClassName)) {
                return true;
            }
        }
        return false;
    }

    private static String buildLocalInterfaceFallbackKey(String ownerName, String fieldName) {
        if (ownerName == null || ownerName.isBlank() || fieldName == null || fieldName.isBlank()) {
            return null;
        }
        return ownerName + "#" + fieldName;
    }

    private static boolean shouldDebugLocalInterface(String ownerName, String fieldName) {
        if (!DEBUG_LOCAL_INTERFACE_RESOLUTION) {
            return false;
        }
        String owner = ownerName == null ? "" : ownerName;
        String field = fieldName == null ? "" : fieldName;
        return owner.contains("RestInterfaceConsts")
                || owner.contains("InterfaceConsts")
                || owner.contains("InterfaceCons")
                || field.contains("YGT")
                || field.contains("E0010001");
    }

    private static void debugLocalInterface(String ownerName, String fieldName, String message) {
        if (!shouldDebugLocalInterface(ownerName, fieldName)) {
            return;
        }
//        System.out.println("[local-interface-debug] " + ownerName + "#" + fieldName + " -> " + message);
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

    private static boolean isLocalInterfacePathConstant(String s) {
        return s != null && s.startsWith("/") && !s.endsWith("/");
    }

    /**
     * 本地接口常量求值时允许中间态字符串（例如 ""、"/BASE"），
     * 只有在真正登记为接口常量时才要求满足 {@link #isLocalInterfacePathConstant(String)}。
     */
    private static boolean isResolvableLocalInterfaceString(String s) {
        return s != null;
    }

    /**
     * 首轮扫描：解析 EagleConstant / Interface* / RestInterfaceConsts 等字段定义，
     * 填充 {@link #constantKeyToConstantComment} 与 {@link #localInterfaceFieldKeyToResolvedUrl}
     *（支持 {@code BASE + "/sub"} 形式，{@code getConstantValue()} 常为 null 时由 AST 求值）。
     */
    private static void indexConstantDefinitions(Path filePath, List<String> classpath, List<String> sourcepaths) {
        try {
            String code = Files.readString(filePath);
            final String[] sourceLines = code.split("\\R", -1);
            ASTParser parser = ASTParser.newParser(AST.JLS17);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
            parser.setUnitName(filePath.getFileName().toString());
            parser.setEnvironment(classpath.toArray(new String[0]), sourcepaths.toArray(new String[0]), null, true);
            parser.setSource(code.toCharArray());
            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            Map<String, String> fileMemo = new HashMap<>();
            cu.accept(new ASTVisitor() {
                @Override
                public boolean visit(FieldDeclaration node) {
                    if (!java.lang.reflect.Modifier.isStatic(node.getModifiers())
                            || !java.lang.reflect.Modifier.isFinal(node.getModifiers())) {
                        return true;
                    }
                    ASTNode parent = node.getParent();
                    if (!(parent instanceof TypeDeclaration typeNode)) {
                        return true;
                    }
                    ITypeBinding classBinding = typeNode.resolveBinding();
                    String qn = classBinding != null ? classBinding.getQualifiedName() : typeNode.getName().getIdentifier();
                    for (Object o : node.fragments()) {
                        if (!(o instanceof VariableDeclarationFragment frag)) {
                            continue;
                        }
                        IVariableBinding vb = frag.resolveBinding();
                        String defComment = extractConstantDefinitionComment(cu, code, sourceLines, node, frag);
                        if (classBinding != null && TARGET_CLASS_FQN.equals(qn) && vb != null) {
                            String fname = vb.getName();
                            if (TARGET_CONSTANT_PREFIXES.stream().anyMatch(fname::startsWith)) {
                                String key = fname.replaceFirst("CMD_", "");
                                mergeConstantDefinitionComment(key, defComment);
                            }
                        } else if (isLocalInterface(qn) || isLocalInterfaceTypeName(typeNode.getName().getIdentifier())) {
                            debugLocalInterface(qn, frag.getName().getIdentifier(),
                                    "index start, initializer=" + (frag.getInitializer() == null ? "<null>" : frag.getInitializer()));
                            String url = resolveLocalInterfaceFieldUrlFromFragment(
                                    frag, classBinding, typeNode, cu, fileMemo, new HashSet<>());
                            debugLocalInterface(qn, frag.getName().getIdentifier(), "index resolved url=" + url);
                            if (isLocalInterfacePathConstant(url)) {
                                if (vb != null) {
                                    localInterfaceFieldKeyToResolvedUrl.put(vb.getVariableDeclaration().getKey(), url);
                                }
                                String fallbackKey = buildLocalInterfaceFallbackKey(
                                        classBinding != null ? classBinding.getQualifiedName() : typeNode.getName().getIdentifier(),
                                        frag.getName().getIdentifier());
                                if (fallbackKey != null) {
                                    localInterfaceFieldNameToResolvedUrl.put(fallbackKey, url);
                                }
                                mergeConstantDefinitionComment(url, defComment);
                            }
                        }
                    }
                    return true;
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Error indexing constants " + filePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static VariableDeclarationFragment findFragmentByNameInClass(TypeDeclaration type, String fieldName) {
        if (type == null || fieldName == null) {
            return null;
        }
        for (FieldDeclaration fd : type.getFields()) {
            for (Object o : fd.fragments()) {
                if (o instanceof VariableDeclarationFragment vdf
                        && fieldName.equals(vdf.getName().getIdentifier())) {
                    return vdf;
                }
            }
        }
        return null;
    }

    /** 供顶层 {@code visit(FieldDeclaration)} 直接使用，已知 fragment / 所在 type / classBinding。 */
    private static String resolveLocalInterfaceFieldUrlFromFragment(
            VariableDeclarationFragment frag, ITypeBinding owningClass, TypeDeclaration owningType,
            CompilationUnit cu, Map<String, String> memo, Set<String> stack) {
        IVariableBinding vb = frag.resolveBinding();
        String ownerName = owningClass != null ? owningClass.getQualifiedName() : owningType.getName().getIdentifier();
        String fieldName = frag.getName().getIdentifier();
        String k = vb != null ? vb.getVariableDeclaration().getKey() : buildLocalInterfaceFallbackKey(ownerName, frag.getName().getIdentifier());
        if (memo.containsKey(k)) {
            debugLocalInterface(ownerName, fieldName, "memo hit, key=" + k + ", value=" + memo.get(k));
            return memo.get(k);
        }
        if (!stack.add(k)) {
            debugLocalInterface(ownerName, fieldName, "cycle detected, key=" + k);
            return null;
        }
        try {
            if (vb != null) {
                Object cv = vb.getConstantValue();
                if (cv instanceof String s && isLocalInterfacePathConstant(s)) {
                    memo.put(k, s);
                    debugLocalInterface(ownerName, fieldName, "constantValue hit=" + s);
                    return s;
                }
                debugLocalInterface(ownerName, fieldName, "constantValue=" + cv);
            }
            Expression init = frag.getInitializer();
            if (init == null) {
                debugLocalInterface(ownerName, fieldName, "initializer is null");
                return null;
            }
            debugLocalInterface(ownerName, fieldName, "evaluate initializer=" + init);
            String evaluated = evalLocalInterfaceStringInitializer(init, owningClass, owningType, cu, memo, stack);
            if (isResolvableLocalInterfaceString(evaluated)) {
                memo.put(k, evaluated);
                debugLocalInterface(ownerName, fieldName, "evaluated success=" + evaluated);
                return evaluated;
            }
            debugLocalInterface(ownerName, fieldName, "evaluated result invalid=" + evaluated);
            return null;
        } finally {
            stack.remove(k);
        }
    }

    private static String evalLocalInterfaceStringInitializer(
            Expression expr, ITypeBinding owningClass, TypeDeclaration owningType, CompilationUnit cu,
            Map<String, String> memo, Set<String> stack) {
        if (expr == null) {
            return null;
        }
        if (expr instanceof StringLiteral sl) {
            return sl.getLiteralValue();
        }
        if (expr instanceof CharacterLiteral cl) {
            return String.valueOf(cl.charValue());
        }
        if (expr instanceof NumberLiteral nl) {
            return nl.getToken();
        }
        if (expr instanceof BooleanLiteral bl) {
            return Boolean.toString(bl.booleanValue());
        }
        if (expr instanceof ParenthesizedExpression pe) {
            return evalLocalInterfaceStringInitializer(pe.getExpression(), owningClass, owningType, cu, memo, stack);
        }
        if (expr instanceof CastExpression ce) {
            return evalLocalInterfaceStringInitializer(ce.getExpression(), owningClass, owningType, cu, memo, stack);
        }
        if (expr instanceof InfixExpression ie && ie.getOperator() == InfixExpression.Operator.PLUS) {
            StringBuilder sb = new StringBuilder();
            String left = evalLocalInterfaceStringInitializer(ie.getLeftOperand(), owningClass, owningType, cu, memo, stack);
            if (left == null) return null;
            sb.append(left);
            String right = evalLocalInterfaceStringInitializer(ie.getRightOperand(), owningClass, owningType, cu, memo, stack);
            if (right == null) return null;
            sb.append(right);
            for (Object ex : ie.extendedOperands()) {
                String p = evalLocalInterfaceStringInitializer((Expression) ex, owningClass, owningType, cu, memo, stack);
                if (p == null) return null;
                sb.append(p);
            }
            debugLocalInterface(owningClass != null ? owningClass.getQualifiedName() : owningType.getName().getIdentifier(),
                    "<expr>", "infix '" + ie + "' => " + sb);
            return sb.toString();
        }
        if (expr instanceof Name name) {
            return resolveNameValueInClass(name, owningClass, owningType, cu, memo, stack);
        }
        return null;
    }

    /**
     * 解析 {@code Name} 引用的常量值：
     * <ol>
     *   <li>优先用 {@link IVariableBinding#getConstantValue()}</li>
     *   <li>其次按名字在同一 {@link TypeDeclaration} 内递归求值（兼容绑定不完整的情况）</li>
     *   <li>最后按绑定所在类型是否为同一 qualifiedName 递归</li>
     * </ol>
     */
    private static String resolveNameValueInClass(
            Name name, ITypeBinding owningClass, TypeDeclaration owningType, CompilationUnit cu,
            Map<String, String> memo, Set<String> stack) {
        String ownerName = owningClass != null ? owningClass.getQualifiedName() : owningType.getName().getIdentifier();
        IBinding binding = name.resolveBinding();
        if (binding instanceof IVariableBinding refVb && refVb.isField()
                && java.lang.reflect.Modifier.isStatic(refVb.getModifiers())) {
            Object cv = refVb.getConstantValue();
            if (cv instanceof String s) {
                debugLocalInterface(ownerName, refVb.getName(), "name '" + name + "' constantValue=" + s);
                return s;
            }
            ITypeBinding refDecl = refVb.getDeclaringClass();
            String refQn = refDecl == null ? null : refDecl.getQualifiedName();
            String ownQn = owningClass == null ? null : owningClass.getQualifiedName();
            if ((refQn != null && refQn.equals(ownQn))
                    || (refDecl != null && owningType != null
                    && refDecl.getName().equals(owningType.getName().getIdentifier()))) {
                VariableDeclarationFragment refFrag = findFragmentByNameInClass(owningType, refVb.getName());
                if (refFrag != null) {
                    debugLocalInterface(ownerName, refVb.getName(), "name '" + name + "' recurse by binding");
                    return resolveLocalInterfaceFieldUrlFromFragment(refFrag, owningClass, owningType, cu, memo, stack);
                }
            }
            debugLocalInterface(ownerName, refVb.getName(), "name '" + name + "' unresolved by binding");
            return null;
        }
        // 绑定缺失时按简单名在同类中找
        String simple = name instanceof SimpleName sn ? sn.getIdentifier()
                : name instanceof QualifiedName qn ? qn.getName().getIdentifier() : null;
        if (simple != null) {
            VariableDeclarationFragment refFrag = findFragmentByNameInClass(owningType, simple);
            if (refFrag != null) {
                debugLocalInterface(ownerName, simple, "name '" + name + "' recurse by simple name");
                return resolveLocalInterfaceFieldUrlFromFragment(refFrag, owningClass, owningType, cu, memo, stack);
            }
        }
        debugLocalInterface(ownerName, simple, "name '" + name + "' unresolved");
        return null;
    }

    private static String resolveLocalInterfaceUrlForUsage(IVariableBinding varBinding) {
        Object cv = varBinding.getConstantValue();
        if (cv instanceof String s && isLocalInterfacePathConstant(s)) {
            return s;
        }
        String k = varBinding.getVariableDeclaration().getKey();
        String resolved = localInterfaceFieldKeyToResolvedUrl.get(k);
        if (resolved != null) {
            return resolved;
        }
        ITypeBinding declaringClass = varBinding.getDeclaringClass();
        String fallbackKey = buildLocalInterfaceFallbackKey(
                declaringClass != null ? declaringClass.getQualifiedName() : null,
                varBinding.getName());
        if (fallbackKey != null) {
            return localInterfaceFieldNameToResolvedUrl.get(fallbackKey);
        }
        return null;
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
                // 使用栈保存外层类的状态，避免嵌套(内部)类访问时覆盖外层类的 isController / classLevelPath。
                // 场景: 如果 Controller 的某个方法定义在内部类之后（例如 GateController.fetchRiskTstSbj），
                // 而内部类本身不是 Controller，退出内部类后需要恢复外层 Controller 的上下文，否则该方法的 URL 无法提取。
                private final Deque<Boolean> isControllerStack = new ArrayDeque<>();
                private final Deque<String> classLevelPathStack = new ArrayDeque<>();

                @Override
                public boolean visit(TypeDeclaration node) {
                    isControllerStack.push(isController);
                    classLevelPathStack.push(classLevelPath);

                    boolean nodeIsController = hasAnnotation(node.modifiers(), "Controller") || hasAnnotation(node.modifiers(), "RestController");
                    String nodeClassLevelPath = getMappingValue(node.modifiers(), "RequestMapping", "GetMapping", "PostMapping");
                    if (nodeIsController) {
                        isController = true;
                        classLevelPath = nodeClassLevelPath;
                    } else {
                        isController = false;
                        classLevelPath = "";
                    }
                    return true;
                }

                @Override
                public void endVisit(TypeDeclaration node) {
                    if (!isControllerStack.isEmpty()) {
                        isController = isControllerStack.pop();
                    }
                    if (!classLevelPathStack.isEmpty()) {
                        classLevelPath = classLevelPathStack.pop();
                    }
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
                            boolean hasMappingAnno = hasAnnotation(node.modifiers(), "RequestMapping")
                                    || hasAnnotation(node.modifiers(), "GetMapping")
                                    || hasAnnotation(node.modifiers(), "PostMapping");
                            if (hasMappingAnno) {
                                String methodLevelPath = getMappingValue(node.modifiers(), "RequestMapping", "GetMapping", "PostMapping");
                                String basePath = classLevelPath == null ? "" : classLevelPath;
                                String subPath = methodLevelPath == null ? "" : methodLevelPath;
                                // 如果方法级 @RequestMapping 没有 value/path（或为空），则直接使用类级 @RequestMapping 的值作为 URL
                                String combinedPath = subPath.isEmpty() ? basePath : (basePath + "/" + subPath);
                                combinedPath = combinedPath.replaceAll("/+", "/");
                                if (!combinedPath.isEmpty()) {
                                    currentMethodNode.mapping = combinedPath;
                                }
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
                                String fieldValue = resolveLocalInterfaceUrlForUsage(varBinding.getVariableDeclaration());
                                if (fieldValue != null) {
                                    List<MethodNode> users = constantUsageMap.computeIfAbsent(fieldValue, k -> new ArrayList<>());
                                    if (!users.contains(currentMethodNode)) {
                                        users.add(currentMethodNode);
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

    private static void mergeConstantDefinitionComment(String key, String comment) {
        if (comment == null || comment.isBlank()) {
            constantKeyToConstantComment.putIfAbsent(key, "");
            return;
        }
        constantKeyToConstantComment.merge(key, comment, (oldV, newV) -> {
            if (oldV == null || oldV.isBlank()) {
                return newV;
            }
            if (oldV.equals(newV)) {
                return oldV;
            }
            return oldV + " | " + newV;
        });
    }

    /**
     * 常量定义处：优先取定义行（含分号行）上的行尾/行内注释；若无则取该行往上数第 1、2 行中的注释。
     */
    private static String extractConstantDefinitionComment(CompilationUnit cu, String fullSource, String[] sourceLines,
                                                           FieldDeclaration field, VariableDeclarationFragment frag) {
        int start = frag.getStartPosition();
        int end = start + frag.getLength();
        int semi = -1;
        for (int i = end - 1; i >= start; i--) {
            if (fullSource.charAt(i) == ';') {
                semi = i;
                break;
            }
        }
        if (semi < 0) {
            semi = Math.max(start, end - 1);
        }
        int line1Based = cu.getLineNumber(semi);
        if (line1Based < 1) {
            return "";
        }
        int idx = line1Based - 1;
        if (idx < sourceLines.length) {
            String defLine = sourceLines[idx];
            String inline = extractInlineCommentOnConstantDefinitionLine(defLine);
            if (!inline.isEmpty()) {
                return normalizeConstantCommentText(inline);
            }
        }
        for (int d = 1; d <= 2; d++) {
            int li = line1Based - 1 - d;
            if (li >= 0 && li < sourceLines.length) {
                String near = extractCommentFromNearbySourceLine(sourceLines[li]);
                if (!near.isEmpty()) {
                    return normalizeConstantCommentText(near);
                }
            }
        }
        return "";
    }

    // 定义行：分号后的行尾 // 或块注释；或分号前同一行上的块注释 /* ... */；否则取整行最后一个 // 之后内容。
    private static String extractInlineCommentOnConstantDefinitionLine(String line) {
        int semi = line.lastIndexOf(';');
        if (semi >= 0) {
            String after = line.substring(semi + 1).trim();
            if (after.startsWith("//")) {
                return after.substring(2).trim();
            }
            if (after.startsWith("/*")) {
                int close = after.indexOf("*/", 2);
                if (close > 2) {
                    return after.substring(2, close).trim();
                }
            }
            String before = line.substring(0, semi);
            String block = lastBlockCommentOnLine(before);
            if (!block.isEmpty()) {
                return block;
            }
        }
        int slash = line.lastIndexOf("//");
        if (slash >= 0) {
            return line.substring(slash + 2).trim();
        }
        return "";
    }

    private static String lastBlockCommentOnLine(String beforeSemi) {
        int open = beforeSemi.lastIndexOf("/*");
        if (open < 0) {
            return "";
        }
        int close = beforeSemi.indexOf("*/", open + 2);
        if (close > open) {
            return beforeSemi.substring(open + 2, close).trim();
        }
        return "";
    }

    private static String extractCommentFromNearbySourceLine(String line) {
        String t = line.trim();
        if (t.startsWith("//")) {
            return t.substring(2).trim();
        }
        if (t.startsWith("/**") && t.contains("*/")) {
            int end = t.indexOf("*/", 3);
            return t.substring(3, end).replace('*', ' ').replaceAll("\\s+", " ").trim();
        }
        if (t.startsWith("/*") && t.contains("*/")) {
            int end = t.indexOf("*/", 2);
            return t.substring(2, end).replace('*', ' ').trim();
        }
        int slash = line.indexOf("//");
        if (slash >= 0) {
            return line.substring(slash + 2).trim();
        }
        return "";
    }

    private static String normalizeConstantCommentText(String raw) {
        return raw.replace('\r', ' ').replace('\n', ' ').trim().replaceAll(",", ";");
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

    private static void ensureBackendServiceDefineTable(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                    CREATE TABLE IF NOT EXISTS backend_service_define (
                      project_name TEXT NOT NULL,
                      zhisheng_interface TEXT NOT NULL,
                      service_url TEXT NOT NULL,
                      comment TEXT,
                      controller_method TEXT NOT NULL,
                      zhisheng_comment TEXT,
                      UNIQUE(project_name, service_url, zhisheng_interface)
                    )
                    """);
        }
        migrateZhishengCommentColumn(conn);
    }

    /** 新列名 zhisheng_comment；旧库若仅有 constant_comment 则重命名。 */
    private static void migrateZhishengCommentColumn(Connection conn) throws SQLException {
        if (tableHasColumn(conn, "backend_service_define", "zhisheng_comment")) {
            return;
        }
        try (Statement st = conn.createStatement()) {
            if (tableHasColumn(conn, "backend_service_define", "constant_comment")) {
                st.execute("ALTER TABLE backend_service_define RENAME COLUMN constant_comment TO zhisheng_comment");
            } else {
                st.execute("ALTER TABLE backend_service_define ADD COLUMN zhisheng_comment TEXT");
            }
        }
    }

    private static boolean tableHasColumn(Connection conn, String table, String column) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final String DELETE_BACKEND_SERVICE_BY_PROJECT_SQL =
            "DELETE FROM backend_service_define WHERE project_name = ?";

    private static void upsertBackendServiceRows(String projectName, Path dbPath, List<BackendServiceRow> rows)
            throws SQLException {
        try {
            if (!Files.exists(dbPath.getParent())) {
                Files.createDirectories(dbPath.getParent());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url)) {
            ensureBackendServiceDefineTable(conn);
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(DELETE_BACKEND_SERVICE_BY_PROJECT_SQL)) {
                    del.setString(1, projectName);
                    del.executeUpdate();
                }
                if (!rows.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(UPSERT_BACKEND_SERVICE_DEFINE_SQL)) {
                        for (BackendServiceRow row : rows) {
                            ps.setString(1, projectName);
                            ps.setString(2, row.zhishengInterface);
                            ps.setString(3, row.serviceUrl);
                            ps.setString(4, row.comment);
                            ps.setString(5, row.controllerMethod);
                            ps.setString(6, row.zhishengComment);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static void writeConstantCallChainsToCsvAndSqlite(String outputPath, String projectName, Path dbPath)
            throws IOException, SQLException {
        List<BackendServiceRow> rows = new ArrayList<>();
        Set<String> seenConstantControllerPairs = new HashSet<>();

        for (Map.Entry<String, List<MethodNode>> entry : constantUsageMap.entrySet()) {
            String constantName = entry.getKey();
            for (MethodNode usageMethod : entry.getValue()) {
                findControllerCallChains(usageMethod, constantName, rows, seenConstantControllerPairs);
            }
        }

        StringBuilder csvSb = new StringBuilder();
        csvSb.append("zhisheng_interface,service_url,comment,controller_method,zhisheng_comment\n");
        for (BackendServiceRow row : rows) {
            csvSb.append(String.format("%s,%s,%s,%s,%s\n",
                    row.zhishengInterface,
                    row.serviceUrl,
                    row.comment,
                    row.controllerMethod,
                    row.zhishengComment));
        }

        Files.writeString(Paths.get(outputPath), csvSb.toString());
        System.out.println("CSV results written to " + outputPath);

        upsertBackendServiceRows(projectName, dbPath, rows);
        System.out.println("SQLite upsert finished: " + dbPath + " (rows=" + rows.size() + ")");
    }

    private static void findControllerCallChains(MethodNode targetNode, String constantName, List<BackendServiceRow> rows,
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

                    String zhishengComment = constantKeyToConstantComment.getOrDefault(constantName, "");
                    rows.add(new BackendServiceRow(constantName, url, comment, controllerFqn, zhishengComment));
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
