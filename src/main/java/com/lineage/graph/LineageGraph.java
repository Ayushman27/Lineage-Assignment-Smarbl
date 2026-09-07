package com.lineage.graph;

import com.lineage.model.Node;
import com.lineage.parser.ExpressionParser;
import com.lineage.parser.ParseResult;

import java.util.*;

/**
 * Directed data lineage / dependency graph.
 *
 * <h2>Edge orientation</h2>
 * Edges always point from <em>dependent</em> to <em>dependency</em>:
 * <pre>
 *   revenue = price * qty  →  revenue → price, revenue → qty
 * </pre>
 * Both directions are stored:
 * <ul>
 *   <li>{@code outgoing} ("uses"):    nodeId → set of nodes it depends on</li>
 *   <li>{@code incoming} ("usedBy"): nodeId → set of nodes that depend on it</li>
 * </ul>
 *
 * <h2>Construction policy for parse failures</h2>
 * If any node's expression cannot be parsed, an {@link ExpressionParseException}
 * is thrown immediately. Construction is aborted so that callers never receive
 * a silently-incorrect partial graph.
 *
 * <h2>Missing references</h2>
 * If an expression references a name that does not correspond to any node in
 * the input list, that name is treated as an external constant and silently
 * skipped — no edge is created.
 */
public final class LineageGraph {

    /** All nodes keyed by their ID. */
    private final Map<String, Node> nodesById;

    /** All nodes keyed by their name (used when resolving expression references). */
    private final Map<String, Node> nodesByName;

    /**
     * Outgoing edges: nodeId → set of nodes that this node directly depends on.
     * Conceptually "uses" or "depends on".
     */
    private final Map<String, Set<Node>> outgoing;

    /**
     * Incoming edges: nodeId → set of nodes that directly depend on this node.
     * Conceptually "usedBy" or "depended on by".
     */
    private final Map<String, Set<Node>> incoming;

    /**
     * Builds the lineage graph from the supplied node list.
     *
     * @param nodes list of nodes to include in the graph
     * @throws ExpressionParseException if any node's expression cannot be parsed
     * @throws CycleException           if the resulting graph contains a cycle
     */
    public LineageGraph(List<Node> nodes) {
        Objects.requireNonNull(nodes, "nodes list must not be null");

        nodesById    = new LinkedHashMap<>(nodes.size() * 2);
        nodesByName  = new LinkedHashMap<>(nodes.size() * 2);
        outgoing     = new LinkedHashMap<>(nodes.size() * 2);
        incoming     = new LinkedHashMap<>(nodes.size() * 2);

        // Index all nodes first so that cross-references can be resolved
        for (Node node : nodes) {
            if (nodesById.containsKey(node.getId())) {
                throw new IllegalArgumentException("Duplicate node id: " + node.getId());
            }
            if (nodesByName.containsKey(node.getName())) {
                throw new IllegalArgumentException("Duplicate node name: " + node.getName());
            }
            nodesById.put(node.getId(), node);
            nodesByName.put(node.getName(), node);
            outgoing.put(node.getId(), new LinkedHashSet<>());
            incoming.put(node.getId(), new LinkedHashSet<>());
        }

        // Parse expressions and build edges
        ExpressionParser parser = new ExpressionParser();
        for (Node node : nodes) {
            ParseResult result = parser.parse(node.getExpression());
            if (!result.isSuccess()) {
                throw new ExpressionParseException(
                    "Failed to parse expression for node id='" + node.getId()
                    + "' name='" + node.getName()
                    + "' expression='" + node.getExpression()
                    + "': " + result.getErrorMessage());
            }

            for (String varName : result.getReferencedVariables()) {
                Node dependency = nodesByName.get(varName);
                if (dependency == null) {
                    // External constant — ignore
                    continue;
                }
                // Avoid self-loops? No — we will let cycle detection catch self-references.
                outgoing.get(node.getId()).add(dependency);
                incoming.get(dependency.getId()).add(node);
            }
        }

        // Detect cycles before the graph can be used
        new CycleDetector().detectCycles(nodes, outgoing);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return an unmodifiable view of all nodes, keyed by id */
    public Map<String, Node> getNodesById() {
        return Collections.unmodifiableMap(nodesById);
    }

    /** @return an unmodifiable view of all nodes, keyed by name */
    public Map<String, Node> getNodesByName() {
        return Collections.unmodifiableMap(nodesByName);
    }

    /** @return all nodes in the graph */
    public Collection<Node> getAllNodes() {
        return Collections.unmodifiableCollection(nodesById.values());
    }

    /**
     * Returns the set of nodes that the given node directly depends on
     * ("outgoing" / "uses" direction).
     *
     * @param nodeId the node id to query
     * @return unmodifiable set of direct dependencies; empty set if nodeId unknown
     */
    public Set<Node> getDirectDependencies(String nodeId) {
        return Collections.unmodifiableSet(
            outgoing.getOrDefault(nodeId, Collections.emptySet()));
    }

    /**
     * Returns the set of nodes that directly depend on the given node
     * ("incoming" / "usedBy" direction).
     *
     * @param nodeId the node id to query
     * @return unmodifiable set of direct dependents; empty set if nodeId unknown
     */
    public Set<Node> getDirectDependents(String nodeId) {
        return Collections.unmodifiableSet(
            incoming.getOrDefault(nodeId, Collections.emptySet()));
    }

    /**
     * Package-private accessors used by {@link DependencyCache} and {@link PathFinder}.
     */
    Map<String, Set<Node>> getOutgoing() {
        return outgoing;
    }

    Map<String, Set<Node>> getIncoming() {
        return incoming;
    }
}
