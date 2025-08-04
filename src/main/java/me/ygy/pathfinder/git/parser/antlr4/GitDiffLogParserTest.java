package me.ygy.pathfinder.git.parser.antlr4;

import me.ygy.pathfinder.code.analysis.CallChainAnalyzer;
import me.ygy.pathfinder.code.analysis.CallGraphGeneratorWithDAG;
import me.ygy.pathfinder.code.analysis.JavaDiffAnalyzer;
import me.ygy.pathfinder.git.parser.antlr4.generated.GitDiffLexer;
import me.ygy.pathfinder.git.parser.antlr4.generated.GitDiffParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GitDiffLogParserTest {

    public static void main(String[] args) throws IOException {
        //读Git diff文件到流
        FileInputStream input = new FileInputStream("/Users/yuguangyuan/code/csc/pc/csc108-etrade-licai-backend/diff.log");
        //使用GitDiffLogParser解析
        GitDiffLexer lexer = new GitDiffLexer(CharStreams.fromStream(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        GitDiffParser parser = new GitDiffParser(tokens);

        GitDiffParser.DiffFileContext context = parser.diffFile();
        FileDiffVisitor fileDiffVisitor = new FileDiffVisitor();
        fileDiffVisitor.visit(context);

        List<FileDiffVisitor.DiffEntry> entries = fileDiffVisitor.getDiffEntries();
        // print entries
        for (FileDiffVisitor.DiffEntry entry : entries) {
            if (!entry.type.equals("CONTEXT")) {
                System.out.println(entry.fileName + " " + entry.oldLineNumber + " " + entry.newLineNumber);
            }
        }

        String baseDir = "/Users/yuguangyuan/code/csc/pc/csc108-etrade-licai-backend";
        // 创建分析器，传入Maven项目的源码根目录（通常是src/main/java）
        JavaDiffAnalyzer analyzer = new JavaDiffAnalyzer(baseDir, entries);

        // 执行分析
        analyzer.analyze();
        analyzer.printAnalysis();

        // 获取分析结果
        Map<String, Set<JavaDiffAnalyzer.MethodSignature>> changedMethods = analyzer.getChangedMethods();
        Map<String, Set<String>> changedFields = analyzer.getChangedFields();

        CallChainAnalyzer chainAnalyzer = new CallChainAnalyzer(baseDir+"/src/main/java", changedMethods, changedFields);
        chainAnalyzer.analyze();
        chainAnalyzer.printCallChains();

//        CallGraphGeneratorWithDAG generator = new CallGraphGeneratorWithDAG();
//        generator.buildGraph(baseDir);
//        for (Map.Entry<String, Set<JavaDiffAnalyzer.MethodSignature>> entry : changedMethods.entrySet()) {
//            String className = entry.getKey();
//            if (className.contains("LiCaiLoginController")) {
//                continue;
//            }
//            System.out.println(className);
//            Set<JavaDiffAnalyzer.MethodSignature> methodSignatures = entry.getValue();
//            for (JavaDiffAnalyzer.MethodSignature methodSignature : methodSignatures) {
//                String method = methodSignature.getName();
//                System.out.println(method);
//            }
//
//            break;
//        }
    }

}
