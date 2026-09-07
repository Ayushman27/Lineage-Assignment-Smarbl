# Expression Parser & Lineage Graph

A production-quality Java system that reads JSON node definitions, parses their expressions with **ANTLR4**, builds a directed **data lineage dependency graph**, detects cycles, pre-computes transitive dependency closures, and finds all simple paths between nodes — all accessible via a clean CLI.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Dependency Direction](#dependency-direction)
4. [Parser Design](#parser-design)
5. [Graph Design](#graph-design)
6. [Cache Design](#cache-design)
7. [Cycle Detection](#cycle-detection)
8. [Path Finding](#path-finding)
9. [Error Handling](#error-handling)
10. [Complexity](#complexity)
11. [Benchmark](#benchmark)
12. [Setup](#setup)
13. [Usage](#usage)
14. [Tests](#tests)
15. [Design Tradeoffs](#design-tradeoffs)

---

## Project Overview

This system models data lineage — the question of *where data comes from and where it flows to* — as a directed acyclic graph (DAG).

Each node represents a computed value (e.g., `revenue = price * qty`). The system:

- Parses each node's expression using a formal ANTLR4 grammar to extract variable references
- Builds directed edges from each dependent node to each node it references
- Stores both forward (uses) and backward (usedBy) adjacency for O(1) bidirectional lookup
- Detects any cycles (which would mean a circular computation dependency)
- Pre-computes all transitive upstream and downstream closures for O(1) repeated queries
- Finds all distinct simple paths between any two nodes

---

## Architecture

```
JSON file
  → NodeJsonLoader        (Jackson: JSON → List<Node>)
  → ExpressionParser      (ANTLR4: expression string → Set<String> variable names)
  → LineageGraph          (builds directed edges, detects cycles)
  → DependencyCache       (pre-computes transitive upstream/downstream closures)
  → PathFinder            (DFS + backtracking: all simple paths)
  → CLI / Main            (user interaction, output formatting)
```

### Package Structure

```
src/main/java/com/lineage/
├── model/
│   └── Node.java                    — immutable node data object
├── parser/
│   ├── ParseResult.java             — success/failure discriminated union
│   ├── ExpressionParser.java        — orchestrates lexer→parser→visitor
│   ├── VariableReferenceVisitor.java — ANTLR visitor: collects IDENTIFIER nodes
│   ├── LineageErrorListener.java    — captures ANTLR errors programmatically
│   └── generated/                   — auto-generated ANTLR classes (build-time)
├── graph/
│   ├── LineageGraph.java            — directed graph with bidirectional adjacency
│   ├── CycleDetector.java           — iterative DFS with 3-color marking
│   ├── DependencyCache.java         — memoized DFS transitive closure
│   ├── PathFinder.java              — DFS + backtracking for all simple paths
│   ├── CycleException.java          — domain exception for cycles
│   └── ExpressionParseException.java — domain exception for parse failures
├── loader/
│   └── NodeJsonLoader.java          — Jackson JSON → List<Node>
├── cli/
│   ├── Main.java                    — CLI entry point
│   └── PathFormatter.java           — presentation utilities
└── benchmark/
    └── BenchmarkRunner.java         — 10k-node performance measurement
```

---

## Dependency Direction

Edges always point **from dependent to dependency** (consumer to producer):

```
revenue = price * qty   →   revenue → price
                            revenue → qty

margin = revenue - cost →   margin → revenue
                            margin → cost
```

The graph stores **both directions**:

| Direction | Meaning        | Example              |
|-----------|----------------|----------------------|
| `outgoing` (uses)   | what a node depends on | `revenue → {price, qty}` |
| `incoming` (usedBy) | what depends on a node | `price ← {revenue}` |

---

## Parser Design

Expression parsing uses the **ANTLR4** toolchain with a custom grammar (`grammar/Lineage.g4`).

### Pipeline

```
expression string
  → LineageLexer         (tokenises: IDENTIFIER, NUMBER, operators, keywords)
  → LineageParser        (builds parse tree for grammar rule: expr)
  → VariableReferenceVisitor  (walks tree, collects Var nodes → Set<String>)
  → ParseResult          (success with variable set, or failure with error)
```

### Key Design Points

- **`VariableReferenceVisitor`** overrides only `visitVar()` (the `# Var` alternative in `expr`). NUMBER tokens are in the `# Const` alternative and are never overridden — they fall through to `visitChildren()` which returns null without collecting anything.
- **`LineageErrorListener`** replaces ANTLR's default `ConsoleErrorListener` on both the lexer and parser. All syntax errors are captured in a list; no error output goes to stderr during parsing.
- **`ExpressionParser`** wraps the entire pipeline in a `try/catch`. Any unexpected exception becomes a `ParseResult.failure(...)` — callers are never exposed to unhandled exceptions.
- **Empty/blank expressions** → failure (not an empty success set).

### ParseResult

```
ParseResult (success) → Set<String> referencedVariables
ParseResult (failure) → String errorMessage, String originalExpression
```

### Grammar Highlights

The grammar (unchanged from specification) supports:
- Binary operators: `+`, `-`, `*`, `/`
- Parenthesised expressions
- `if (condition) then expr else expr` with comparison operators `==`, `!=`, `<`, `<=`, `>`, `>=`
- IDENTIFIER tokens: `[a-zA-Z_][a-zA-Z_0-9]*`
- NUMBER tokens: `[0-9]+ ('.' [0-9]+)?` (integer and decimal)
- Keywords (`if`, `then`, `else`) are dedicated lexer tokens and never appear as IDENTIFIER

---

## Graph Design

`LineageGraph` maintains:

```java
Map<String, Node>     nodesById     // id → Node (O(1) lookup by id)
Map<String, Node>     nodesByName   // name → Node (used when resolving expression refs)
Map<String, Set<Node>> outgoing     // nodeId → set of direct dependencies
Map<String, Set<Node>> incoming     // nodeId → set of direct dependents
```

`LinkedHashSet` is used for adjacency sets to avoid duplicate edges while preserving insertion order.

### Construction

1. All nodes are indexed first (so forward references resolve correctly)
2. Each expression is parsed with `ExpressionParser`
3. Referenced variable names are resolved in `nodesByName`
4. Unresolved names (external constants) are silently skipped — no edge created
5. `CycleDetector` is run immediately after all edges are built

### Parse Failure Policy

If **any** node's expression cannot be parsed, construction throws `ExpressionParseException` immediately. This ensures the graph is never in a partially-built, silently-incorrect state. The exception message identifies the failing node by id, name, and expression.

---

## Cache Design

`DependencyCache` pre-computes transitive closures using **memoized DFS** (post-order):

```
upstream(N)   = all nodes N directly or transitively depends on
                (closure along outgoing edges)

downstream(N) = all nodes that directly or transitively depend on N
                (closure along incoming edges)
```

Pre-computation traverses each node once per direction. After that:

```java
cache.getUpstream("7")   // O(1) — direct HashMap lookup
cache.getDownstream("1") // O(1) — direct HashMap lookup
```

Unknown node IDs return `Collections.emptySet()` — no exception thrown.

---

## Cycle Detection

Uses **iterative DFS with 3-colour marking** (WHITE → GREY → BLACK):

- **WHITE**: not yet visited
- **GREY**: on the current DFS path (back-edge to GREY = cycle)
- **BLACK**: fully explored (no cycle through this node)

Iterative (not recursive) DFS is used to avoid `StackOverflowError` on very deep graphs. When a cycle is found, the algorithm traces back through the current path to identify the involved nodes and includes them in the `CycleException` message.

Cycle detection runs immediately after graph construction, before any dependency cache is built. A cyclic graph cannot proceed.

---

## Path Finding

`PathFinder.findPaths(fromId, toId)` uses **DFS with backtracking**:

1. Start at `from`, add to `currentPath` and `visited` set
2. If `current == target`, record a copy of `currentPath` as a result
3. Otherwise, for each unvisited outgoing neighbour, recurse
4. Backtrack: remove `current` from `currentPath` and `visited`

This guarantees:
- All returned paths are **simple** (no repeated nodes)
- **All** distinct simple paths are returned
- No **duplicate** paths

If either node ID is unknown, an empty list is returned immediately.

---

## Error Handling

| Scenario | Behaviour |
|----------|-----------|
| Invalid expression syntax | `ParseResult.failure(...)` returned; no exception propagated from `ExpressionParser` |
| Parse failure during graph build | `ExpressionParseException` thrown with node id, name, and expression in message |
| Missing referenced node | Treated as external constant; silently skipped; no edge created |
| Cycle in graph | `CycleException` thrown with cycle path in message |
| Unknown node ID in cache query | Empty set returned; no exception |
| Unknown node ID in path query | Empty list returned; no exception |
| Malformed JSON | `IOException` propagated from `NodeJsonLoader` |

---

## Complexity

| Operation | Time | Space |
|-----------|------|-------|
| Graph construction | O(V × E_expr) where E_expr = avg expression parse time | O(V + E) |
| Cycle detection | O(V + E) | O(V) |
| Cache pre-computation | O(V × C_avg) where C_avg = avg closure size | O(V²) worst case |
| `getUpstream` / `getDownstream` | **O(1)** (cached) | — |
| `findPaths` | O(V! / (V-k)!) worst case (exponential for dense DAGs) | O(V) per path |

### Space note

The upstream/downstream caches store sets of `Node` references. In the worst case (complete DAG / long linear chain) these sets can hold up to O(V) nodes each, giving O(V²) total space. For 10,000 nodes this is manageable (~hundreds of MB). For very large graphs (millions of nodes), consider lazy computation or approximate alternatives.

---

## Benchmark

**Environment:**
- JVM: OpenJDK 64-Bit Server VM 17.0.18 (Microsoft build)
- OS: Windows 11
- Graph topology: layered DAG — 100 leaf constant nodes, subsequent layers each referencing up to 3 nodes from the previous layer, until 10,000 total nodes

**Results (actual measured, not estimated):**

| Operation | Result |
|-----------|--------|
| Nodes | 10,000 |
| Estimated edges | ~29,700 |
| Node generation | 29.3 ms |
| Graph construction (parse + edge building + cycle detection) | 441.1 ms |
| Dependency cache pre-computation | 6,504.1 ms |
| `getUpstream` (last node, 7,499 upstream nodes) | 0.030 ms |
| `getDownstream` (first leaf node, 7,499 downstream nodes) | 0.003 ms |
| `findPaths` (200-node linear chain, 1 path) | 0.539 ms |

### Cache pre-computation note

The 6.5s cache time reflects the **worst-case behaviour of a highly connected layered DAG**: each node's transitive closure can contain ~7,500 other nodes. Computing and union-ing these large sets across 10,000 nodes approaches O(V²) work. For a sparser real-world graph (where most nodes have small closures), this would be significantly faster.

### Path-finding note

Path-finding on the layered 10,000-node DAG (fan-in=3) is intentionally not benchmarked: the number of distinct simple paths is exponential (~3^layers), which is an inherent property of all-simple-paths algorithms on dense DAGs. For practical lineage graphs (sparse, shallow), path-finding is fast. The 200-node linear chain measurement (0.5 ms) demonstrates the baseline traversal speed.

---

## Setup

### Prerequisites

- Java 11 or later (tested on OpenJDK 17)
- Maven 3.6 or later

### Running setup

```bash
chmod +x setup.sh
./setup.sh
```

This will:
1. Validate Java and Maven are installed
2. Download all Maven dependencies (first run requires internet)
3. Generate ANTLR4 lexer/parser from `grammar/Lineage.g4`
4. Compile all sources (main + tests)
5. Package the fat JAR at `target/expression-parser-lineage-1.0.0.jar`

On Windows, run directly with Maven:
```cmd
"C:\apache-maven-3.9.14\bin\mvn.cmd" clean package
```

---

## Usage

### Via run.sh (Linux/macOS)

```bash
# Validate graph (build + cycle detection)
./run.sh sample.json validate

# Upstream transitive dependencies of node 7 (report)
./run.sh sample.json upstream 7

# Downstream transitive dependents of node 1 (price)
./run.sh sample.json downstream 1

# All simple paths from node 7 (report) to node 1 (price)
./run.sh sample.json paths 7 1
```

### Via java directly (Windows / cross-platform)

```cmd
java -jar target\expression-parser-lineage-1.0.0.jar sample.json validate
java -jar target\expression-parser-lineage-1.0.0.jar sample.json upstream 7
java -jar target\expression-parser-lineage-1.0.0.jar sample.json downstream 1
java -jar target\expression-parser-lineage-1.0.0.jar sample.json paths 7 1
```

### Sample output

**validate:**
```
Loaded 7 node(s) from sample.json
Graph built successfully. Nodes: 7
Dependency cache built.

Graph is valid (no cycles detected).
```

**upstream 7:**
```
Upstream of node 7 (6 nodes):
cost, margin, price, qty, revenue, threshold
```

**downstream 1:**
```
Downstream of node 1 (3 nodes):
margin, report, revenue
```

**paths 7 1:**
```
Paths from node 7 to node 1 (1 path(s) found):
Path 1: report -> margin -> revenue -> price
```

### JSON input format

```json
[
  { "id": 1, "name": "price",     "expression": "100" },
  { "id": 2, "name": "qty",       "expression": "5" },
  { "id": 3, "name": "revenue",   "expression": "price * qty" },
  { "id": 4, "name": "cost",      "expression": "300" },
  { "id": 5, "name": "margin",    "expression": "revenue - cost" },
  { "id": 6, "name": "threshold", "expression": "500" },
  { "id": 7, "name": "report",    "expression": "if (margin > threshold) then margin else 0" }
]
```

- `id`: numeric or string, must be unique
- `name`: string, must be unique, used in expressions
- `expression`: any expression supported by the grammar (see [Grammar](#grammar-highlights))

---

## Tests

Run all tests:

```bash
# Linux/macOS
./setup.sh  # if not already built
mvn test

# Windows
"C:\apache-maven-3.9.14\bin\mvn.cmd" test
```

**Test suite: 77 tests, 0 failures**

| Test class | Tests | Covers |
|------------|-------|--------|
| `ExpressionParserTest` | 28 | Subtraction, multiplication, division, addition, parentheses, if/then/else (all 6 operators), numeric constants, decimal numbers, underscores, digits in identifiers, duplicates, nested expressions, whitespace/newlines, null/empty/blank, malformed expressions |
| `LineageGraphTest` | 14 | Constant nodes, basic edges, chained deps, missing references, duplicate references, bidirectional consistency, multiple dependents, lookup by name/id, canonical graph, invalid expressions, duplicate ids/names, empty graph |
| `CycleDetectorTest` | 8 | Empty graph, single node, linear chain, canonical graph (no cycle), self-cycle, 2-node cycle, larger 3-node cycle, deep embedded cycle |
| `DependencyCacheTest` | 13 | Upstream of leaf, direct upstream, transitive upstream, full upstream of report, downstream of root, direct downstream, transitive downstream, unknown id (upstream), unknown id (downstream), isolated node, idempotent repeated queries |
| `PathFinderTest` | 14 | Single path, diamond (2 paths), no path (reversed), no path (disconnected), same-node, unknown fromId, unknown toId, both unknown, all paths simple, no duplicate paths, branching graph, path start/end correctness |

---

## Design Tradeoffs

### Pre-computation vs lazy computation

**Chosen**: Pre-compute all closures on graph construction.
- ✅ O(1) query time — best for systems where queries are frequent
- ✅ Errors in the graph surface at build time, not query time
- ❌ O(V²) space in worst case (dense graphs)
- ❌ Slow build time for very dense, large graphs (measured: 6.5s for 10k highly connected nodes)

**Alternative**: Lazy DFS with memoisation on first query.
- ✅ Faster build time
- ❌ First query per node is slow; harder to predict query latency

### Iterative DFS for cycle detection

**Chosen**: Explicit stack, not recursion.
- ✅ No `StackOverflowError` on deep graphs
- ✅ Works correctly for thousands of nodes
- ❌ Slightly more code than recursive version

### All-simple-paths

**Chosen**: DFS + backtracking returning all paths.
- ✅ Correct and complete
- ❌ Exponential worst case on dense DAGs (inherent to the problem)
- For production systems with very dense graphs, consider limiting to shortest paths or paths up to a length limit

### Jackson vs Gson

**Chosen**: Jackson (more widely used, better performance for large files, familiar to most Java developers).

### No Spring Boot

**Intentional**: The system is a standalone CLI tool. Spring Boot would add significant startup latency (~2-5 seconds) and classpath complexity for no benefit. Standard Java with Jackson is the right choice here.

### Fail-fast on parse errors

**Chosen**: Throw `ExpressionParseException` on the first bad expression during graph construction.
- ✅ No silently-incorrect partial graphs
- ✅ Clear error message with node id, name, expression, and parser error details
- Alternative: collect all errors and report them in batch (useful for UI tools, not CLI tools)
