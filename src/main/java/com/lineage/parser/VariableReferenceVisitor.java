package com.lineage.parser;

import com.lineage.parser.generated.LineageBaseVisitor;
import com.lineage.parser.generated.LineageParser;

import java.util.HashSet;
import java.util.Set;

/**
 * ANTLR Visitor that walks a parse tree and collects all IDENTIFIER references.
 *
 * <p>Only {@link LineageParser.VarContext} nodes (the {@code # Var} labelled alternative)
 * are collected. NUMBER constants ({@code # Const}) are explicitly ignored,
 * so purely numeric expressions such as {@code 42} correctly produce an empty set.
 *
 * <p>The visitor recurses into every sub-rule automatically via the default
 * {@link LineageBaseVisitor#visitChildren} mechanism, so nested expressions,
 * {@code if/then/else} branches, and condition operands are all covered.
 */
public final class VariableReferenceVisitor extends LineageBaseVisitor<Void> {

    private final Set<String> referencedVariables = new HashSet<>();

    /**
     * Collects the IDENTIFIER text when visiting a {@code Var} alternative.
     * The keywords {@code if}, {@code then}, {@code else} are handled by dedicated
     * lexer tokens and will never appear as IDENTIFIER tokens, so no keyword
     * filtering is needed here.
     */
    @Override
    public Void visitVar(LineageParser.VarContext ctx) {
        referencedVariables.add(ctx.IDENTIFIER().getText());
        return null;
    }

    /**
     * Const nodes (numeric literals) are deliberately not overridden — they will
     * use {@code visitChildren} which returns null without adding anything.
     */

    /**
     * @return the set of variable names found during the tree walk; never null.
     */
    public Set<String> getReferencedVariables() {
        return referencedVariables;
    }
}
