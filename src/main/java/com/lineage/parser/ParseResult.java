package com.lineage.parser;

import java.util.Collections;
import java.util.Set;

/**
 * Represents the outcome of parsing a single expression.
 *
 * <p>A result is either:
 * <ul>
 *   <li><b>SUCCESS</b>: contains the set of referenced variable names found in the expression.</li>
 *   <li><b>FAILURE</b>: contains a descriptive error message and the original expression.</li>
 * </ul>
 *
 * <p>Use the factory methods {@link #success(Set)} and {@link #failure(String, String)} to create instances.
 */
public final class ParseResult {

    private final boolean success;
    private final Set<String> referencedVariables;
    private final String errorMessage;
    private final String originalExpression;

    private ParseResult(boolean success, Set<String> referencedVariables,
                        String errorMessage, String originalExpression) {
        this.success = success;
        this.referencedVariables = referencedVariables;
        this.errorMessage = errorMessage;
        this.originalExpression = originalExpression;
    }

    /**
     * Creates a successful parse result.
     *
     * @param referencedVariables the set of variable names referenced in the expression
     * @return a success ParseResult
     */
    public static ParseResult success(Set<String> referencedVariables) {
        return new ParseResult(true, Collections.unmodifiableSet(referencedVariables), null, null);
    }

    /**
     * Creates a failed parse result.
     *
     * @param errorMessage       human-readable description of the parse error
     * @param originalExpression the expression that caused the failure
     * @return a failure ParseResult
     */
    public static ParseResult failure(String errorMessage, String originalExpression) {
        return new ParseResult(false, Collections.emptySet(), errorMessage, originalExpression);
    }

    /** @return true if parsing succeeded */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the set of variable names found in the expression.
     * Always returns an empty set on failure.
     */
    public Set<String> getReferencedVariables() {
        return referencedVariables;
    }

    /**
     * Returns the error message if parsing failed, or null on success.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the original expression string if parsing failed, or null on success.
     */
    public String getOriginalExpression() {
        return originalExpression;
    }

    @Override
    public String toString() {
        if (success) {
            return "ParseResult{SUCCESS, variables=" + referencedVariables + "}";
        }
        return "ParseResult{FAILURE, expression='" + originalExpression + "', error='" + errorMessage + "'}";
    }
}
