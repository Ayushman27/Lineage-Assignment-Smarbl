package com.lineage.benchmark;

import com.lineage.graph.DependencyCache;
import com.lineage.graph.LineageGraph;
import com.lineage.graph.PathFinder;
import com.lineage.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Standalone benchmark for the Expression Parser &amp; Lineage Graph system.
 *
 * <h2>Graph topology</h2>
 * Generates a layered DAG:
 * <ul>
 *   <li>Layer 0: {@code N_LEAVES} leaf nodes with constant expressions.</li>
 *   <li>Layer 1..L: each node references up to {@code FANIN} nodes from the previous layer.</li>
 * </ul>
 * The total number of nodes targets approximately {@code TARGET_NODES}.
 *
 * <p>Run this class directly (after building the project) to produce actual timing numbers.
 */
public final class BenchmarkRunner {

    /** Approximate target number of nodes. */
    private static final int TARGET_NODES = 10_000;

    /** Number of leaf (constant) nodes in layer 0. */
    private static final int N_LEAVES = 100;

    /** Maximum number of dependencies per non-leaf node. */
    private static final int FANIN = 3;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Expression Parser & Lineage Graph — Benchmark ===");
        System.out.println("Target nodes : ~" + TARGET_NODES);
        System.out.println("Leaf nodes   : " + N_LEAVES);
        System.out.println("Max fan-in   : " + FANIN);
        System.out.println("JVM          : " + System.getProperty("java.vm.name")
                           + " " + System.getProperty("java.version"));
        System.out.println();

        // -----------------------------------------------------------------
        // 1. Generate node list
        // -----------------------------------------------------------------
        long genStart = System.nanoTime();
        List<Node> nodes = generateNodes();
        long genEnd = System.nanoTime();
        System.out.printf("Node generation : %,d nodes generated in %.1f ms%n",
                nodes.size(), (genEnd - genStart) / 1_000_000.0);

        // Count edges for reporting
        // (edges = sum of references per non-leaf node, capped at fanin)
        int estimatedEdges = (nodes.size() - N_LEAVES) * FANIN;
        System.out.printf("Estimated edges : ~%,d%n", estimatedEdges);
        System.out.println();

        // -----------------------------------------------------------------
        // 2. Graph construction (includes parsing + cycle detection)
        // -----------------------------------------------------------------
        long graphStart = System.nanoTime();
        LineageGraph graph = new LineageGraph(nodes);
        long graphEnd = System.nanoTime();
        System.out.printf("Graph construction (parse + edges + cycle detect) : %.1f ms%n",
                (graphEnd - graphStart) / 1_000_000.0);

        // -----------------------------------------------------------------
        // 3. Dependency cache pre-computation
        // -----------------------------------------------------------------
        long cacheStart = System.nanoTime();
        DependencyCache cache = new DependencyCache(graph);
        long cacheEnd = System.nanoTime();
        System.out.printf("Dependency cache pre-computation                  : %.1f ms%n",
                (cacheEnd - cacheStart) / 1_000_000.0);

        // -----------------------------------------------------------------
        // 4. Upstream/downstream queries (should be ~0 ms each)
        // -----------------------------------------------------------------
        String lastNodeId = nodes.get(nodes.size() - 1).getId();
        String firstNodeId = nodes.get(0).getId();

        long upStart = System.nanoTime();
        Set<Node> upstream = cache.getUpstream(lastNodeId);
        long upEnd = System.nanoTime();
        System.out.printf("getUpstream  (last node, %,d results)            : %.3f ms%n",
                upstream.size(), (upEnd - upStart) / 1_000_000.0);

        long downStart = System.nanoTime();
        Set<Node> downstream = cache.getDownstream(firstNodeId);
        long downEnd = System.nanoTime();
        System.out.printf("getDownstream (first node, %,d results)          : %.3f ms%n",
                downstream.size(), (downEnd - downStart) / 1_000_000.0);

        // -----------------------------------------------------------------
        // 5. Path finding on a SEPARATE linear chain (bounded)
        //    A layered DAG with fan-in=3 produces an EXPONENTIAL number
        //    of simple paths (up to 3^layers), so we measure path-finding
        //    on a 200-node linear chain where exactly 1 path exists.
        // -----------------------------------------------------------------
        System.out.println();
        System.out.println("-- Path-finding sub-benchmark (200-node linear chain, 1 path) --");
        List<Node> chain = buildLinearChain(200);
        LineageGraph chainGraph = new LineageGraph(chain);
        PathFinder chainFinder = new PathFinder(chainGraph);
        String chainTop  = chain.get(chain.size() - 1).getId();
        String chainLeaf = chain.get(0).getId();

        long pathStart = System.nanoTime();
        List<List<Node>> paths = chainFinder.findPaths(chainTop, chainLeaf);
        long pathEnd = System.nanoTime();
        System.out.printf("findPaths (200-node chain, %,d path(s) found)     : %.3f ms%n",
                paths.size(), (pathEnd - pathStart) / 1_000_000.0);

        System.out.println();
        System.out.println("NOTE: Path-finding on the layered 10k-node DAG (fan-in=" + FANIN + ") is not");
        System.out.println("      measured because the number of simple paths is exponential (~3^layers).");
        System.out.println("      This is an inherent property of all-simple-paths on dense DAGs.");
        System.out.println();
        System.out.println("=== Benchmark complete ===");
    }

    /** Builds a 200-node linear chain: top -> top-1 -> ... -> leaf(0). */
    private static List<Node> buildLinearChain(int size) {
        List<Node> chain = new ArrayList<>(size);
        // Leaf (constant)
        Node prev = new Node("chain_1", "chain_n1", "1");
        chain.add(prev);
        for (int i = 2; i <= size; i++) {
            Node n = new Node("chain_" + i, "chain_n" + i, prev.getName());
            chain.add(n);
            prev = n;
        }
        return chain;
    }

    /**
     * Generates a layered DAG.
     * Layer 0: N_LEAVES constant nodes.
     * Subsequent layers: each new node references up to FANIN nodes from the previous layer.
     * Stop when TARGET_NODES is reached.
     */
    private static List<Node> generateNodes() {
        List<Node> nodes = new ArrayList<>(TARGET_NODES);
        int idCounter = 1;

        // Layer 0 — leaf constants
        List<Node> currentLayer = new ArrayList<>();
        for (int i = 0; i < N_LEAVES; i++) {
            Node n = new Node(String.valueOf(idCounter), "n" + idCounter, "1");
            nodes.add(n);
            currentLayer.add(n);
            idCounter++;
        }

        // Subsequent layers
        while (nodes.size() < TARGET_NODES) {
            List<Node> nextLayer = new ArrayList<>();
            int layerSize = Math.min(currentLayer.size(), TARGET_NODES - nodes.size());
            if (layerSize <= 0) break;

            for (int i = 0; i < layerSize; i++) {
                // Pick up to FANIN nodes from the previous layer as dependencies
                StringBuilder expr = new StringBuilder();
                int startIdx = i % currentLayer.size();
                for (int f = 0; f < FANIN; f++) {
                    int depIdx = (startIdx + f) % currentLayer.size();
                    if (f > 0) expr.append(" + ");
                    expr.append(currentLayer.get(depIdx).getName());
                }

                Node n = new Node(String.valueOf(idCounter), "n" + idCounter, expr.toString());
                nodes.add(n);
                nextLayer.add(n);
                idCounter++;

                if (nodes.size() >= TARGET_NODES) break;
            }

            currentLayer = nextLayer;
        }

        return nodes;
    }
}
