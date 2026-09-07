package com.lineage.graph;

import com.lineage.model.Node;

import java.util.*;

/**
 * Detects cycles in the lineage dependency graph using iterative DFS with
 * three-colour marking (WHITE → GREY → BLACK).
 *
 * <h2>Algorithm</h2>
 * <ul>
 *   <li>WHITE (unvisited): node has not been visited yet.</li>
 *   <li>GREY  (in-stack): node is on the current DFS path — a back-edge to a
 *       grey node means a cycle.</li>
 *   <li>BLACK (finished): node and all its descendants have been fully explored;
 *       no cycle passes through this node.</li>
 * </ul>
 *
 * <p>When a cycle is detected the algorithm traces the cycle nodes from the
 * DFS stack and includes them in the {@link CycleException} message.
 */
public final class CycleDetector {

    private enum Color { WHITE, GREY, BLACK }

    /**
     * Checks the given outgoing adjacency map for cycles.
     *
     * @param nodes    all nodes in the graph
     * @param outgoing map from node id → set of nodes that node directly depends on
     * @throws CycleException if any cycle is found
     */
    public void detectCycles(Collection<Node> nodes, Map<String, Set<Node>> outgoing) {
        Map<String, Color> color = new HashMap<>();
        for (Node n : nodes) {
            color.put(n.getId(), Color.WHITE);
        }

        for (Node start : nodes) {
            if (color.get(start.getId()) == Color.WHITE) {
                dfs(start, outgoing, color);
            }
        }
    }

    private void dfs(Node start, Map<String, Set<Node>> outgoing, Map<String, Color> color) {
        // Iterative DFS using an explicit stack of (node, iterator-over-neighbors) pairs
        // to avoid stack overflow on very deep graphs.
        Deque<NodeState> stack = new ArrayDeque<>();
        List<String> path = new ArrayList<>(); // tracks current DFS path for cycle reporting

        stack.push(new NodeState(start, outgoing.getOrDefault(start.getId(), Collections.emptySet()).iterator()));
        color.put(start.getId(), Color.GREY);
        path.add(start.getId());

        while (!stack.isEmpty()) {
            NodeState top = stack.peek();

            if (top.neighbors.hasNext()) {
                Node neighbor = top.neighbors.next();
                Color neighborColor = color.getOrDefault(neighbor.getId(), Color.WHITE);

                if (neighborColor == Color.GREY) {
                    // Back-edge found: cycle detected
                    int cycleStart = path.indexOf(neighbor.getId());
                    List<String> cycleNodes = new ArrayList<>(path.subList(cycleStart, path.size()));
                    cycleNodes.add(neighbor.getId()); // close the loop
                    throw new CycleException(
                        "Cycle detected involving nodes: " + String.join(" → ", cycleNodes));
                }

                if (neighborColor == Color.WHITE) {
                    color.put(neighbor.getId(), Color.GREY);
                    path.add(neighbor.getId());
                    stack.push(new NodeState(neighbor,
                        outgoing.getOrDefault(neighbor.getId(), Collections.emptySet()).iterator()));
                }
            } else {
                // All neighbors explored — mark node as finished
                stack.pop();
                color.put(top.node.getId(), Color.BLACK);
                path.remove(path.size() - 1);
            }
        }
    }

    /** Holds DFS traversal state for one node on the iterative stack. */
    private static final class NodeState {
        final Node node;
        final Iterator<Node> neighbors;

        NodeState(Node node, Iterator<Node> neighbors) {
            this.node = node;
            this.neighbors = neighbors;
        }
    }
}
