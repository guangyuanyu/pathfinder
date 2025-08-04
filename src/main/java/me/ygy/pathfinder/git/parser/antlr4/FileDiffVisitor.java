package me.ygy.pathfinder.git.parser.antlr4;

import me.ygy.pathfinder.git.parser.antlr4.generated.GitDiffBaseVisitor;
import me.ygy.pathfinder.git.parser.antlr4.generated.GitDiffParser;
import me.ygy.pathfinder.git.parser.antlr4.generated.GitDiffVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileDiffVisitor extends GitDiffBaseVisitor<Void> {
    private String currentFile;
    private HunkRange currentHunkRange;
    private List<DiffEntry> diffEntries = new ArrayList<>();
    // 用于跟踪当前行号
    private int currentOldLine = 0;
    private int currentNewLine = 0;

    // 用于解析 hunk header 的正则表达式
    private static final Pattern HUNK_PATTERN =
            Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");

    public static class HunkRange {
        public final int oldStart;
        public final int oldCount;
        public final int newStart;
        public final int newCount;

        public HunkRange(int oldStart, int oldCount, int newStart, int newCount) {
            this.oldStart = oldStart;
            this.oldCount = oldCount;
            this.newStart = newStart;
            this.newCount = newCount;
        }

        // Added for better debugging and logging
        @Override
        public String toString() {
            return String.format("@@ -%d,%d +%d,%d @@", oldStart, oldCount, newStart, newCount);
        }
    }

    public static class DiffEntry {
        public final String fileName;
        public final String type;      // "ADD", "REMOVE", "CONTEXT"
        public final String content;   // 不包含前缀的实际内容
        public final String rawLine;   // 原始行（包含前缀）
        public final int oldLineNumber; // 原始文件的行号
        public final int newLineNumber; // 新文件的行号
        public final HunkRange hunkRange; // 添加 HunkRange 字段

        public DiffEntry(String fileName, String type, String content, String rawLine,
                         int oldLineNumber, int newLineNumber, HunkRange hunkRange) {
            this.fileName = fileName;
            this.type = type;
            this.content = content;
            this.rawLine = rawLine;
            this.oldLineNumber = oldLineNumber;
            this.newLineNumber = newLineNumber;
            this.hunkRange = hunkRange;
        }

        @Override
        public String toString() {
            return String.format("File: %s, Type: %s, Old Line: %d, New Line: %d, Hunk: %s, Content: %s",
                    fileName, type, oldLineNumber, newLineNumber,
                    hunkRange != null ? hunkRange.toString() : "N/A", content);
        }
    }

    @Override
    public Void visitDiffFile(GitDiffParser.DiffFileContext ctx) {
        diffEntries.clear();
        currentFile = null;
        currentHunkRange = null;
        currentOldLine = 0;
        currentNewLine = 0;
        return visitChildren(ctx);
    }

    @Override
    public Void visitFileHeader(GitDiffParser.FileHeaderContext ctx) {
        String diffHeader = ctx.DIFF_HEADER().getText();
        String[] parts = diffHeader.split(" ");
        if (parts.length >= 4) {
            currentFile = parts[3].substring(2).trim();
        }
        return visitChildren(ctx);
    }

    @Override
    public Void visitHunkHeader(GitDiffParser.HunkHeaderContext ctx) {
        String hunkHeader = ctx.HUNK_HEADER().getText();
        Matcher matcher = HUNK_PATTERN.matcher(hunkHeader);

        if (matcher.find()) {
            currentOldLine = Integer.parseInt(matcher.group(1));
            currentNewLine = Integer.parseInt(matcher.group(3));
            int oldCount = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1;
            int newCount = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 1;

            currentHunkRange = new HunkRange(
                    currentOldLine,
                    oldCount,
                    currentNewLine,
                    newCount
            );
        }

        return visitChildren(ctx);
    }

    @Override
    public Void visitAddedLine(GitDiffParser.AddedLineContext ctx) {
        if (currentFile != null) {
            String rawLine = ctx.ADDED_LINE().getText();
            String content = rawLine.substring(1).replaceAll("\r?\n$", "");
            diffEntries.add(new DiffEntry(
                    currentFile, "ADD", content, rawLine,
                    -1,  // 添加的行在原文件中不存在
                    currentNewLine++,
                    currentHunkRange
            ));
        }
        return null;
    }

    @Override
    public Void visitRemovedLine(GitDiffParser.RemovedLineContext ctx) {
        if (currentFile != null) {
            String rawLine = ctx.REMOVED_LINE().getText();
            String content = rawLine.substring(1).replaceAll("\r?\n$", "");
            diffEntries.add(new DiffEntry(
                    currentFile, "REMOVE", content, rawLine,
                    currentOldLine++,
                    -1,  // 删除的行在新文件中不存在
                    currentHunkRange
            ));
        }
        return null;
    }

    @Override
    public Void visitContextLine(GitDiffParser.ContextLineContext ctx) {
        if (currentFile != null) {
            String rawLine = ctx.CONTEXT_LINE().getText();
            String content = rawLine.substring(1).replaceAll("\r?\n$", "");
            diffEntries.add(new DiffEntry(
                    currentFile, "CONTEXT", content, rawLine,
                    currentOldLine++,
                    currentNewLine++,
                    currentHunkRange
            ));
        }
        return null;
    }

    /**
     * 获取当前的 hunk 范围信息
     */
    public HunkRange getCurrentHunkRange() {
        return currentHunkRange;
    }

    /**
     * 获取所有的差异条目
     */
    public List<DiffEntry> getDiffEntries() {
        return new ArrayList<>(diffEntries);
    }

    /**
     * 获取特定文件的差异条目
     */
    public List<DiffEntry> getDiffEntriesForFile(String fileName) {
        return diffEntries.stream()
                .filter(entry -> entry.fileName.equals(fileName))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有涉及的文件名
     */
    public Set<String> getAffectedFiles() {
        return diffEntries.stream()
                .map(entry -> entry.fileName)
                .collect(Collectors.toSet());
    }
}