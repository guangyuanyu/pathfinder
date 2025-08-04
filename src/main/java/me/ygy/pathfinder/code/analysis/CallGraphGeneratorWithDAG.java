package me.ygy.pathfinder.code.analysis;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtExecutableReference;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Deprecated
public class CallGraphGeneratorWithDAG {

    // 创建调用图
    private DefaultDirectedGraph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);

    // 用于存储方法的注解和注释信息
    private Map<String, String> methodAnnotations = new HashMap<>();

    public static void main(String[] args) {
        CallGraphGeneratorWithDAG generator = new CallGraphGeneratorWithDAG();
        // 替换为你的Maven项目的根目录路径
        String baseDir = "/Users/yuguangyuan/code/csc/pc/csc108-etrade-licai-backend";
        generator.buildGraph(baseDir);
    }


    public void buildGraph(String baseDir) {
        // 替换为你的Maven项目的src目录路径
        //String projectPath = "path/to/your/maven/project/src/main/java";
        String projectPath = Path.of(baseDir, "src/main/java").toAbsolutePath().toString();
        String classpath = Path.of(baseDir, "target/classes").toAbsolutePath().toString();
//        System.out.println(classpath);
        // 初始化Spoon Launcher
        Launcher launcher = new Launcher();
        launcher.addInputResource(projectPath);

        String libs = Path.of(baseDir, "target/licai/WEB-INF/lib").toAbsolutePath().toString();
        // 遍历libs，读取目录下所有jar包
        File directory = new File(libs);
        // 添加jar包到classpath
        File[] jars = directory.listFiles((file) -> file.isFile() && file.getName().endsWith(".jar"));
        for (File jar : jars) {
            launcher.addInputResource(jar.getAbsolutePath());
        }

//        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setSourceClasspath(new String[]{classpath});
//        launcher.buildModel();
        launcher.run();

        CtModel model = launcher.getModel();

        // 遍历所有类型
        for (CtType<?> ctType : model.getAllTypes()) {
            for (CtMethod<?> ctMethod : ctType.getMethods()) {
                // 获取方法全限定名
                String className = ctMethod.getDeclaringType().getQualifiedName();
                String methodNameWithParams = ctMethod.getSignature();
                String fullMethodName = className + "." + methodNameWithParams;

                // 收集注解和注释信息
                StringBuilder annotationDetails = new StringBuilder();
                for (CtAnnotation<?> annotation : ctMethod.getAnnotations()) {
                    annotationDetails.append("Annotation: ").append(annotation).append("\n");
                }
                if (ctMethod.getComments() != null && !ctMethod.getComments().isEmpty()) {
                    annotationDetails.append("Comment: ").append(ctMethod.getComments().get(0).toString()).append("\n");
                }
                if (annotationDetails.length() > 0) {
                    methodAnnotations.put(fullMethodName, annotationDetails.toString());
                }

                // 添加方法为顶点
                graph.addVertex(fullMethodName);

                // 遍历方法中的调用
                for (Object element : ctMethod.getElements(e -> true)) {
                    if (element instanceof CtInvocation) {
                        CtInvocation<?> invocation = (CtInvocation<?>) element;
                        CtExecutableReference<?> executable = invocation.getExecutable();
                        if (executable != null) {
                            try {
                                String calledClassName = executable.getDeclaringType().getQualifiedName();
                                String calledMethodNameWithParams = executable.getSignature();
                                String calledFullMethodName = calledClassName + "." + calledMethodNameWithParams;

                                // 添加被调用方法为顶点
                                graph.addVertex(calledFullMethodName);

                                // 添加调用关系
                                graph.addEdge(fullMethodName, calledFullMethodName);
                            } catch (Throwable ex) {
                                System.out.println("Error processing invocation: " + invocation.toString() + " - " + ex);
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        }

        // 目标方法
        String targetMethod = "com.csc108.etrade.service.kusa.BaseKuasService.getKuasL1400021(String, Map<String, String>, HttpServletRequest)";

        // 查找上游调用方并打印
        System.out.println("Upstream Callers for: " + targetMethod);
        Set<String> visited = new HashSet<>();
        findUpstreamCallers(targetMethod, 0, visited);
    }

    /**
     * 递归查找目标方法的所有上游调用方，并打印调用链。
     *
     * @param targetMethod      目标方法的全限定名
     * @param depth             当前递归深度，用于控制缩进
     * @param visited           已访问的方法集合，用于检测递归调用
     */
    public void findUpstreamCallers(String targetMethod, int depth, Set<String> visited) {
        // 如果目标方法不存在于图中，直接返回
        if (!graph.containsVertex(targetMethod)) {
            System.out.println("Method not found in the graph: " + targetMethod);
            return;
        }

        // 如果已经访问过，标记为递归调用
        if (visited.contains(targetMethod)) {
            String indent = "";
            for (int i = 0; i < depth; i++) {
                indent += "  ";
            }
            System.out.println(indent + targetMethod + " (recursive call detected)");
            return;
        }

        // 将当前方法标记为已访问
        visited.add(targetMethod);

        String indent = "";
        for (int i = 0; i < depth; i++) {
            indent += "  ";
        }

        // 打印当前方法
        System.out.println(indent + targetMethod);

        // 打印方法的注解和注释（如果存在）
        if (methodAnnotations.containsKey(targetMethod)) {
            System.out.println(indent + "  " + methodAnnotations.get(targetMethod));
        }

        // 遍历所有指向目标方法的边，查找上游调用方
        for (DefaultEdge incomingEdge : graph.incomingEdgesOf(targetMethod)) {
            String sourceMethod = graph.getEdgeSource(incomingEdge);

            // 递归查找上游调用方
            findUpstreamCallers(sourceMethod, depth + 1, visited);
        }

        // 递归结束后移除标记，允许后续其他路径重新访问
        visited.remove(targetMethod);
    }
}
