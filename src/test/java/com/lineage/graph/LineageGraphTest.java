package com.lineage.graph;

import com.lineage.model.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link LineageGraph} covering edge creation, both directions,
 * missing references, duplicates, and invalid inputs.
 */
@DisplayName("LineageGraph tests")
class LineageGraphTest {

    // -------------------------------------------------------------------------
    // Helper factory methods
    // -------------------------------------------------------------------------

    private static Node node(String id, String name, String expression) {
        return new Node(id, name, expression);
    }

    private static LineageGraph graph(Node... nodes) {
        return new LineageGraph(Arrays.asList(nodes));
    }

    // -------------------------------------------------------------------------
    // Basic edge creation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Single node with constant expression creates no edges")
    void testSingleConstantNode() {
        Node price = node("1", "price", "100");
        LineageGraph g = graph(price);

        assertTrue(g.getDirectDependencies("1").isEmpty());
        assertTrue(g.getDirectDependents("1").isEmpty());
    }

    @Test
    @DisplayName("Basic dependency: revenue = price * qty")
    void testBasicEdgeCreation() {
        Node price   = node("1", "price", "100");
        Node qty     = node("2", "qty", "5");
        Node revenue = node("3", "revenue", "price * qty");

        LineageGraph g = graph(price, qty, revenue);

        // revenue depends on price and qty
        Set<Node> deps = g.getDirectDependencies("3");
        assertTrue(deps.contains(price));
        assertTrue(deps.contains(qty));
        assertEquals(2, deps.size());
    }

    @Test
    @DisplayName("Multiple dependencies chained")
    void testChainedDependencies() {
        Node price   = node("1", "price", "100");
        Node qty     = node("2", "qty", "5");
        Node revenue = node("3", "revenue", "price * qty");
        Node cost    = node("4", "cost", "300");
        Node margin  = node("5", "margin", "revenue - cost");

        LineageGraph g = graph(price, qty, revenue, cost, margin);

        // margin depends on revenue and cost
        Set<Node> marginDeps = g.getDirectDependencies("5");
        assertTrue(marginDeps.contains(revenue));
        assertTrue(marginDeps.contains(cost));
        assertEquals(2, marginDeps.size());

        // revenue is used by margin
        Set<Node> revUsedBy = g.getDirectDependents("3");
        assertTrue(revUsedBy.contains(margin));
    }

    // -------------------------------------------------------------------------
    // Missing references
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Reference to non-existent node is treated as external constant")
    void testMissingReferenceIgnored() {
        // "external_rate" does not exist as a node
        Node tax = node("1", "tax", "revenue * external_rate");
        Node revenue = node("2", "revenue", "100");

        LineageGraph g = graph(tax, revenue);

        // tax should depend on revenue, but external_rate is ignored
        Set<Node> deps = g.getDirectDependencies("1");
        assertTrue(deps.contains(revenue));
        // No node for "external_rate" was added
        assertEquals(1, deps.size());
    }

    // -------------------------------------------------------------------------
    // Duplicate references
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Same variable referenced twice in expression creates single edge")
    void testDuplicateReferenceInExpression() {
        Node x = node("1", "x", "10");
        Node y = node("2", "y", "x * x");

        LineageGraph g = graph(x, y);

        // y → x should be a single edge
        Set<Node> deps = g.getDirectDependencies("2");
        assertEquals(1, deps.size());
        assertTrue(deps.contains(x));
    }

    // -------------------------------------------------------------------------
    // Incoming / outgoing consistency
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Incoming and outgoing are consistent")
    void testBidirectionalConsistency() {
        Node price   = node("1", "price", "100");
        Node revenue = node("2", "revenue", "price");

        LineageGraph g = graph(price, revenue);

        // outgoing: revenue → price
        assertTrue(g.getDirectDependencies("2").contains(price));
        // incoming: price ← revenue
        assertTrue(g.getDirectDependents("1").contains(revenue));
    }

    @Test
    @DisplayName("Multiple dependents of one node")
    void testMultipleDependents() {
        Node price   = node("1", "price", "100");
        Node rev1    = node("2", "revenue1", "price * 5");
        Node rev2    = node("3", "revenue2", "price * 10");

        LineageGraph g = graph(price, rev1, rev2);

        Set<Node> priceUsedBy = g.getDirectDependents("1");
        assertTrue(priceUsedBy.contains(rev1));
        assertTrue(priceUsedBy.contains(rev2));
        assertEquals(2, priceUsedBy.size());
    }

    // -------------------------------------------------------------------------
    // Lookup by name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Nodes accessible by name")
    void testLookupByName() {
        Node price = node("1", "price", "100");
        LineageGraph g = graph(price);

        assertNotNull(g.getNodesByName().get("price"));
        assertEquals("1", g.getNodesByName().get("price").getId());
    }

    @Test
    @DisplayName("Nodes accessible by id")
    void testLookupById() {
        Node price = node("42", "price", "100");
        LineageGraph g = graph(price);

        assertNotNull(g.getNodesById().get("42"));
        assertEquals("price", g.getNodesById().get("42").getName());
    }

    // -------------------------------------------------------------------------
    // Graph with the canonical example
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Canonical example graph has correct structure")
    void testCanonicalGraph() {
        List<Node> nodes = buildCanonicalGraph();
        LineageGraph g = new LineageGraph(nodes);

        // report → margin and report → threshold
        Node report    = g.getNodesByName().get("report");
        Node margin    = g.getNodesByName().get("margin");
        Node threshold = g.getNodesByName().get("threshold");
        Node revenue   = g.getNodesByName().get("revenue");
        Node cost      = g.getNodesByName().get("cost");

        Set<Node> reportDeps = g.getDirectDependencies(report.getId());
        assertTrue(reportDeps.contains(margin));
        assertTrue(reportDeps.contains(threshold));

        // margin → revenue, margin → cost
        Set<Node> marginDeps = g.getDirectDependencies(margin.getId());
        assertTrue(marginDeps.contains(revenue));
        assertTrue(marginDeps.contains(cost));
    }

    // -------------------------------------------------------------------------
    // Error cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Invalid expression throws ExpressionParseException")
    void testInvalidExpressionThrows() {
        Node bad = node("1", "bad", "if (a +");
        assertThrows(ExpressionParseException.class, () -> graph(bad));
    }

    @Test
    @DisplayName("Duplicate node id throws IllegalArgumentException")
    void testDuplicateIdThrows() {
        Node n1 = node("1", "a", "100");
        Node n2 = node("1", "b", "200");
        assertThrows(IllegalArgumentException.class, () -> graph(n1, n2));
    }

    @Test
    @DisplayName("Duplicate node name throws IllegalArgumentException")
    void testDuplicateNameThrows() {
        Node n1 = node("1", "same", "100");
        Node n2 = node("2", "same", "200");
        assertThrows(IllegalArgumentException.class, () -> graph(n1, n2));
    }

    @Test
    @DisplayName("Empty node list builds empty graph without error")
    void testEmptyGraph() {
        LineageGraph g = new LineageGraph(Collections.emptyList());
        assertTrue(g.getAllNodes().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    static List<Node> buildCanonicalGraph() {
        return Arrays.asList(
            node("1", "price",     "100"),
            node("2", "qty",       "5"),
            node("3", "revenue",   "price * qty"),
            node("4", "cost",      "300"),
            node("5", "margin",    "revenue - cost"),
            node("6", "threshold", "500"),
            node("7", "report",    "if (margin > threshold) then margin else 0")
        );
    }
}
