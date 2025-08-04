package me.ygy.pathfinder.code.analysis;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import me.ygy.pathfinder.git.parser.antlr4.FileDiffVisitor.DiffEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class JavaDiffAnalyzer {
    private final String baseDir;
    private final List<DiffEntry> diffEntries;
    private final Map<String, Set<MethodSignature>> changedMethods;
    private final Map<String, Set<String>> changedFields;

    public static class MethodSignature {
        private final String name;
        private final List<String> parameterTypes;

        public MethodSignature(String name, List<String> parameterTypes) {
            this.name = name;
            this.parameterTypes = parameterTypes;
        }

        public String getName() {
            return name;
        }

        public List<String> getParameterTypes() {
            return parameterTypes;
        }

        @Override
        public String toString() {
            return name + "(" + String.join(", ", parameterTypes) + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodSignature that = (MethodSignature) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, parameterTypes);
        }
    }

    public JavaDiffAnalyzer(String baseDir, List<DiffEntry> diffEntries) {
        this.baseDir = baseDir;
        this.diffEntries = diffEntries;
        this.changedMethods = new HashMap<>();
        this.changedFields = new HashMap<>();
    }

    public void analyze() throws IOException {
        Map<String, List<DiffEntry>> fileEntries = groupByFile();

        for (Map.Entry<String, List<DiffEntry>> entry : fileEntries.entrySet()) {
            String fileName = entry.getKey();
            if (!fileName.endsWith(".java")) {
                continue;
            }

            analyzeJavaFile(fileName, entry.getValue());
        }
    }

    private Map<String, List<DiffEntry>> groupByFile() {
        Map<String, List<DiffEntry>> fileEntries = new HashMap<>();
        for (DiffEntry entry : diffEntries) {
            fileEntries.computeIfAbsent(entry.fileName, k -> new ArrayList<>())
                    .add(entry);
        }
        return fileEntries;
    }

    private void analyzeJavaFile(String fileName, List<DiffEntry> entries) throws IOException {
        Path filePath = Paths.get(baseDir, fileName);
        File javaFile = filePath.toFile();

        if (!javaFile.exists()) {
            return;
        }

        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        Set<Integer> changedLines = getChangedLines(entries);

        String packageName = cu.getPackageDeclaration()
                .map(PackageDeclaration::getNameAsString)
                .orElse("");

        for (TypeDeclaration<?> type : cu.getTypes()) {
            String fullQualifiedName = packageName.isEmpty() ?
                    type.getNameAsString() :
                    packageName + "." + type.getNameAsString();

            analyzeTypeDeclaration(type, fullQualifiedName, changedLines);
        }
    }

    private void analyzeTypeDeclaration(TypeDeclaration<?> type, String fullQualifiedName, Set<Integer> changedLines) {
        // 分析方法
        for (MethodDeclaration method : type.getMethods()) {
            if (isElementChanged(method, changedLines)) {
                MethodSignature signature = createMethodSignature(method);
                changedMethods.computeIfAbsent(fullQualifiedName, k -> new HashSet<>())
                        .add(signature);
            }
        }

        // 分析字段
        for (FieldDeclaration field : type.getFields()) {
            if (isElementChanged(field, changedLines)) {
                field.getVariables().forEach(var ->
                        changedFields.computeIfAbsent(fullQualifiedName, k -> new HashSet<>())
                                .add(var.getNameAsString())
                );
            }
        }

        // 递归分析内部类
        for (TypeDeclaration<?> innerType : type.getChildNodesByType(TypeDeclaration.class)) {
            String innerFullQualifiedName = fullQualifiedName + "." + innerType.getNameAsString();
            analyzeTypeDeclaration(innerType, innerFullQualifiedName, changedLines);
        }
    }

    private MethodSignature createMethodSignature(MethodDeclaration method) {
        String methodName = method.getNameAsString();
        List<String> parameterTypes = method.getParameters().stream()
                .map(Parameter::getType)
                .map(Object::toString)
                .collect(Collectors.toList());
        return new MethodSignature(methodName, parameterTypes);
    }

    private Set<Integer> getChangedLines(List<DiffEntry> entries) {
        Set<Integer> lines = new HashSet<>();
        for (DiffEntry entry : entries) {
            if (entry.type.equals("ADD") && entry.newLineNumber > 0) {
                lines.add(entry.newLineNumber);
            } else if (entry.type.equals("REMOVE") && entry.oldLineNumber > 0) {
                lines.add(entry.oldLineNumber);
            }
        }
        return lines;
    }

    private boolean isElementChanged(MethodDeclaration method, Set<Integer> changedLines) {
        if (method.getBegin().isPresent() && method.getEnd().isPresent()) {
            int startLine = method.getBegin().get().line;
            int endLine = method.getEnd().get().line;

            return changedLines.stream()
                    .anyMatch(line -> line >= startLine && line <= endLine);
        }
        return false;
    }

    private boolean isElementChanged(FieldDeclaration field, Set<Integer> changedLines) {
        if (field.getBegin().isPresent() && field.getEnd().isPresent()) {
            int startLine = field.getBegin().get().line;
            int endLine = field.getEnd().get().line;

            return changedLines.stream()
                    .anyMatch(line -> line >= startLine && line <= endLine);
        }
        return false;
    }

    public Map<String, Set<MethodSignature>> getChangedMethods() {
        return new HashMap<>(changedMethods);
    }

    public Map<String, Set<String>> getChangedFields() {
        return new HashMap<>(changedFields);
    }

    public void printAnalysis() {
        System.out.println("Changed Methods:");
        changedMethods.forEach((className, methods) -> {
            System.out.println("  Class: " + className);
            methods.forEach(method -> System.out.println("    - " + method));
        });

        System.out.println("\nChanged Fields:");
        changedFields.forEach((className, fields) -> {
            System.out.println("  Class: " + className);
            fields.forEach(field -> System.out.println("    - " + field));
        });
    }
}