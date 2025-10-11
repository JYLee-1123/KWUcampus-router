package com.campus.kwangwoon;

import com.campus.kwangwoon.graph.Graph;
import com.campus.kwangwoon.graph.Router;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Demo {
    private static Path resolveDataDir() throws URISyntaxException {
        // 클래스패스의 /data 폴더를 Path로 변환
        var url = Demo.class.getClassLoader().getResource("data");
        if (url == null)
            throw new RuntimeException("resources/data 폴더를 찾을 수 없습니다.");
        return Paths.get(url.toURI());
    }

    public static void main(String[] args) throws Exception {
        Path dataDir = resolveDataDir(); // ← 여기만 바뀜
        Graph g = Graph.load(dataDir);
        System.out.println(g.summary());

        Router.Options opt = new Router.Options();
        opt.avoidStair = true;
        opt.avoidCrub = false;

        Router.RouteResult res = Router.shortestPath(g, "BH-MG", "CH-MG", opt);
        if (res == null) {
            System.out.println("경로 없음");
        } else {
            System.out.println("경로: " + String.join(" -> ", res.path));
            System.out.println("총 비용: " + res.cost);
        }
    }
}
