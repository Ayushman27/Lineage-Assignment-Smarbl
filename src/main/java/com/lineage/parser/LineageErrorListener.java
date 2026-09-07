package com.lineage.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom ANTLR error listener that suppresses the default console output and
 * collects syntax errors into a list for programmatic inspection.
 *
 * <p>Attach this to both the Lexer and the Parser before parsing so that all
 * errors are captured rather than printed to stderr.
 */
public final class LineageErrorListener extends BaseErrorListener {

    private final List<String> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        errors.add(String.format("line %d:%d %s", line, charPositionInLine, msg));
    }

    /** @return true if any syntax errors were captured */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** @return unmodifiable list of all captured error messages */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /** @return a single combined error string, useful for ParseResult failure messages */
    public String getCombinedErrorMessage() {
        return String.join("; ", errors);
    }
}
