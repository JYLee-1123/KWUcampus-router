package com.campus.kwangwoon.graph;

import com.campus.kwangwoon.model.Edge;

import java.util.*;

/**
 * Dijkstra shortest path on Graph with simple filters:
 * - avoidStair: 계단 금지
 * - avoidCrub: 턱(연석) 금지
 * Uses Edge.weight as cost.
 */
public class Router {

    public static final class Options {
        public boolean avoidStair = false;
        public boolean avoidCrub = false;
    }

    public static final class RouteResult {
        public final List<String> path; // node id path
        public final double cost;

        public RouteResult(List<String> path, double cost) {
            this.path = path;
            this.cost = cost;
        }
    }

    public static RouteResult shortestPath(Graph g, String src, String dst, Options opt) {
        if (g.getNode(src) == null)
            throw new IllegalArgumentException("Unknown source: " + src);
        if (g.getNode(dst) == null)
            throw new IllegalArgumentException("Unknown target: " + dst);
        if (src.equals(dst))
            return new RouteResult(List.of(src), 0.0);

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
                if (opt != null) {
                    if (opt.avoidStair && e.isStair())
                        continue;
                    if (opt.avoidCrub && e.isCrub())
                        continue;
                }
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

        if (!prev.containsKey(dst) && !src.equals(dst)) {
            return null; // no route
        }

        // reconstruct
        LinkedList<String> path = new LinkedList<>();
        String cur = dst;
        path.addFirst(cur);
        while (!cur.equals(src)) {
            cur = prev.get(cur);
            if (cur == null)
                break;
            path.addFirst(cur);
        }
        return new RouteResult(path, dist.get(dst));
    }
}
