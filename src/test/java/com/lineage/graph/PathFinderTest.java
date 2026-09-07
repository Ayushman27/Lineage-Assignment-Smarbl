package com.lineage.graph;

import com.lineage.model.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PathFinder}.
 *
 * Uses the canonical 7-node graph and various custom graph shapes.
 */
@DisplayName("PathFinder tests")
class PathFinderTest {

    private LineageGraph canonicalGraph;
    private PathFinder canonicalFinder;

    @BeforeEach
    void setUp() {
        canonicalGraph = new LineageGraph(LineageGraphTest.buildCanonicalGraph());
        canonicalFinder = new PathFinder(canonicalGraph);
    }

    private String id(String name) {
        return canonicalGraph.getNodesByName().get(name).getId();
    }

    private static Node node(String id, String name, String expr) {
        return new Node(id, name, expr);
    }

    // -------------------------------------------------------------------------
    // One path cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Single path: report → margin → revenue → price")
    void testSinglePathReportToPrice() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("report"), id("price"));

        assertFalse(paths.isEmpty(), "Expected at least one path");

        // Verify one of the paths matches: report → margin → revenue → price
        boolean found = paths.stream().anyMatch(path -> {
            List<String> names = path.stream().map(Node::getName).collect(Collectors.toList());
            return names.equals(Arrays.asList("report", "margin", "revenue", "price"));
        });
        assertTrue(found, "Expected path [report, margin, revenue, price] to exist");
    }

    @Test
    @DisplayName("Single path: revenue → price")
    void testSimpleOnePath() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("revenue"), id("price"));
        assertEquals(1, paths.size());
        List<String> names = paths.get(0).stream().map(Node::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("revenue", "price"), names);
    }

    @Test
    @DisplayName("Single path: revenue → qty")
    void testSimpleOnePathQty() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("revenue"), id("qty"));
        assertEquals(1, paths.size());
        List<String> names = paths.get(0).stream().map(Node::getName).collect(Collectors.toList());
        assertEquals(Arrays.asList("revenue", "qty"), names);
    }

    // -------------------------------------------------------------------------
    // Multiple paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Multiple paths in a diamond graph: A → B → D, A → C → D")
    void testMultiplePaths() {
        // A → B → D
        // A → C → D
        LineageGraph g = new LineageGraph(Arrays.asList(
            node("1", "D", "100"),
            node("2", "B", "D"),
            node("3", "C", "D"),
            node("4", "A", "B + C")
        ));
        PathFinder finder = new PathFinder(g);

        List<List<Node>> paths = finder.findPaths("4", "1");
        assertEquals(2, paths.size(), "Should find exactly 2 paths in diamond graph");

        // Collect all paths as name lists for easier assertion
        Set<List<String>> pathNames = paths.stream()
            .map(p -> p.stream().map(Node::getName).collect(Collectors.toList()))
            .collect(Collectors.toSet());

        assertTrue(pathNames.contains(Arrays.asList("A", "B", "D")));
        assertTrue(pathNames.contains(Arrays.asList("A", "C", "D")));
    }

    // -------------------------------------------------------------------------
    // No path
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("No path when direction is reversed")
    void testNoPathReversed() {
        // price has no outgoing edges, so price → report is impossible
        List<List<Node>> paths = canonicalFinder.findPaths(id("price"), id("report"));
        assertTrue(paths.isEmpty());
    }

    @Test
    @DisplayName("No path between disconnected nodes")
    void testNoPathDisconnected() {
        // threshold → qty: no edge exists
        List<List<Node>> paths = canonicalFinder.findPaths(id("threshold"), id("qty"));
        assertTrue(paths.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Same node
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Same-node path returns single one-element path")
    void testSameNode() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("price"), id("price"));
        assertEquals(1, paths.size());
        assertEquals(1, paths.get(0).size());
        assertEquals("price", paths.get(0).get(0).getName());
    }

    // -------------------------------------------------------------------------
    // Unknown node IDs
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unknown fromId returns empty list")
    void testUnknownFrom() {
        List<List<Node>> paths = canonicalFinder.findPaths("unknown_id", id("price"));
        assertTrue(paths.isEmpty());
    }

    @Test
    @DisplayName("Unknown toId returns empty list")
    void testUnknownTo() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("report"), "unknown_id");
        assertTrue(paths.isEmpty());
    }

    @Test
    @DisplayName("Both IDs unknown returns empty list")
    void testBothUnknown() {
        List<List<Node>> paths = canonicalFinder.findPaths("x", "y");
        assertTrue(paths.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Simple-path guarantee (no repeated nodes)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("All returned paths are simple (no repeated nodes)")
    void testPathsAreSimple() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("report"), id("price"));
        for (List<Node> path : paths) {
            long distinctCount = path.stream().map(Node::getId).distinct().count();
            assertEquals(path.size(), distinctCount, "Path contains repeated node: " + path);
        }
    }

    // -------------------------------------------------------------------------
    // No duplicate paths
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("No duplicate paths returned")
    void testNoDuplicatePaths() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("report"), id("price"));
        Set<List<String>> namePathSet = paths.stream()
            .map(p -> p.stream().map(Node::getName).collect(Collectors.toList()))
            .collect(Collectors.toSet());
        assertEquals(paths.size(), namePathSet.size(), "Duplicate paths found");
    }

    // -------------------------------------------------------------------------
    // Branching graph
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Branching graph: multiple paths via different routes")
    void testBranchingGraph() {
        // root → a → c (leaf)
        //      → b → c
        LineageGraph g = new LineageGraph(Arrays.asList(
            node("1", "c",    "100"),
            node("2", "a",    "c"),
            node("3", "b",    "c"),
            node("4", "root", "a + b")
        ));
        PathFinder finder = new PathFinder(g);
        List<List<Node>> paths = finder.findPaths("4", "1");

        assertEquals(2, paths.size());
        Set<List<String>> names = paths.stream()
            .map(p -> p.stream().map(Node::getName).collect(Collectors.toList()))
            .collect(Collectors.toSet());
        assertTrue(names.contains(Arrays.asList("root", "a", "c")));
        assertTrue(names.contains(Arrays.asList("root", "b", "c")));
    }

    // -------------------------------------------------------------------------
    // Path correctness: first and last node
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("All paths start with fromId and end with toId")
    void testPathStartAndEnd() {
        List<List<Node>> paths = canonicalFinder.findPaths(id("report"), id("qty"));
        for (List<Node> path : paths) {
            assertFalse(path.isEmpty());
            assertEquals(id("report"), path.get(0).getId());
            assertEquals(id("qty"), path.get(path.size() - 1).getId());
        }
    }
}
