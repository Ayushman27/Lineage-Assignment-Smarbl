#!/usr/bin/env bash
# =============================================================================
# run.sh — Run the Expression Parser & Lineage Graph CLI
#
# Usage:
#   ./run.sh <input.json> validate
#   ./run.sh <input.json> upstream   <nodeId>
#   ./run.sh <input.json> downstream <nodeId>
#   ./run.sh <input.json> paths      <fromId> <toId>
#
# Examples:
#   ./run.sh sample.json validate
#   ./run.sh sample.json upstream 7
#   ./run.sh sample.json downstream 1
#   ./run.sh sample.json paths 7 1
#
# The script will recompile if the JAR does not exist.
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

JAR="target/expression-parser-lineage-1.0.0.jar"

# -----------------------------------------------------------------------
# Check arguments
# -----------------------------------------------------------------------
if [ "$#" -lt 2 ]; then
    echo ""
    echo "Usage:"
    echo "  ./run.sh <input.json> validate"
    echo "  ./run.sh <input.json> upstream   <nodeId>"
    echo "  ./run.sh <input.json> downstream <nodeId>"
    echo "  ./run.sh <input.json> paths      <fromId> <toId>"
    echo ""
    echo "Examples:"
    echo "  ./run.sh sample.json validate"
    echo "  ./run.sh sample.json upstream 7"
    echo "  ./run.sh sample.json downstream 1"
    echo "  ./run.sh sample.json paths 7 1"
    echo ""
    exit 1
fi

# -----------------------------------------------------------------------
# Build if JAR doesn't exist
# -----------------------------------------------------------------------
if [ ! -f "$JAR" ]; then
    echo "JAR not found. Running setup first..."
    ./setup.sh
fi

# -----------------------------------------------------------------------
# Run
# -----------------------------------------------------------------------
java -jar "$JAR" "$@"
