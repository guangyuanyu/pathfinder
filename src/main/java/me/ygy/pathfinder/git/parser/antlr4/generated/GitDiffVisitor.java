// Generated from GitDiff.g4 by ANTLR 4.13.1

package me.ygy.pathfinder.git.parser.antlr4.generated;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link GitDiffParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface GitDiffVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#diffFile}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiffFile(GitDiffParser.DiffFileContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#fileDiff}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileDiff(GitDiffParser.FileDiffContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#fileHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFileHeader(GitDiffParser.FileHeaderContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#headerContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHeaderContent(GitDiffParser.HeaderContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#filePaths}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilePaths(GitDiffParser.FilePathsContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#diffContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDiffContent(GitDiffParser.DiffContentContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#hunkDiff}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHunkDiff(GitDiffParser.HunkDiffContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#binaryDiff}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryDiff(GitDiffParser.BinaryDiffContext ctx);
	/**
	 * Visit a parse tree produced by {@link GitDiffParser#hunkHeader}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHunkHeader(GitDiffParser.HunkHeaderContext ctx);
	/**
	 * Visit a parse tree produced by the {@code addedLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddedLine(GitDiffParser.AddedLineContext ctx);
	/**
	 * Visit a parse tree produced by the {@code removedLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRemovedLine(GitDiffParser.RemovedLineContext ctx);
	/**
	 * Visit a parse tree produced by the {@code contextLine}
	 * labeled alternative in {@link GitDiffParser#lineContent}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContextLine(GitDiffParser.ContextLineContext ctx);
}