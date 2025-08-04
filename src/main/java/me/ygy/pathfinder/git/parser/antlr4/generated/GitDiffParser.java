// Generated from GitDiff.g4 by ANTLR 4.13.1

package me.ygy.pathfinder.git.parser.antlr4.generated;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class GitDiffParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		DIFF_HEADER=1, FILE_MODE_HEADER=2, INDEX_HEADER=3, OLD_FILE_HEADER=4, 
		NEW_FILE_HEADER=5, BINARY_FILES=6, BINARY_STATUS=7, HUNK_HEADER=8, ADDED_LINE=9, 
		REMOVED_LINE=10, CONTEXT_LINE=11, NO_NEWLINE=12, WS=13;
	public static final int
		RULE_diffFile = 0, RULE_fileDiff = 1, RULE_fileHeader = 2, RULE_headerContent = 3, 
		RULE_filePaths = 4, RULE_diffContent = 5, RULE_hunkDiff = 6, RULE_binaryDiff = 7, 
		RULE_hunkHeader = 8, RULE_lineContent = 9;
	private static String[] makeRuleNames() {
		return new String[] {
			"diffFile", "fileDiff", "fileHeader", "headerContent", "filePaths", "diffContent", 
			"hunkDiff", "binaryDiff", "hunkHeader", "lineContent"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "DIFF_HEADER", "FILE_MODE_HEADER", "INDEX_HEADER", "OLD_FILE_HEADER", 
			"NEW_FILE_HEADER", "BINARY_FILES", "BINARY_STATUS", "HUNK_HEADER", "ADDED_LINE", 
			"REMOVED_LINE", "CONTEXT_LINE", "NO_NEWLINE", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "GitDiff.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public GitDiffParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DiffFileContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(GitDiffParser.EOF, 0); }
		public List<FileDiffContext> fileDiff() {
			return getRuleContexts(FileDiffContext.class);
		}
		public FileDiffContext fileDiff(int i) {
			return getRuleContext(FileDiffContext.class,i);
		}
		public DiffFileContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_diffFile; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterDiffFile(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitDiffFile(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitDiffFile(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DiffFileContext diffFile() throws RecognitionException {
		DiffFileContext _localctx = new DiffFileContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_diffFile);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(23);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DIFF_HEADER) {
				{
				{
				setState(20);
				fileDiff();
				}
				}
				setState(25);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(26);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FileDiffContext extends ParserRuleContext {
		public FileHeaderContext fileHeader() {
			return getRuleContext(FileHeaderContext.class,0);
		}
		public DiffContentContext diffContent() {
			return getRuleContext(DiffContentContext.class,0);
		}
		public FileDiffContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fileDiff; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterFileDiff(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitFileDiff(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitFileDiff(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileDiffContext fileDiff() throws RecognitionException {
		FileDiffContext _localctx = new FileDiffContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_fileDiff);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(28);
			fileHeader();
			setState(29);
			diffContent();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FileHeaderContext extends ParserRuleContext {
		public TerminalNode DIFF_HEADER() { return getToken(GitDiffParser.DIFF_HEADER, 0); }
		public List<HeaderContentContext> headerContent() {
			return getRuleContexts(HeaderContentContext.class);
		}
		public HeaderContentContext headerContent(int i) {
			return getRuleContext(HeaderContentContext.class,i);
		}
		public FileHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_fileHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterFileHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitFileHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitFileHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FileHeaderContext fileHeader() throws RecognitionException {
		FileHeaderContext _localctx = new FileHeaderContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_fileHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(31);
			match(DIFF_HEADER);
			setState(35);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 28L) != 0)) {
				{
				{
				setState(32);
				headerContent();
				}
				}
				setState(37);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HeaderContentContext extends ParserRuleContext {
		public TerminalNode INDEX_HEADER() { return getToken(GitDiffParser.INDEX_HEADER, 0); }
		public TerminalNode FILE_MODE_HEADER() { return getToken(GitDiffParser.FILE_MODE_HEADER, 0); }
		public FilePathsContext filePaths() {
			return getRuleContext(FilePathsContext.class,0);
		}
		public HeaderContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_headerContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterHeaderContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitHeaderContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitHeaderContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HeaderContentContext headerContent() throws RecognitionException {
		HeaderContentContext _localctx = new HeaderContentContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_headerContent);
		try {
			setState(41);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case INDEX_HEADER:
				enterOuterAlt(_localctx, 1);
				{
				setState(38);
				match(INDEX_HEADER);
				}
				break;
			case FILE_MODE_HEADER:
				enterOuterAlt(_localctx, 2);
				{
				setState(39);
				match(FILE_MODE_HEADER);
				}
				break;
			case OLD_FILE_HEADER:
				enterOuterAlt(_localctx, 3);
				{
				setState(40);
				filePaths();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FilePathsContext extends ParserRuleContext {
		public TerminalNode OLD_FILE_HEADER() { return getToken(GitDiffParser.OLD_FILE_HEADER, 0); }
		public TerminalNode NEW_FILE_HEADER() { return getToken(GitDiffParser.NEW_FILE_HEADER, 0); }
		public FilePathsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_filePaths; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterFilePaths(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitFilePaths(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitFilePaths(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FilePathsContext filePaths() throws RecognitionException {
		FilePathsContext _localctx = new FilePathsContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_filePaths);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(43);
			match(OLD_FILE_HEADER);
			setState(44);
			match(NEW_FILE_HEADER);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class DiffContentContext extends ParserRuleContext {
		public HunkDiffContext hunkDiff() {
			return getRuleContext(HunkDiffContext.class,0);
		}
		public BinaryDiffContext binaryDiff() {
			return getRuleContext(BinaryDiffContext.class,0);
		}
		public DiffContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_diffContent; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterDiffContent(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitDiffContent(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitDiffContent(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DiffContentContext diffContent() throws RecognitionException {
		DiffContentContext _localctx = new DiffContentContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_diffContent);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(48);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case HUNK_HEADER:
				{
				setState(46);
				hunkDiff();
				}
				break;
			case BINARY_FILES:
			case BINARY_STATUS:
				{
				setState(47);
				binaryDiff();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HunkDiffContext extends ParserRuleContext {
		public List<HunkHeaderContext> hunkHeader() {
			return getRuleContexts(HunkHeaderContext.class);
		}
		public HunkHeaderContext hunkHeader(int i) {
			return getRuleContext(HunkHeaderContext.class,i);
		}
		public HunkDiffContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hunkDiff; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterHunkDiff(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitHunkDiff(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitHunkDiff(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HunkDiffContext hunkDiff() throws RecognitionException {
		HunkDiffContext _localctx = new HunkDiffContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_hunkDiff);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(51); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(50);
				hunkHeader();
				}
				}
				setState(53); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==HUNK_HEADER );
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class BinaryDiffContext extends ParserRuleContext {
		public TerminalNode BINARY_FILES() { return getToken(GitDiffParser.BINARY_FILES, 0); }
		public TerminalNode BINARY_STATUS() { return getToken(GitDiffParser.BINARY_STATUS, 0); }
		public BinaryDiffContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_binaryDiff; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterBinaryDiff(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitBinaryDiff(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitBinaryDiff(this);
			else return visitor.visitChildren(this);
		}
	}

	public final BinaryDiffContext binaryDiff() throws RecognitionException {
		BinaryDiffContext _localctx = new BinaryDiffContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_binaryDiff);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(55);
			_la = _input.LA(1);
			if ( !(_la==BINARY_FILES || _la==BINARY_STATUS) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class HunkHeaderContext extends ParserRuleContext {
		public TerminalNode HUNK_HEADER() { return getToken(GitDiffParser.HUNK_HEADER, 0); }
		public List<LineContentContext> lineContent() {
			return getRuleContexts(LineContentContext.class);
		}
		public LineContentContext lineContent(int i) {
			return getRuleContext(LineContentContext.class,i);
		}
		public List<TerminalNode> NO_NEWLINE() { return getTokens(GitDiffParser.NO_NEWLINE); }
		public TerminalNode NO_NEWLINE(int i) {
			return getToken(GitDiffParser.NO_NEWLINE, i);
		}
		public HunkHeaderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_hunkHeader; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterHunkHeader(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitHunkHeader(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitHunkHeader(this);
			else return visitor.visitChildren(this);
		}
	}

	public final HunkHeaderContext hunkHeader() throws RecognitionException {
		HunkHeaderContext _localctx = new HunkHeaderContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_hunkHeader);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(57);
			match(HUNK_HEADER);
			setState(62);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 7680L) != 0)) {
				{
				setState(60);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case ADDED_LINE:
				case REMOVED_LINE:
				case CONTEXT_LINE:
					{
					setState(58);
					lineContent();
					}
					break;
				case NO_NEWLINE:
					{
					setState(59);
					match(NO_NEWLINE);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(64);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LineContentContext extends ParserRuleContext {
		public LineContentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lineContent; }
	 
		public LineContentContext() { }
		public void copyFrom(LineContentContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class RemovedLineContext extends LineContentContext {
		public TerminalNode REMOVED_LINE() { return getToken(GitDiffParser.REMOVED_LINE, 0); }
		public RemovedLineContext(LineContentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterRemovedLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitRemovedLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitRemovedLine(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class AddedLineContext extends LineContentContext {
		public TerminalNode ADDED_LINE() { return getToken(GitDiffParser.ADDED_LINE, 0); }
		public AddedLineContext(LineContentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterAddedLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitAddedLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitAddedLine(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ContextLineContext extends LineContentContext {
		public TerminalNode CONTEXT_LINE() { return getToken(GitDiffParser.CONTEXT_LINE, 0); }
		public ContextLineContext(LineContentContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).enterContextLine(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof GitDiffListener ) ((GitDiffListener)listener).exitContextLine(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof GitDiffVisitor ) return ((GitDiffVisitor<? extends T>)visitor).visitContextLine(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LineContentContext lineContent() throws RecognitionException {
		LineContentContext _localctx = new LineContentContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_lineContent);
		try {
			setState(68);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case ADDED_LINE:
				_localctx = new AddedLineContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(65);
				match(ADDED_LINE);
				}
				break;
			case REMOVED_LINE:
				_localctx = new RemovedLineContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(66);
				match(REMOVED_LINE);
				}
				break;
			case CONTEXT_LINE:
				_localctx = new ContextLineContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(67);
				match(CONTEXT_LINE);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001\rG\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0001\u0000\u0005\u0000\u0016\b\u0000\n\u0000"+
		"\f\u0000\u0019\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0002\u0001\u0002\u0005\u0002\"\b\u0002\n\u0002\f\u0002"+
		"%\t\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003*\b\u0003\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0003\u00051\b"+
		"\u0005\u0001\u0006\u0004\u00064\b\u0006\u000b\u0006\f\u00065\u0001\u0007"+
		"\u0001\u0007\u0001\b\u0001\b\u0001\b\u0005\b=\b\b\n\b\f\b@\t\b\u0001\t"+
		"\u0001\t\u0001\t\u0003\tE\b\t\u0001\t\u0000\u0000\n\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0000\u0001\u0001\u0000\u0006\u0007F\u0000"+
		"\u0017\u0001\u0000\u0000\u0000\u0002\u001c\u0001\u0000\u0000\u0000\u0004"+
		"\u001f\u0001\u0000\u0000\u0000\u0006)\u0001\u0000\u0000\u0000\b+\u0001"+
		"\u0000\u0000\u0000\n0\u0001\u0000\u0000\u0000\f3\u0001\u0000\u0000\u0000"+
		"\u000e7\u0001\u0000\u0000\u0000\u00109\u0001\u0000\u0000\u0000\u0012D"+
		"\u0001\u0000\u0000\u0000\u0014\u0016\u0003\u0002\u0001\u0000\u0015\u0014"+
		"\u0001\u0000\u0000\u0000\u0016\u0019\u0001\u0000\u0000\u0000\u0017\u0015"+
		"\u0001\u0000\u0000\u0000\u0017\u0018\u0001\u0000\u0000\u0000\u0018\u001a"+
		"\u0001\u0000\u0000\u0000\u0019\u0017\u0001\u0000\u0000\u0000\u001a\u001b"+
		"\u0005\u0000\u0000\u0001\u001b\u0001\u0001\u0000\u0000\u0000\u001c\u001d"+
		"\u0003\u0004\u0002\u0000\u001d\u001e\u0003\n\u0005\u0000\u001e\u0003\u0001"+
		"\u0000\u0000\u0000\u001f#\u0005\u0001\u0000\u0000 \"\u0003\u0006\u0003"+
		"\u0000! \u0001\u0000\u0000\u0000\"%\u0001\u0000\u0000\u0000#!\u0001\u0000"+
		"\u0000\u0000#$\u0001\u0000\u0000\u0000$\u0005\u0001\u0000\u0000\u0000"+
		"%#\u0001\u0000\u0000\u0000&*\u0005\u0003\u0000\u0000\'*\u0005\u0002\u0000"+
		"\u0000(*\u0003\b\u0004\u0000)&\u0001\u0000\u0000\u0000)\'\u0001\u0000"+
		"\u0000\u0000)(\u0001\u0000\u0000\u0000*\u0007\u0001\u0000\u0000\u0000"+
		"+,\u0005\u0004\u0000\u0000,-\u0005\u0005\u0000\u0000-\t\u0001\u0000\u0000"+
		"\u0000.1\u0003\f\u0006\u0000/1\u0003\u000e\u0007\u00000.\u0001\u0000\u0000"+
		"\u00000/\u0001\u0000\u0000\u00001\u000b\u0001\u0000\u0000\u000024\u0003"+
		"\u0010\b\u000032\u0001\u0000\u0000\u000045\u0001\u0000\u0000\u000053\u0001"+
		"\u0000\u0000\u000056\u0001\u0000\u0000\u00006\r\u0001\u0000\u0000\u0000"+
		"78\u0007\u0000\u0000\u00008\u000f\u0001\u0000\u0000\u00009>\u0005\b\u0000"+
		"\u0000:=\u0003\u0012\t\u0000;=\u0005\f\u0000\u0000<:\u0001\u0000\u0000"+
		"\u0000<;\u0001\u0000\u0000\u0000=@\u0001\u0000\u0000\u0000><\u0001\u0000"+
		"\u0000\u0000>?\u0001\u0000\u0000\u0000?\u0011\u0001\u0000\u0000\u0000"+
		"@>\u0001\u0000\u0000\u0000AE\u0005\t\u0000\u0000BE\u0005\n\u0000\u0000"+
		"CE\u0005\u000b\u0000\u0000DA\u0001\u0000\u0000\u0000DB\u0001\u0000\u0000"+
		"\u0000DC\u0001\u0000\u0000\u0000E\u0013\u0001\u0000\u0000\u0000\b\u0017"+
		"#)05<>D";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}