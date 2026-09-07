package com.lineage.graph;

import com.lineage.model.Node;

import java.util.*;

/**
 * Pre-computes and caches the full transitive upstream and downstream dependency
 * sets for every node in the lineage graph.
 *
 * <h2>Definitions</h2>
 * <ul>
 *   <li><b>Upstream</b> of node N: all nodes that N directly or transitively
 *       depends on (following outgoing edges).</li>
 *   <li><b>Downstream</b> of node N: all nodes that directly or transitively
 *       depend on N (following incoming edges).</li>
 * </ul>
 *
 * <h2>Algorithm</h2>
 * Iterative post-order DFS (explicit stack) with memoisation. Recursive DFS
 * was replaced to avoid {@link StackOverflowError} on deep linear chains
 * (e.g. 10,000-node chains exceed Java's default call-stack depth).
 *
 * <h2>Complexity</h2>
 * Pre-computation: O(V × C_avg) where C_avg is the average closure size.
 * Repeated queries: O(1) — results returned directly from the cache map.
 *
 * <h2>Space</h2>
 * In the worst case (linear chain) caches hold O(V²) node references.
 */
public final class DependencyCache {

    private final Map<String, Set<Node>> upstreamCache;
    private final Map<String, Set<Node>> downstreamCache;

    /**
     * Builds the dependency cache for the given graph.
     *
     * @param graph the fully-built, acyclic lineage graph
     */
    public DependencyCache(LineageGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");

        Collection<Node> nodes = graph.getAllNodes();
        upstreamCache   = new HashMap<>(nodes.size() * 2);
        downstreamCache = new HashMap<>(nodes.size() * 2);

        Map<String, Set<Node>> outgoing = graph.getOutgoing();
        Map<String, Set<Node>> incoming = graph.getIncoming();

        // Compute upstream closure (along outgoing edges) for every node
        for (Node node : nodes) {
            if (!upstreamCache.containsKey(node.getId())) {
                computeClosureIterative(node.getId(), outgoing, upstreamCache);
            }
        }

        // Compute downstream closure (along incoming edges) for every node
        for (Node node : nodes) {
            if (!downstreamCache.containsKey(node.getId())) {
                computeClosureIterative(node.getId(), incoming, downstreamCache);
            }
        }
    }

    /**
     * Returns all nodes that the given node directly or transitively depends on.
     *
     * @param nodeId the node id to query
     * @return unmodifiable set of upstream nodes; empty set if nodeId is unknown
     */
    public Set<Node> getUpstream(String nodeId) {
        return Collections.unmodifiableSet(
            upstreamCache.getOrDefault(nodeId, Collections.emptySet()));
    }

    /**
     * Returns all nodes that directly or transitively depend on the given node.
     *
     * @param nodeId the node id to query
     * @return unmodifiable set of downstream nodes; empty set if nodeId is unknown
     */
    public Set<Node> getDownstream(String nodeId) {
        return Collections.unmodifiableSet(
            downstreamCache.getOrDefault(nodeId, Collections.emptySet()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Iterative post-order DFS that computes the transitive closure for
     * {@code startId} along the given adjacency map and stores results into
     * {@code cache}.
     *
     * <p>Post-order ensures a node's cache entry is only written after all its
     * neighbours' entries are complete, so we can union them in O(size).
     *
     * <p>Uses an explicit Deque instead of recursion to support arbitrarily
     * deep graphs (e.g. 10,000-node linear chains) without StackOverflowError.
     *
     * @param startId   id of the node to start from
     * @param adjacency the direction-specific adjacency map (outgoing or incoming)
     * @param cache     the target cache map to write results into
     */
    private void computeClosureIterative(String startId,
                                         Map<String, Set<Node>> adjacency,
                                         Map<String, Set<Node>> cache) {
        // Each stack frame holds: the node id + an iterator over its neighbours.
        // We push a frame when we first visit a node (pre-order),
        // and process/pop it once the iterator is exhausted (post-order).
        Deque<StackFrame> stack = new ArrayDeque<>();

        // Only push if not already cached
        if (!cache.containsKey(startId)) {
            Set<Node> neighbours = adjacency.getOrDefault(startId, Collections.emptySet());
            stack.push(new StackFrame(startId, neighbours.iterator()));
        }

        while (!stack.isEmpty()) {
            StackFrame top = stack.peek();

            if (top.neighbours.hasNext()) {
                Node neighbour = top.neighbours.next();
                String nid = neighbour.getId();

                if (!cache.containsKey(nid)) {
                    // Neighbour not yet computed — push it so it is processed first
                    Set<Node> nNeighbours = adjacency.getOrDefault(nid, Collections.emptySet());
                    stack.push(new StackFrame(nid, nNeighbours.iterator()));
                }
                // If already cached we will union it when we pop the current frame
            } else {
                // All neighbours processed — build this node's closure (post-order)
                stack.pop();
                Set<Node> closure = new HashSet<>();
                for (Node neighbour : adjacency.getOrDefault(top.nodeId, Collections.emptySet())) {
                    closure.add(neighbour);                          // direct
                    closure.addAll(cache.getOrDefault(             // transitive
                        neighbour.getId(), Collections.emptySet()));
                }
                cache.put(top.nodeId, closure);
            }
        }
    }

    /** Lightweight frame for the iterative DFS stack. */
    private static final class StackFrame {
        final String nodeId;
        final Iterator<Node> neighbours;

        StackFrame(String nodeId, Iterator<Node> neighbours) {
            this.nodeId     = nodeId;
            this.neighbours = neighbours;
        }
    }
}
