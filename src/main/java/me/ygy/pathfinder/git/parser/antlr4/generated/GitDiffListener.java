// Generated from GitDiff.g4 by ANTLR 4.13.1

package me.ygy.pathfinder.git.parser.antlr4.generated;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link GitDiffParser}.
 */
public interface GitDiffListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#diffFile}.
	 * @param ctx the parse tree
	 */
	void enterDiffFile(GitDiffParser.DiffFileContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#diffFile}.
	 * @param ctx the parse tree
	 */
	void exitDiffFile(GitDiffParser.DiffFileContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#fileDiff}.
	 * @param ctx the parse tree
	 */
	void enterFileDiff(GitDiffParser.FileDiffContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#fileDiff}.
	 * @param ctx the parse tree
	 */
	void exitFileDiff(GitDiffParser.FileDiffContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#fileHeader}.
	 * @param ctx the parse tree
	 */
	void enterFileHeader(GitDiffParser.FileHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#fileHeader}.
	 * @param ctx the parse tree
	 */
	void exitFileHeader(GitDiffParser.FileHeaderContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#headerContent}.
	 * @param ctx the parse tree
	 */
	void enterHeaderContent(GitDiffParser.HeaderContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#headerContent}.
	 * @param ctx the parse tree
	 */
	void exitHeaderContent(GitDiffParser.HeaderContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#filePaths}.
	 * @param ctx the parse tree
	 */
	void enterFilePaths(GitDiffParser.FilePathsContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#filePaths}.
	 * @param ctx the parse tree
	 */
	void exitFilePaths(GitDiffParser.FilePathsContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#diffContent}.
	 * @param ctx the parse tree
	 */
	void enterDiffContent(GitDiffParser.DiffContentContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#diffContent}.
	 * @param ctx the parse tree
	 */
	void exitDiffContent(GitDiffParser.DiffContentContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#hunkDiff}.
	 * @param ctx the parse tree
	 */
	void enterHunkDiff(GitDiffParser.HunkDiffContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#hunkDiff}.
	 * @param ctx the parse tree
	 */
	void exitHunkDiff(GitDiffParser.HunkDiffContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#binaryDiff}.
	 * @param ctx the parse tree
	 */
	void enterBinaryDiff(GitDiffParser.BinaryDiffContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#binaryDiff}.
	 * @param ctx the parse tree
	 */
	void exitBinaryDiff(GitDiffParser.BinaryDiffContext ctx);
	/**
	 * Enter a parse tree produced by {@link GitDiffParser#hunkHeader}.
	 * @param ctx the parse tree
	 */
	void enterHunkHeader(GitDiffParser.HunkHeaderContext ctx);
	/**
	 * Exit a parse tree produced by {@link GitDiffParser#hunkHeader}.
	 * @param ctx the parse tree
	 */
	void exitHunkHeader(GitDiffParser.HunkHeaderContext ctx);
	/**
	 * Enter a parse tree produced by the {@code addedLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 */
	void enterAddedLine(GitDiffParser.AddedLineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code addedLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 */
	void exitAddedLine(GitDiffParser.AddedLineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code removedLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 */
	void enterRemovedLine(GitDiffParser.RemovedLineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code removedLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 */
	void exitRemovedLine(GitDiffParser.RemovedLineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code contextLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 */
	void enterContextLine(GitDiffParser.ContextLineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code contextLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 */
	void exitContextLine(GitDiffParser.ContextLineContext ctx);
}