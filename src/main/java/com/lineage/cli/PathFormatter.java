package com.lineage.cli;

import com.lineage.model.Node;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class that formats graph query results for CLI display.
 *
 * <p>Keeps all presentation logic separate from the graph algorithms.
 */
public final class PathFormatter {

    private PathFormatter() {}

    /**
     * Formats a single path as a human-readable arrow-separated string.
     *
     * <p>Example: {@code report -> margin -> revenue -> price}
     *
     * @param path ordered list of nodes forming the path
     * @return formatted string
     */
    public static String formatPath(List<Node> path) {
        return path.stream()
                   .map(Node::getName)
                   .collect(Collectors.joining(" -> "));
    }

    /**
     * Formats a list of paths, prefixing each with "Path N:".
     *
     * <p>Example:
     * <pre>
     * Path 1: report -> margin -> revenue -> price
     * Path 2: report -> margin -> cost
     * </pre>
     *
     * @param paths list of paths to format
     * @return multi-line formatted string
     */
    public static String formatPaths(List<List<Node>> paths) {
        if (paths.isEmpty()) {
            return "(no paths found)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append("Path ").append(i + 1).append(": ").append(formatPath(paths.get(i)));
        }
        return sb.toString();
    }

    /**
     * Formats a set of nodes as a comma-separated list of names, sorted for
     * deterministic output.
     *
     * @param nodes the set to format
     * @return sorted, comma-separated name list, or "(none)" if empty
     */
    public static String formatNodeSet(Set<Node> nodes) {
        if (nodes.isEmpty()) {
            return "(none)";
        }
        return nodes.stream()
                    .map(Node::getName)
                    .sorted()
                    .collect(Collectors.joining(", "));
    }
}
