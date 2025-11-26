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
            // 1. 노드 객체 가져오기
            Node u = nodesById.get(e.getFrom());
            Node v = nodesById.get(e.getTo());

            /*
             * JSON에 weight가 있든 없든 무조건 실제 물리적 거리를 계산해서 덮어씌움.
             * 파일 내용은 변하지 않고, 메모리 상에서만 정확한 거리로 동작함.
             */
            double distance = calculateDistance(u, v);
            e.setWeight(distance);

            // 유효성 검사 (계산된 거리가 0 이하인지 체크)
            if (e.getWeight() <= 0)
                throw new IllegalArgumentException(
                        "Calculated weight must be >0. Check coordinates: " + e.getFrom() + " -> " + e.getTo());
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

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * 두 노드(Node) 간의 실제 거리(미터)를 계산하는 헬퍼 메서드
     */
    private static double calculateDistance(Node from, Node to) {
        // 데이터 방어 로직: 좌표 정보가 없으면 에러
        if (from.getLocation() == null || to.getLocation() == null) {
            throw new IllegalArgumentException("Location missing for node: " +
                    (from.getLocation() == null ? from.getId() : to.getId()));
        }

        // Node -> Point -> lat, lng 추출 (Point.java에 getLng() 확인됨)
        return calculateDistance(
                from.getLocation().getLat(),
                from.getLocation().getLng(),
                to.getLocation().getLat(),
                to.getLocation().getLng());
    }

    /**
     * Haversine 공식을 이용한 실제 물리적 거리 계산 엔진
     */
    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                        * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
