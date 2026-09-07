package com.lineage.parser;

import com.lineage.parser.generated.LineageLexer;
import com.lineage.parser.generated.LineageParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

/**
 * Parses a single expression string and extracts all referenced variable names.
 *
 * <h2>Pipeline</h2>
 * <pre>
 *   expression string
 *     → {@link LineageLexer}  (tokenises)
 *     → {@link LineageParser} (produces parse tree)
 *     → {@link VariableReferenceVisitor} (walks tree, collects IDENTIFIERs)
 *     → {@link ParseResult}
 * </pre>
 *
 * <h2>Error handling</h2>
 * The default ANTLR console error output is removed. A custom
 * {@link LineageErrorListener} is attached to both the lexer and parser so that
 * syntax errors are captured programmatically. Any exception during parsing is
 * also caught and returned as a {@link ParseResult#failure} — the caller is
 * never exposed to an unhandled exception.
 *
 * <h2>Empty expressions</h2>
 * A blank or empty expression string is treated as a parse failure, not a
 * successful parse with no variables.
 */
public final class ExpressionParser {

    /**
     * Parses the given expression and returns a {@link ParseResult}.
     *
     * @param expression the expression to parse (may be null/blank — will be treated as failure)
     * @return a success result containing referenced variable names,
     *         or a failure result with a descriptive error message
     */
    public ParseResult parse(String expression) {
        if (expression == null || expression.isBlank()) {
            return ParseResult.failure("Expression is null or empty", expression == null ? "" : expression);
        }

        try {
            // 1. Lexer
            LineageLexer lexer = new LineageLexer(CharStreams.fromString(expression));
            lexer.removeErrorListeners();                         // suppress default console output
            LineageErrorListener lexerErrors = new LineageErrorListener();
            lexer.addErrorListener(lexerErrors);

            // 2. Parser
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            LineageParser parser = new LineageParser(tokens);
            parser.removeErrorListeners();                        // suppress default console output
            LineageErrorListener parserErrors = new LineageErrorListener();
            parser.addErrorListener(parserErrors);

            // 3. Build parse tree
            LineageParser.ExprContext tree = parser.expr();

            // 4. Check for errors after tree construction
            if (lexerErrors.hasErrors() || parserErrors.hasErrors()) {
                StringBuilder sb = new StringBuilder();
                if (lexerErrors.hasErrors()) {
                    sb.append("Lexer errors: ").append(lexerErrors.getCombinedErrorMessage());
                }
                if (parserErrors.hasErrors()) {
                    if (sb.length() > 0) sb.append("; ");
                    sb.append("Parser errors: ").append(parserErrors.getCombinedErrorMessage());
                }
                return ParseResult.failure(sb.toString(), expression);
            }

            // 5. Walk parse tree with visitor
            VariableReferenceVisitor visitor = new VariableReferenceVisitor();
            visitor.visit(tree);

            return ParseResult.success(visitor.getReferencedVariables());

        } catch (Exception e) {
            return ParseResult.failure("Unexpected error during parsing: " + e.getMessage(), expression);
        }
    }
}
