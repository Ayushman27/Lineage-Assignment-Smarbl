package com.lineage.graph;

import com.lineage.model.Node;

import java.util.*;

/**
 * Finds all distinct simple paths between two nodes in the lineage graph.
 *
 * <h2>Algorithm</h2>
 * Iterative DFS with explicit backtracking using an {@link ArrayDeque} stack.
 * Each stack frame holds the current node and an iterator over its unvisited
 * neighbours. When the iterator is exhausted the node is popped and removed
 * from the current path (backtrack). This mirrors recursive DFS + backtracking
 * exactly, but does not consume Java call-stack frames, so it works correctly
 * on arbitrarily deep graphs (e.g. 10,000-node linear chains).
 *
 * <h2>Complexity</h2>
 * In the worst case (fully connected DAG) the number of simple paths is
 * exponential. For practical lineage graphs this is acceptable. Callers
 * should be aware of this for large/dense inputs.
 *
 * <h2>Edge orientation</h2>
 * Paths follow directed edges in the outgoing direction (dependent → dependency).
 * For example, in {@code report → margin → revenue → price},
 * {@code findPaths("report", "price")} returns {@code [[report, margin, revenue, price]]}.
 */
public final class PathFinder {

    private final Map<String, Set<Node>> outgoing;
    private final Map<String, Node>      nodesById;

    /**
     * Creates a PathFinder backed by the given graph.
     *
     * @param graph the fully-built lineage graph
     */
    public PathFinder(LineageGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");
        this.outgoing  = graph.getOutgoing();
        this.nodesById = graph.getNodesById();
    }

    /**
     * Returns all distinct simple paths from {@code fromId} to {@code toId}.
     *
     * @param fromId starting node id
     * @param toId   target node id
     * @return list of paths; each path is an ordered list of nodes from
     *         {@code fromId} (inclusive) to {@code toId} (inclusive).
     *         Returns an empty list if no path exists or if either id is unknown.
     *         Returns a single one-node path {@code [[node]]} if {@code fromId == toId}.
     */
    public List<List<Node>> findPaths(String fromId, String toId) {
        Node from = nodesById.get(fromId);
        Node to   = nodesById.get(toId);

        if (from == null || to == null) {
            return Collections.emptyList();
        }

        List<List<Node>> results = new ArrayList<>();

        // Fast path: same node
        if (fromId.equals(toId)) {
            results.add(Collections.singletonList(from));
            return results;
        }

        // Iterative DFS with backtracking.
        // Stack frames: each frame = (node, iterator-over-unvisited-neighbours).
        // currentPath mirrors the call-stack's implicit path in recursive DFS.
        Deque<StackFrame>  stack       = new ArrayDeque<>();
        LinkedList<Node>   currentPath = new LinkedList<>();
        Set<String>        visited     = new LinkedHashSet<>();

        // Push the start node
        stack.push(new StackFrame(from,
            outgoing.getOrDefault(from.getId(), Collections.emptySet()).iterator()));
        currentPath.addLast(from);
        visited.add(from.getId());

        while (!stack.isEmpty()) {
            StackFrame top = stack.peek();

            // Look for the next unvisited neighbour
            Node next = null;
            while (top.neighbours.hasNext()) {
                Node candidate = top.neighbours.next();
                if (!visited.contains(candidate.getId())) {
                    next = candidate;
                    break;
                }
            }

            if (next != null) {
                // Visit next node (pre-order)
                currentPath.addLast(next);
                visited.add(next.getId());

                if (next.getId().equals(to.getId())) {
                    // Reached target — record path, then immediately backtrack
                    results.add(new ArrayList<>(currentPath));
                    currentPath.removeLast();
                    visited.remove(next.getId());
                } else {
                    // Push next onto stack to continue DFS from it
                    stack.push(new StackFrame(next,
                        outgoing.getOrDefault(next.getId(), Collections.emptySet()).iterator()));
                }
            } else {
                // No more neighbours to explore from top — backtrack
                stack.pop();
                currentPath.removeLast();
                visited.remove(top.node.getId());
            }
        }

        return results;
    }

    // -------------------------------------------------------------------------

    private static final class StackFrame {
        final Node          node;
        final Iterator<Node> neighbours;

        StackFrame(Node node, Iterator<Node> neighbours) {
            this.node       = node;
            this.neighbours = neighbours;
        }
    }
}
