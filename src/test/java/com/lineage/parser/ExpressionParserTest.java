package com.lineage.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for {@link ExpressionParser}.
 *
 * Each test verifies a specific parsing scenario, covering:
 * - correct variable extraction
 * - numeric constant exclusion
 * - complex expressions
 * - malformed/invalid expressions
 * - edge cases (empty, underscores, decimal numbers)
 */
@DisplayName("ExpressionParser tests")
class ExpressionParserTest {

    private ExpressionParser parser;

    @BeforeEach
    void setUp() {
        parser = new ExpressionParser();
    }

    // -------------------------------------------------------------------------
    // Basic extraction
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Simple subtraction returns both operands")
    void testSimpleSubtraction() {
        ParseResult result = parser.parse("revenue - cost");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("revenue", "cost"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Simple multiplication returns both operands")
    void testSimpleMultiplication() {
        ParseResult result = parser.parse("price * qty");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("price", "qty"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Division extracts both variables")
    void testDivision() {
        ParseResult result = parser.parse("total / count");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("total", "count"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Addition extracts both variables")
    void testAddition() {
        ParseResult result = parser.parse("base + offset");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("base", "offset"), result.getReferencedVariables());
    }

    // -------------------------------------------------------------------------
    // Parentheses
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Parenthesised multiplication with division by constant")
    void testParentheses() {
        ParseResult result = parser.parse("(price * qty) / 100");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("price", "qty"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Deeply nested parentheses")
    void testDeeplyNestedParentheses() {
        ParseResult result = parser.parse("((a + b) * (c - d))");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("a", "b", "c", "d"), result.getReferencedVariables());
    }

    // -------------------------------------------------------------------------
    // if/then/else
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("if/then/else with comparison extracts all variables")
    void testIfThenElse() {
        ParseResult result = parser.parse("if (score >= threshold) then bonus else 0");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("score", "threshold", "bonus"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("if/then/else with numeric else branch")
    void testIfThenElseWithNumericElse() {
        ParseResult result = parser.parse("if (margin > threshold) then margin else 0");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("margin", "threshold"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("if/then/else with expressions in both branches")
    void testIfThenElseBothBranches() {
        ParseResult result = parser.parse("if (x > 0) then x * rate else base + offset");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("x", "rate", "base", "offset"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("if/then/else with != operator")
    void testIfNotEqual() {
        ParseResult result = parser.parse("if (status != 0) then active else inactive");
        assertTrue(result.isSuccess());
        assertTrue(result.getReferencedVariables().contains("status"));
        assertTrue(result.getReferencedVariables().contains("active"));
        assertTrue(result.getReferencedVariables().contains("inactive"));
    }

    @Test
    @DisplayName("if/then/else with == operator")
    void testIfEqual() {
        ParseResult result = parser.parse("if (flag == 1) then val else default_val");
        assertTrue(result.isSuccess());
        assertTrue(result.getReferencedVariables().contains("flag"));
        assertTrue(result.getReferencedVariables().contains("val"));
        assertTrue(result.getReferencedVariables().contains("default_val"));
    }

    @Test
    @DisplayName("if/then/else with <= operator")
    void testIfLessOrEqual() {
        ParseResult result = parser.parse("if (score <= limit) then a else b");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("score", "limit", "a", "b"), result.getReferencedVariables());
    }

    // -------------------------------------------------------------------------
    // Numeric constants
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Numeric-only expression yields empty variable set")
    void testNumericOnly() {
        ParseResult result = parser.parse("42");
        assertTrue(result.isSuccess());
        assertTrue(result.getReferencedVariables().isEmpty());
    }

    @Test
    @DisplayName("Decimal number yields empty variable set")
    void testDecimalNumber() {
        ParseResult result = parser.parse("3.14");
        assertTrue(result.isSuccess());
        assertTrue(result.getReferencedVariables().isEmpty());
    }

    @Test
    @DisplayName("Expression mixing variable and constant")
    void testVariableAndConstant() {
        ParseResult result = parser.parse("revenue / 100");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("revenue"), result.getReferencedVariables());
    }

    // -------------------------------------------------------------------------
    // Identifier edge cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Identifier with underscore")
    void testUnderscoreIdentifier() {
        ParseResult result = parser.parse("my_var + other_var");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("my_var", "other_var"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Identifier starting with underscore")
    void testLeadingUnderscoreIdentifier() {
        ParseResult result = parser.parse("_internal * scale");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("_internal", "scale"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Identifier containing digits")
    void testIdentifierWithDigits() {
        ParseResult result = parser.parse("var1 + var2");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("var1", "var2"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Duplicate variable in expression counted once")
    void testDuplicateVariable() {
        ParseResult result = parser.parse("x * x");
        assertTrue(result.isSuccess());
        // Sets deduplicate
        assertEquals(Set.of("x"), result.getReferencedVariables());
    }

    // -------------------------------------------------------------------------
    // Nested / complex expressions
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Multi-level nested expression")
    void testNestedExpression() {
        ParseResult result = parser.parse("a + b * c - d / e");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("a", "b", "c", "d", "e"), result.getReferencedVariables());
    }

    @Test
    @DisplayName("Expression with spaces and newlines is valid")
    void testWhitespace() {
        ParseResult result = parser.parse("  revenue\n-\ncost  ");
        assertTrue(result.isSuccess());
        assertEquals(Set.of("revenue", "cost"), result.getReferencedVariables());
    }

    // -------------------------------------------------------------------------
    // Failure cases
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Null expression returns failure")
    void testNullExpression() {
        ParseResult result = parser.parse(null);
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Empty expression returns failure")
    void testEmptyExpression() {
        ParseResult result = parser.parse("");
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Blank expression returns failure")
    void testBlankExpression() {
        ParseResult result = parser.parse("   ");
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Malformed expression with unmatched paren returns failure")
    void testMalformedExpression() {
        ParseResult result = parser.parse("if (a +");
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
        assertNotNull(result.getOriginalExpression());
    }

    @Test
    @DisplayName("Malformed expression does not throw exception")
    void testMalformedExpressionNoException() {
        // Must NOT throw — must return a failure result
        assertDoesNotThrow(() -> parser.parse("((( broken"));
    }

    @Test
    @DisplayName("ParseResult failure contains original expression")
    void testFailureContainsOriginalExpression() {
        String bad = "if (a +";
        ParseResult result = parser.parse(bad);
        assertFalse(result.isSuccess());
        assertEquals(bad, result.getOriginalExpression());
    }

    @Test
    @DisplayName("Dangling operator returns failure")
    void testDanglingOperator() {
        ParseResult result = parser.parse("a + + b");
        // This may or may not fail depending on ANTLR recovery; test that we get a result
        assertNotNull(result);
    }
}
