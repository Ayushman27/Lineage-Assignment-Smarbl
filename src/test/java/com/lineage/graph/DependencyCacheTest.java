package com.lineage.graph;

import com.lineage.model.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DependencyCache}.
 *
 * Uses the canonical 7-node sample graph for most tests:
 * <pre>
 *   price (1), qty (2), cost (4), threshold (6) — leaf constants
 *   revenue (3)   = price * qty
 *   margin  (5)   = revenue - cost
 *   report  (7)   = if (margin > threshold) then margin else 0
 * </pre>
 *
 * Dependency direction: dependent → dependency
 */
@DisplayName("DependencyCache tests")
class DependencyCacheTest {

    private LineageGraph graph;
    private DependencyCache cache;

    @BeforeEach
    void setUp() {
        graph = new LineageGraph(LineageGraphTest.buildCanonicalGraph());
        cache = new DependencyCache(graph);
    }

    private String id(String name) {
        return graph.getNodesByName().get(name).getId();
    }

    private Node node(String name) {
        return graph.getNodesByName().get(name);
    }

    // -------------------------------------------------------------------------
    // Upstream tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Upstream of leaf node (price) is empty")
    void testUpstreamLeaf() {
        Set<Node> upstream = cache.getUpstream(id("price"));
        assertTrue(upstream.isEmpty(), "price has no upstream dependencies");
    }

    @Test
    @DisplayName("Direct upstream of revenue is {price, qty}")
    void testDirectUpstreamRevenue() {
        Set<Node> upstream = cache.getUpstream(id("revenue"));
        assertEquals(2, upstream.size());
        assertTrue(upstream.contains(node("price")));
        assertTrue(upstream.contains(node("qty")));
    }

    @Test
    @DisplayName("Transitive upstream of margin is {revenue, cost, price, qty}")
    void testTransitiveUpstreamMargin() {
        Set<Node> upstream = cache.getUpstream(id("margin"));
        assertEquals(4, upstream.size());
        assertTrue(upstream.contains(node("revenue")));
        assertTrue(upstream.contains(node("cost")));
        assertTrue(upstream.contains(node("price")));
        assertTrue(upstream.contains(node("qty")));
    }

    @Test
    @DisplayName("Full transitive upstream of report is {margin, revenue, cost, price, qty, threshold}")
    void testFullUpstreamReport() {
        Set<Node> upstream = cache.getUpstream(id("report"));
        assertEquals(6, upstream.size());
        assertTrue(upstream.contains(node("margin")));
        assertTrue(upstream.contains(node("revenue")));
        assertTrue(upstream.contains(node("cost")));
        assertTrue(upstream.contains(node("price")));
        assertTrue(upstream.contains(node("qty")));
        assertTrue(upstream.contains(node("threshold")));
    }

    // -------------------------------------------------------------------------
    // Downstream tests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Downstream of report (root) is empty")
    void testDownstreamRoot() {
        Set<Node> downstream = cache.getDownstream(id("report"));
        assertTrue(downstream.isEmpty(), "report has no downstream (nothing depends on it)");
    }

    @Test
    @DisplayName("Direct downstream of price is {revenue}")
    void testDirectDownstreamPrice() {
        Set<Node> downstream = cache.getDownstream(id("price"));
        assertTrue(downstream.contains(node("revenue")));
    }

    @Test
    @DisplayName("Transitive downstream of price includes margin and report")
    void testTransitiveDownstreamPrice() {
        Set<Node> downstream = cache.getDownstream(id("price"));
        assertTrue(downstream.contains(node("revenue")));
        assertTrue(downstream.contains(node("margin")));
        assertTrue(downstream.contains(node("report")));
    }

    @Test
    @DisplayName("Transitive downstream of revenue includes margin and report")
    void testDownstreamRevenue() {
        Set<Node> downstream = cache.getDownstream(id("revenue"));
        assertTrue(downstream.contains(node("margin")));
        assertTrue(downstream.contains(node("report")));
    }

    // -------------------------------------------------------------------------
    // Unknown node id
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUpstream with unknown id returns empty set (no exception)")
    void testUpstreamUnknownId() {
        Set<Node> upstream = cache.getUpstream("non_existent_id");
        assertNotNull(upstream);
        assertTrue(upstream.isEmpty());
    }

    @Test
    @DisplayName("getDownstream with unknown id returns empty set (no exception)")
    void testDownstreamUnknownId() {
        Set<Node> downstream = cache.getDownstream("non_existent_id");
        assertNotNull(downstream);
        assertTrue(downstream.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Isolated node
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Isolated constant node has empty upstream and downstream")
    void testIsolatedNode() {
        // cost is only depended on by margin (not isolated, but let's check)
        // create a truly isolated graph
        List<Node> single = List.of(new Node("99", "standalone", "42"));
        LineageGraph g = new LineageGraph(single);
        DependencyCache c = new DependencyCache(g);

        assertTrue(c.getUpstream("99").isEmpty());
        assertTrue(c.getDownstream("99").isEmpty());
    }

    // -------------------------------------------------------------------------
    // Repeated queries return cached results (idempotence)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Repeated getUpstream calls return same result")
    void testUpstreamIdempotent() {
        Set<Node> first  = cache.getUpstream(id("report"));
        Set<Node> second = cache.getUpstream(id("report"));
        assertEquals(first, second);
    }

    @Test
    @DisplayName("Repeated getDownstream calls return same result")
    void testDownstreamIdempotent() {
        Set<Node> first  = cache.getDownstream(id("price"));
        Set<Node> second = cache.getDownstream(id("price"));
        assertEquals(first, second);
    }
}
