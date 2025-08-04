grammar GitDiff;

@header {
package me.ygy.pathfinder.git.parser.antlr4.generated;
}

// Parser Rules
diffFile
    : fileDiff* EOF
    ;

fileDiff
    : fileHeader
      diffContent
    ;

fileHeader
    : DIFF_HEADER
      headerContent*
    ;

headerContent
    : INDEX_HEADER
    | FILE_MODE_HEADER
    | filePaths
    ;

filePaths
    : OLD_FILE_HEADER NEW_FILE_HEADER
    ;

diffContent
    : (hunkDiff | binaryDiff)
    ;

hunkDiff
    : hunkHeader+
    ;

binaryDiff
    : BINARY_FILES
    | BINARY_STATUS
    ;

hunkHeader
    : HUNK_HEADER
      (lineContent | NO_NEWLINE)*
    ;

lineContent
    : ADDED_LINE    # addedLine
    | REMOVED_LINE  # removedLine
    | CONTEXT_LINE  # contextLine
    ;

// Lexer Rules
DIFF_HEADER
    : 'diff --git ' ~[\r\n]* NEWLINE
    ;

FILE_MODE_HEADER
    : ('deleted file mode '
    | 'new file mode '
    | 'old mode ') OCTAL NEWLINE
    ;

INDEX_HEADER
    : 'index ' HEX '..' HEX (' ' OCTAL)? NEWLINE
    ;

OLD_FILE_HEADER
    : '--- ' FILE_REF NEWLINE
    ;

NEW_FILE_HEADER
    : '+++ ' FILE_REF NEWLINE
    ;

BINARY_FILES
    : 'Binary files ' ~[\r\n]* ' differ' NEWLINE
    ;

BINARY_STATUS
    : ('Binary file ' ~[\r\n]* ' added' NEWLINE
    | 'Binary file ' ~[\r\n]* ' deleted' NEWLINE
    | 'Binary file ' ~[\r\n]* ' has changed' NEWLINE)
    ;

HUNK_HEADER
    : '@@ -' NUM (',' NUM)? ' +' NUM (',' NUM)? ' @@' .*? NEWLINE
    ;

ADDED_LINE
    : '+' ~[\r\n]* NEWLINE
    ;

REMOVED_LINE
    : '-' ~[\r\n]* NEWLINE
    ;

CONTEXT_LINE
    : ' ' ~[\r\n]* NEWLINE
    ;

NO_NEWLINE
    : '\\ No newline at end of file' NEWLINE
    ;

fragment FILE_REF
    : (('a/' | 'b/') ~[\r\n]+ | '/dev/null')
    ;

fragment HEX
    : [0-9a-f]+
    ;

fragment OCTAL
    : '100644' | '100755' | '120000'
    ;

fragment NUM
    : [0-9]+
    ;

fragment NEWLINE
    : '\r'? '\n'
    ;

// Skip whitespace between tokens
WS
    : [ \t]+ -> skip
    ;