package com.lineage.graph;

import com.lineage.model.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CycleDetector}.
 */
@DisplayName("CycleDetector tests")
class CycleDetectorTest {

    private static Node node(String id, String name, String expr) {
        return new Node(id, name, expr);
    }

    // -------------------------------------------------------------------------
    // No cycle cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Empty graph has no cycle")
    void testEmptyGraph() {
        assertDoesNotThrow(() ->
            new LineageGraph(Collections.emptyList()));
    }

    @Test
    @DisplayName("Single node with constant expression has no cycle")
    void testSingleNode() {
        assertDoesNotThrow(() ->
            new LineageGraph(Collections.singletonList(node("1", "a", "100"))));
    }

    @Test
    @DisplayName("Linear chain has no cycle")
    void testLinearChain() {
        // a → b → c
        assertDoesNotThrow(() ->
            new LineageGraph(Arrays.asList(
                node("1", "c", "100"),
                node("2", "b", "c"),
                node("3", "a", "b")
            )));
    }

    @Test
    @DisplayName("Canonical sample graph has no cycle")
    void testCanonicalNoCycle() {
        assertDoesNotThrow(() ->
            new LineageGraph(LineageGraphTest.buildCanonicalGraph()));
    }

    // -------------------------------------------------------------------------
    // Cycle cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Self-cycle is detected")
    void testSelfCycle() {
        // a depends on itself: a = a + 1
        CycleException ex = assertThrows(CycleException.class, () ->
            new LineageGraph(Collections.singletonList(
                node("1", "a", "a + 1")
            )));
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("1") || ex.getMessage().toLowerCase().contains("cycle"));
    }

    @Test
    @DisplayName("Simple 2-node cycle is detected: A → B → A")
    void testTwoNodeCycle() {
        CycleException ex = assertThrows(CycleException.class, () ->
            new LineageGraph(Arrays.asList(
                node("1", "A", "B"),
                node("2", "B", "A")
            )));
        String msg = ex.getMessage();
        assertTrue(msg != null && !msg.isEmpty());
    }

    @Test
    @DisplayName("Larger cycle A → B → C → A is detected")
    void testLargerCycle() {
        CycleException ex = assertThrows(CycleException.class, () ->
            new LineageGraph(Arrays.asList(
                node("1", "A", "B"),
                node("2", "B", "C"),
                node("3", "C", "A")
            )));
        String msg = ex.getMessage();
        assertTrue(msg != null && !msg.isEmpty());
    }

    @Test
    @DisplayName("Cycle deeper in the graph is detected")
    void testDeepCycle() {
        // root → x → y → z → x (x is involved in cycle but root is not)
        CycleException ex = assertThrows(CycleException.class, () ->
            new LineageGraph(Arrays.asList(
                node("1", "z", "x + 1"),    // z depends on x
                node("2", "y", "z"),          // y depends on z
                node("3", "x", "y"),          // x depends on y → forms x→y→z→x
                node("4", "root", "x + 10")  // root depends on x
            )));
        assertNotNull(ex.getMessage());
    }
}
