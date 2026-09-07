package com.lineage.loader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lineage.model.Node;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a list of {@link Node} objects from a JSON file.
 *
 * <h2>Expected JSON format</h2>
 * <pre>
 * [
 *   { "id": 1, "name": "price", "expression": "100" },
 *   { "id": 2, "name": "qty",   "expression": "5"   },
 *   ...
 * ]
 * </pre>
 *
 * <ul>
 *   <li>{@code id} may be a numeric value; it is stored as a String internally.</li>
 *   <li>{@code name} must be a non-blank string.</li>
 *   <li>{@code expression} must be a string (may be an empty string for leaf nodes
 *       that evaluate to a constant — however the parser will report these as
 *       failures if completely blank).</li>
 * </ul>
 *
 * <p>JSON parsing logic is intentionally kept separate from graph-algorithm classes.
 */
public final class NodeJsonLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Loads nodes from the given JSON file.
     *
     * @param file the JSON file to read
     * @return list of nodes in file order
     * @throws IOException              if the file cannot be read or is malformed JSON
     * @throws IllegalArgumentException if a required field is missing or invalid
     */
    public List<Node> load(File file) throws IOException {
        JsonNode root = MAPPER.readTree(file);

        if (!root.isArray()) {
            throw new IllegalArgumentException(
                "JSON file must contain a top-level array of node objects, found: "
                + root.getNodeType());
        }

        List<Node> nodes = new ArrayList<>(root.size());
        for (int i = 0; i < root.size(); i++) {
            JsonNode obj = root.get(i);
            nodes.add(parseNode(obj, i));
        }
        return nodes;
    }

    /**
     * Loads nodes from a JSON string (useful for tests).
     *
     * @param json the JSON string
     * @return list of nodes
     * @throws IOException if parsing fails
     */
    public List<Node> loadFromString(String json) throws IOException {
        JsonNode root = MAPPER.readTree(json);

        if (!root.isArray()) {
            throw new IllegalArgumentException("JSON must be a top-level array");
        }

        List<Node> nodes = new ArrayList<>(root.size());
        for (int i = 0; i < root.size(); i++) {
            nodes.add(parseNode(root.get(i), i));
        }
        return nodes;
    }

    private Node parseNode(JsonNode obj, int index) {
        JsonNode idNode         = obj.get("id");
        JsonNode nameNode       = obj.get("name");
        JsonNode expressionNode = obj.get("expression");

        if (idNode == null) {
            throw new IllegalArgumentException("Node at index " + index + " is missing required field 'id'");
        }
        if (nameNode == null) {
            throw new IllegalArgumentException("Node at index " + index + " is missing required field 'name'");
        }
        if (expressionNode == null) {
            throw new IllegalArgumentException("Node at index " + index + " is missing required field 'expression'");
        }

        // Accept both numeric and string IDs
        String id         = idNode.asText();
        String name       = nameNode.asText();
        String expression = expressionNode.asText();

        return new Node(id, name, expression);
    }
}
