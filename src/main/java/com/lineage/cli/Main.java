package com.lineage.cli;

import com.lineage.graph.CycleException;
import com.lineage.graph.DependencyCache;
import com.lineage.graph.ExpressionParseException;
import com.lineage.graph.LineageGraph;
import com.lineage.graph.PathFinder;
import com.lineage.loader.NodeJsonLoader;
import com.lineage.model.Node;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Command-line entry point for the Expression Parser &amp; Lineage Graph tool.
 *
 * <h2>Usage</h2>
 * <pre>
 *   # Validate the graph (build + detect cycles)
 *   java -jar lineage.jar &lt;input.json&gt; validate
 *
 *   # Query upstream transitive dependencies for a node
 *   java -jar lineage.jar &lt;input.json&gt; upstream &lt;nodeId&gt;
 *
 *   # Query downstream transitive dependents for a node
 *   java -jar lineage.jar &lt;input.json&gt; downstream &lt;nodeId&gt;
 *
 *   # Find all simple paths between two nodes
 *   java -jar lineage.jar &lt;input.json&gt; paths &lt;fromId&gt; &lt;toId&gt;
 * </pre>
 *
 * <p>Equivalent invocations via the provided shell scripts:
 * <pre>
 *   ./run.sh input.json validate
 *   ./run.sh input.json upstream 3
 *   ./run.sh input.json downstream 1
 *   ./run.sh input.json paths 7 1
 * </pre>
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        String jsonPath = args[0];
        String command  = args[1].toLowerCase();

        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists() || !jsonFile.isFile()) {
            System.err.println("ERROR: File not found: " + jsonPath);
            System.exit(1);
        }

        try {
            // 1. Load nodes
            NodeJsonLoader loader = new NodeJsonLoader();
            List<Node> nodes = loader.load(jsonFile);
            System.out.println("Loaded " + nodes.size() + " node(s) from " + jsonPath);

            // 2. Build graph (parses all expressions, detects cycles)
            LineageGraph graph = new LineageGraph(nodes);
            System.out.println("Graph built successfully. Nodes: " + graph.getAllNodes().size());

            // 3. Build dependency cache
            DependencyCache cache = new DependencyCache(graph);
            System.out.println("Dependency cache built.");
            System.out.println();

            switch (command) {
                case "validate":
                    System.out.println("Graph is valid (no cycles detected).");
                    break;

                case "upstream":
                    handleUpstream(args, graph, cache);
                    break;

                case "downstream":
                    handleDownstream(args, graph, cache);
                    break;

                case "paths":
                    handlePaths(args, graph);
                    break;

                default:
                    System.err.println("ERROR: Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }

        } catch (ExpressionParseException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(2);
        } catch (CycleException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(3);
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(4);
        }
    }

    private static void handleUpstream(String[] args, LineageGraph graph, DependencyCache cache) {
        if (args.length < 3) {
            System.err.println("ERROR: 'upstream' requires a node id argument.");
            printUsage();
            System.exit(1);
        }
        String nodeId = args[2];
        if (!graph.getNodesById().containsKey(nodeId)) {
            System.out.println("WARNING: No node found with id '" + nodeId + "'. Returning empty set.");
        }
        Set<Node> upstream = cache.getUpstream(nodeId);
        System.out.println("Upstream of node " + nodeId + " (" + upstream.size() + " nodes):");
        System.out.println(PathFormatter.formatNodeSet(upstream));
    }

    private static void handleDownstream(String[] args, LineageGraph graph, DependencyCache cache) {
        if (args.length < 3) {
            System.err.println("ERROR: 'downstream' requires a node id argument.");
            printUsage();
            System.exit(1);
        }
        String nodeId = args[2];
        if (!graph.getNodesById().containsKey(nodeId)) {
            System.out.println("WARNING: No node found with id '" + nodeId + "'. Returning empty set.");
        }
        Set<Node> downstream = cache.getDownstream(nodeId);
        System.out.println("Downstream of node " + nodeId + " (" + downstream.size() + " nodes):");
        System.out.println(PathFormatter.formatNodeSet(downstream));
    }

    private static void handlePaths(String[] args, LineageGraph graph) {
        if (args.length < 4) {
            System.err.println("ERROR: 'paths' requires two node id arguments.");
            printUsage();
            System.exit(1);
        }
        String fromId = args[2];
        String toId   = args[3];

        if (!graph.getNodesById().containsKey(fromId)) {
            System.out.println("WARNING: No node found with id '" + fromId + "'.");
        }
        if (!graph.getNodesById().containsKey(toId)) {
            System.out.println("WARNING: No node found with id '" + toId + "'.");
        }

        PathFinder finder = new PathFinder(graph);
        List<List<Node>> paths = finder.findPaths(fromId, toId);

        System.out.println("Paths from node " + fromId + " to node " + toId
            + " (" + paths.size() + " path(s) found):");
        System.out.println(PathFormatter.formatPaths(paths));
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  lineage <input.json> validate");
        System.out.println("  lineage <input.json> upstream   <nodeId>");
        System.out.println("  lineage <input.json> downstream <nodeId>");
        System.out.println("  lineage <input.json> paths      <fromId> <toId>");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  ./run.sh sample.json validate");
        System.out.println("  ./run.sh sample.json upstream 7");
        System.out.println("  ./run.sh sample.json downstream 1");
        System.out.println("  ./run.sh sample.json paths 7 1");
    }
}
