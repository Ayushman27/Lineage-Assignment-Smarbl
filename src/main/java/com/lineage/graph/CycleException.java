package com.lineage.graph;

/**
 * Thrown when the lineage graph contains a cycle.
 *
 * <p>The message includes the cycle path to help users identify which nodes
 * are involved. A cyclic graph cannot be used for dependency pre-computation,
 * so construction fails fast.
 */
public class CycleException extends RuntimeException {

    public CycleException(String message) {
        super(message);
    }
}
