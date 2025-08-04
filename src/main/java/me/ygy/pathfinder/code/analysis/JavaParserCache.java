package me.ygy.pathfinder.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaParserCache {
    private final Map<String, CompilationUnit> cache = new HashMap<>();

    private MemoryTypeSolver memoryTypeSolver = new MemoryTypeSolver();

    private static JavaParserCache instance;

    public static JavaParserCache getParser(String projectRoot) throws IOException {
        if (instance == null) {
            instance = new JavaParserCache();
            // 配置 SymbolResolver
            instance.configureSymbolResolver(projectRoot);
        }

        return instance;
    }

    public void addToMemoryTypeSolver(String className, ResolvedReferenceTypeDeclaration typeDeclaration) {
        memoryTypeSolver.addDeclaration(className, typeDeclaration);
    }


    /**
     * 配置 SymbolResolver
     */
    private void configureSymbolResolver(String projectRoot) throws IOException {
        List<TypeSolver> typeSolvers = new ArrayList<>();
        typeSolvers.add(memoryTypeSolver);
        typeSolvers.add(new JavaParserTypeSolver(new File(projectRoot + "/src/main/java")));
        typeSolvers.add(new ReflectionTypeSolver(false));

        String libs = Path.of(projectRoot, "target/licai/WEB-INF/lib").toAbsolutePath().toString();
        // 遍历libs，读取目录下所有jar包
        File directory = new File(libs);
        if (directory.exists()) {
            // 添加jar包到classpath
            File[] jars = directory.listFiles((file) -> file.isFile() && file.getName().endsWith(".jar"));
            if (jars != null) {
                JarTypeSolver[] jarTypeSolvers = new JarTypeSolver[jars.length];
                for (int i = 0; i < jars.length; i++) {
                    jarTypeSolvers[i] = new JarTypeSolver(jars[i]);
                    typeSolvers.add(jarTypeSolvers[i]);
                }
            }
        }

        // 创建 TypeSolver，包含项目类路径
        TypeSolver typeSolver = new CombinedTypeSolver(typeSolvers.toArray(new TypeSolver[0]));

        // 设置 JavaSymbolSolver 到解析器配置
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
    }

    /**
     * Parses a Java file and caches the result.
     * If the file has already been parsed, returns the cached result.
     *
     * @param file The Java file to parse.
     * @return The parsed CompilationUnit.
     * @throws IOException If the file cannot be read.
     */
    public CompilationUnit parse(File file) throws IOException {
        String filePath = file.getCanonicalPath();
        if (cache.containsKey(filePath)) {
            return cache.get(filePath);
        }

        CompilationUnit cu = StaticJavaParser.parse(file);
        cache.put(filePath, cu);
        return cu;
    }
}
