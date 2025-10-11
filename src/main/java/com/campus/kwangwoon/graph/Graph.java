package com.campus.kwangwoon.graph;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.campus.kwangwoon.model.Node;
import com.campus.kwangwoon.model.Edge;
import com.campus.kwangwoon.model.Point;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Graph loader/holder:
 * - load(meta.json, node.json, edge.json)
 * - auto add reverse edges if directed=false
 * - validate node/edge consistency
 */
public class Graph {

    public static final class Meta {
        public boolean directed = false;
        public String unit = "meter";
        public String schemaVersion = "1.0.0";
    }

    private final Meta meta;
    private final Map<String, Node> nodesById;
    private final Map<String, List<Edge>> adj; // outgoing edges

    private Graph(Meta meta,
            Map<String, Node> nodesById,
            Map<String, List<Edge>> adj) {
        this.meta = meta;
        this.nodesById = nodesById;
        this.adj = adj;
    }

    public Meta meta() {
        return meta;
    }

    public Collection<Node> nodes() {
        return nodesById.values();
    }

    public Node getNode(String id) {
        return nodesById.get(id);
    }

    public List<Edge> outgoing(String nodeId) {
        return adj.getOrDefault(nodeId, List.of());
    }

    /** Convenience: number of edges (counting directed arcs actually stored). */
    public int edgeCount() {
        int sum = 0;
        for (List<Edge> list : adj.values())
            sum += list.size();
        return sum;
    }

    /**
     * Load graph from a directory that contains meta.json, node.json, edge.json.
     */
    public static Graph load(Path dir) throws IOException {
        ObjectMapper om = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 1) meta
        Meta meta = readMeta(om, dir.resolve("meta.json"));

        // 2) nodes
        List<Node> nodeList = om.readValue(
                Files.newBufferedReader(dir.resolve("node.json")),
                new TypeReference<List<Node>>() {
                });
        Map<String, Node> nodesById = new LinkedHashMap<>();
        for (Node n : nodeList) {
            if (n.getId() == null || n.getId().isBlank())
                throw new IllegalArgumentException("Node id is missing: " + n);
            if (nodesById.put(n.getId(), n) != null)
                throw new IllegalArgumentException("Duplicated node id: " + n.getId());
        }

        // 3) edges
        List<Edge> rawEdges = om.readValue(
                Files.newBufferedReader(dir.resolve("edge.json")),
                new TypeReference<List<Edge>>() {
                });

        // 4) build adjacency, validate and auto-add reverse if needed
        Map<String, List<Edge>> adj = new LinkedHashMap<>();
        for (String id : nodesById.keySet())
            adj.put(id, new ArrayList<>());

        for (Edge e : rawEdges) {
            // basic validation
            if (!nodesById.containsKey(e.getFrom()))
                throw new IllegalArgumentException("Edge.from not found: " + e.getFrom());
            if (!nodesById.containsKey(e.getTo()))
                throw new IllegalArgumentException("Edge.to not found: " + e.getTo());
            if (Double.isNaN(e.getWeight()) || e.getWeight() <= 0)
                throw new IllegalArgumentException("Edge.weight must be >0: " + e);

            adj.get(e.getFrom()).add(e);

            // reverse
            if (!meta.directed) {
                Edge rev = new Edge();
                rev.setFrom(e.getTo());
                rev.setTo(e.getFrom());
                rev.setWeight(e.getWeight());
                rev.setStair(e.isStair());
                rev.setCrub(e.isCrub());
                // geometry 역방향: 단순히 뒤집기 (있으면)
                if (e.getGeometry() != null && !e.getGeometry().isEmpty()) {
                    List<Point> g = new ArrayList<>(e.getGeometry());
                    Collections.reverse(g);
                    rev.setGeometry(g);
                }
                adj.get(rev.getFrom()).add(rev);
            }
        }

        // (선택) 경고만: 노드 좌표가 터무니없이 먼 경우 등
        // 사용자가 "GH-2MG 좌표 수정하지 말라"고 하셨으므로, 여기서는 수정하지 않고 경고만 가능.
        // validateCoordinates(nodesById); // 필요시 구현

        return new Graph(meta, nodesById, adj);
    }

    private static Meta readMeta(ObjectMapper om, Path path) throws IOException {
        if (!Files.exists(path)) {
            // default
            Meta m = new Meta();
            return m;
        }
        return om.readValue(Files.newBufferedReader(path), Meta.class);
    }

    /** 간단한 통계 출력용 */
    public String summary() {
        int n = nodesById.size();
        int m = edgeCount();
        return String.format("Graph: nodes=%d, arcs=%d (directed=%s, unit=%s, schema=%s)",
                n, m, meta.directed, meta.unit, meta.schemaVersion);
    }
}
