package com.lineage.model;

import java.util.Objects;

/**
 * Represents a single node in the lineage graph.
 * Each node has a unique numeric ID, a unique name, and an expression string.
 *
 * <p>Expressions reference other nodes by NAME (not by ID).
 * For example: {@code revenue = price * qty} means node "revenue" depends on
 * nodes named "price" and "qty".
 */
public final class Node {

    private final String id;
    private final String name;
    private final String expression;

    /**
     * Creates a Node. All fields are required.
     *
     * @param id         unique string identifier (may originate from a numeric JSON field)
     * @param name       unique human-readable name used in expressions
     * @param expression the formula/expression string for this node
     */
    public Node(String id, String name, String expression) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Node id must not be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Node name must not be null or blank");
        }
        if (expression == null) {
            throw new IllegalArgumentException("Node expression must not be null");
        }
        this.id = id;
        this.name = name;
        this.expression = expression;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getExpression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Node{id='" + id + "', name='" + name + "', expression='" + expression + "'}";
    }
}
