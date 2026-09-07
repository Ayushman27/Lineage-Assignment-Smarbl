package com.lineage.graph;

/**
 * Thrown when an expression in a node cannot be parsed during graph construction.
 *
 * <p>Construction fails fast on the first bad expression so that the caller
 * is never left with a partially-built, silently-incorrect graph.
 */
public class ExpressionParseException extends RuntimeException {

    public ExpressionParseException(String message) {
        super(message);
    }
}
