#!/usr/bin/env bash
# =============================================================================
# setup.sh — Set up the Expression Parser & Lineage Graph project
#
# What this script does:
#   1. Validates that Java (≥11) and Maven are installed.
#   2. Downloads all Maven dependencies (including ANTLR4 runtime and Jackson).
#   3. Generates the ANTLR4 lexer/parser Java source files from grammar/Lineage.g4.
#   4. Compiles all source code (main + tests).
#   5. Packages the fat JAR (target/expression-parser-lineage-1.0.0.jar).
#
# Assumptions:
#   - Java 11+ is installed and on the PATH.
#   - Maven 3.6+ is installed and on the PATH.
#   - An internet connection is available for the first run (to download deps).
#
# Usage:
#   chmod +x setup.sh
#   ./setup.sh
# =============================================================================

set -e  # Exit immediately on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================="
echo " Expression Parser & Lineage Graph — Setup"
echo "=============================================="
echo ""

# -----------------------------------------------------------------------
# 1. Validate Java
# -----------------------------------------------------------------------
if ! command -v java &>/dev/null; then
    echo "ERROR: Java is not installed or not on PATH."
    echo "       Please install Java 11 or later and try again."
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d. -f1)
if [ "$JAVA_VERSION" -lt 11 ] 2>/dev/null; then
    echo "ERROR: Java 11 or later is required. Found Java $JAVA_VERSION."
    exit 1
fi
echo "[OK] Java found: $(java -version 2>&1 | head -n 1)"

# -----------------------------------------------------------------------
# 2. Validate Maven
# -----------------------------------------------------------------------
if ! command -v mvn &>/dev/null; then
    echo "ERROR: Maven is not installed or not on PATH."
    echo "       Please install Maven 3.6+ and try again."
    exit 1
fi
echo "[OK] Maven found: $(mvn --version | head -n 1)"
echo ""

# -----------------------------------------------------------------------
# 3. Build (download deps + generate ANTLR sources + compile + package)
# -----------------------------------------------------------------------
echo "Running: mvn clean package -q"
echo "(This may take a minute on first run while downloading dependencies.)"
echo ""

mvn clean package -q

echo ""
echo "=============================================="
echo " Setup complete!"
echo ""
echo " Generated artifacts:"
echo "   target/expression-parser-lineage-1.0.0.jar  (fat JAR)"
echo ""
echo " Generated ANTLR sources:"
echo "   target/generated-sources/antlr4/com/lineage/parser/generated/"
echo ""
echo " To run the application:"
echo "   ./run.sh sample.json validate"
echo "   ./run.sh sample.json upstream 7"
echo "   ./run.sh sample.json downstream 1"
echo "   ./run.sh sample.json paths 7 1"
echo ""
echo " To run tests:"
echo "   mvn test"
echo "=============================================="
