package com.campus.kwangwoon.graph;

import com.campus.kwangwoon.model.Edge;
import com.campus.kwangwoon.model.Node; // [필수] Node import 추가
import java.util.*;

public class Router {

    public static final class Options {
        public boolean avoidStair = false;
        public boolean avoidCrub = false;
    }

    public static final class RouteResult {
        public final List<String> path;
        public final double cost;
        public final boolean hasStair;
        public final boolean hasCrub;

        public RouteResult(List<String> path, double cost, boolean hasStair, boolean hasCrub) {
            this.path = path;
            this.cost = cost;
            this.hasStair = hasStair;
            this.hasCrub = hasCrub;
        }
    }

    public static RouteResult shortestPath(Graph g, String src, String dst, Options opt) {
        if (g.getNode(src) == null)
            throw new IllegalArgumentException("Unknown source");
        if (g.getNode(dst) == null)
            throw new IllegalArgumentException("Unknown target");
        if (src.equals(dst))
            return new RouteResult(List.of(src), 0.0, false, false);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (var n : g.nodes())
            dist.put(n.getId(), Double.POSITIVE_INFINITY);
        dist.put(src, 0.0);

        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingDouble(dist::get));
        pq.add(src);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(dst))
                break;

            for (Edge e : g.outgoing(u)) {
                // ▼▼▼ [수정 1] 탐색 중 필터링 로직 강화 (노드 속성까지 검사) ▼▼▼
                Node targetNode = g.getNode(e.getTo()); // 도착 노드 정보 가져오기

                if (opt != null) {
                    // 1. 엣지 자체 검사
                    if (opt.avoidStair && e.isStair())
                        continue;
                    if (opt.avoidCrub && e.isCrub())
                        continue;

                    // 2. 도착 노드 검사 (여기가 핵심!)
                    if (targetNode != null) {
                        if (opt.avoidStair && targetNode.isStair())
                            continue;
                        if (opt.avoidCrub && targetNode.isCrub())
                            continue;
                    }
                }
                // ▲▲▲ [수정 끝] ▲▲▲

                String v = e.getTo();
                double nd = dist.get(u) + e.getWeight();
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                    prev.put(v, u);
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }

        if (!prev.containsKey(dst))
            return null;

        LinkedList<String> path = new LinkedList<>();
        String cur = dst;
        path.addFirst(cur);
        while (!cur.equals(src)) {
            cur = prev.get(cur);
            path.addFirst(cur);
        }

        // ▼▼▼ [수정 2] 결과 정보 생성 로직 강화 (노드 속성까지 검사) ▼▼▼
        boolean hasStair = false;
        boolean hasCrub = false;

        for (int i = 0; i < path.size() - 1; i++) {
            String u = path.get(i);
            String v = path.get(i + 1);

            for (Edge e : g.outgoing(u)) {
                if (e.getTo().equals(v)) {
                    // 1. 엣지 검사
                    if (e.isStair())
                        hasStair = true;
                    if (e.isCrub())
                        hasCrub = true;

                    // 2. 도착 노드 검사
                    Node targetNode = g.getNode(v);
                    if (targetNode != null) {
                        if (targetNode.isStair())
                            hasStair = true;
                        if (targetNode.isCrub())
                            hasCrub = true;
                    }
                    break;
                }
            }
        }
        // ▲▲▲ [수정 끝] ▲▲▲

        return new RouteResult(path, dist.get(dst), hasStair, hasCrub);
    }
}