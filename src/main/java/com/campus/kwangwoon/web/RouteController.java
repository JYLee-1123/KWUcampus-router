package com.campus.kwangwoon.web;

import com.campus.kwangwoon.graph.Graph;
import com.campus.kwangwoon.graph.Router;
import com.campus.kwangwoon.model.Node;
import com.campus.kwangwoon.model.Point;
import com.campus.kwangwoon.model.BuildingInfo;
import com.campus.kwangwoon.model.Edge;
import com.campus.kwangwoon.model.Feedback; // [추가] Entity
import com.campus.kwangwoon.model.FeedbackRequest; // [추가] DTO
import com.campus.kwangwoon.repository.FeedbackRepository; // [추가] Repository
import com.campus.kwangwoon.service.GraphService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@RestController // 이 클래스가 JSON을 반환하는 API 컨트롤러임을 선언
@RequestMapping("/api") // 이 클래스의 모든 API 주소는 "/api"로 시작
public class RouteController {

    // @Autowired: Spring Boot가 2단계에서 만든 GraphService를 여기에 자동으로 연결
    @Autowired
    private GraphService graphService;

    @Autowired
    private FeedbackRepository feedbackRepository;

    // 1. [사용자용] 불편 신고 접수 (DB에 저장)
    @PostMapping("/feedback")
    public ResponseEntity<String> receiveFeedback(@RequestBody FeedbackRequest req) {
        // 받은 요청(Request)을 DB용 객체(Entity)로 변환
        Feedback feedback = new Feedback(
                req.getCategory(),
                req.getContent(),
                req.getLat(),
                req.getLng());

        // DB 저장 (INSERT)
        feedbackRepository.save(feedback);

        System.out.println("📢 DB 저장됨: " + feedback.getContent());
        return ResponseEntity.ok("접수 완료");
    }

    // 2. [운영자용] 신고 목록 조회 (DB에서 조회)
    @GetMapping("/feedback")
    public List<Feedback> getFeedbackList() {
        // DB 조회 (SELECT)
        List<Feedback> list = feedbackRepository.findAll();
        Collections.reverse(list); // 최신순 정렬
        return list;
    }

    // ▼▼▼ [신규] 프론트엔드용 API: 빌딩 목록 전체 반환 ▼▼▼
    /**
     * 프론트엔드가 목적지 드롭다운을 채우기 위해 호출할 API
     * 예: http://localhost:8080/api/buildings
     */
    @GetMapping("/buildings")
    public List<BuildingInfo> getBuildingList() {
        // [ { "id": "BimaHall", "name": "비마관", ... }, { ... } ]
        return graphService.getBuildingInfoList();
    }

    /**
     * 경로 탐색 API
     * 예: http://localhost:8080/api/route?start=BH-MG&end=CH-MG&avoidStair=true
     */
    @GetMapping("/route") // GET 방식의 /api/route 주소 요청을 이 메소드가 처리
    public ResponseEntity<Router.RouteResult> findRoute(
            // @RequestParam: URL의 파라미터(물음표 뒤) 값을 읽어옴
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "false") boolean avoidStair,
            @RequestParam(defaultValue = "false") boolean avoidCrub) {

        // 1. 2단계에서 만든 서비스로부터 로드된 그래프를 가져옴
        Graph g = graphService.getGraph();

        // 2. Demo.java의 main 메소드와 동일한 로직 수행
        Router.Options opt = new Router.Options(); //
        opt.avoidStair = avoidStair; //
        opt.avoidCrub = avoidCrub; //

        Router.RouteResult res = Router.shortestPath(g, start, end, opt); //

        // 3. 결과 반환
        if (res == null) {
            // Router.shortestPath가 null을 반환하면 "경로 없음" (404 Not Found)
            return ResponseEntity.notFound().build();
        } else {
            // RouteResult 객체를 반환하면 Spring Boot가 자동으로 JSON으로 변환
            // 예: { "path": ["BH-MG", "...", "CH-MG"], "cost": 3.0 }
            return ResponseEntity.ok(res);
        }
    }

    /**
     * 모든 노드 목록 반환 API (프론트엔드에서 드롭다운 메뉴 만들 때 사용)
     * 예: http://localhost:8080/api/nodes
     */
    @GetMapping("/nodes")
    public Collection<Node> getAllNodes() {
        // 그래프의 모든 노드 정보를 반환
        return graphService.getGraph().nodes();
    }

    /* GPS 좌표를 그래프에 존재하는 가까운 node로 mapping하는 method 2개 */
    // 1. [신규 API] GPS 좌표로 경로 탐색
    /**
     * GPS 좌표 기반 경로 탐색 API
     * 예:
     * http://localhost:8080/api/route/gps?startLat=37.619550&startLng=127.059400&end=CH-MG
     */
    @GetMapping("/route/gps")
    public ResponseEntity<Router.RouteResult> findRouteFromGps(
            @RequestParam double startLat, // 사용자의 현재 위도
            @RequestParam double startLng, // 사용자의 현재 경도
            @RequestParam String end, // 목적지 노드 ID
            @RequestParam(defaultValue = "false") boolean avoidStair,
            @RequestParam(defaultValue = "false") boolean avoidCrub) {

        Graph g = graphService.getGraph();

        // 1. 사용자의 GPS 좌표와 가장 가까운 노드를 찾음
        String nearestStartNodeId = findNearestNode(g, startLat, startLng);
        if (nearestStartNodeId == null) {
            // 그래프에 노드가 하나도 없는 예외 상황
            return ResponseEntity.badRequest().build();
        }

        System.out.println("GPS(" + startLat + "," + startLng + ") -> 가장 가까운 노드: " + nearestStartNodeId);

        // 2. 찾은 노드를 '출발지'로 사용하여 경로 탐색 실행
        Router.Options opt = new Router.Options();
        opt.avoidStair = avoidStair;
        opt.avoidCrub = avoidCrub;
        Router.RouteResult res = Router.shortestPath(g, nearestStartNodeId, end, opt);

        if (res == null) {
            return ResponseEntity.notFound().build();
        } else {
            // (개선) 여기서 res.cost에 "내 위치 ~ nearestStartNode"까지의 실제 거리를 더해주면
            // 더 정확한 총 비용(거리)을 사용자에게 알려줄 수 있습니다.
            return ResponseEntity.ok(res);
        }
    }

    // 2. [헬퍼 메소드] 가장 가까운 노드 ID를 찾는 로직
    /**
     * 주어진 좌표(userLat, userLng)에서 가장 가까운 노드를 찾아 ID를 반환합니다.
     */
    private String findNearestNode(Graph g, double userLat, double userLng) {
        String bestNodeId = null;
        double minDistanceSq = Double.POSITIVE_INFINITY; // 최소 거리의 '제곱' (성능 최적화)

        // 그래프의 모든 노드를 순회
        for (Node node : g.nodes()) {
            Point location = node.getLocation();
            if (location == null)
                continue;

            double nodeLat = location.getLat();
            double nodeLng = location.getLng();

            // 위도/경도 차이를 이용한 간단한 유클리드 거리 제곱 계산
            // (정확한 계산을 위해서는 Haversine 공식을 써야 하지만,
            // 캠퍼스처럼 좁은 범위에서는 이 방식으로도 충분히 가장 '가까운' 노드를 찾을 수 있습니다.)
            double distSq = (userLat - nodeLat) * (userLat - nodeLat) +
                    (userLng - nodeLng) * (userLng - nodeLng);

            if (distSq < minDistanceSq) {
                minDistanceSq = distSq;
                bestNodeId = node.getId();
            }
        }
        return bestNodeId;
    }

    /**
     * [최종 API] GPS 좌표(출발지)에서 빌딩(목적지)까지 최적 경로 탐색
     * 사용자가 "비마관"을 요청하면, 옵션에 맞는(계단X) 게이트 중
     * 가장 가까운 게이트로 안내합니다.
     *
     * 예: /api/find?lat=37.619550&lng=127.059400&building=BimaHall&avoidStair=true
     */
    @GetMapping("/find")
    public ResponseEntity<Router.RouteResult> findOptimalRoute(
            @RequestParam double lat, // GPS 위도
            @RequestParam double lng, // GPS 경도
            @RequestParam String building, // "BimaHall" 같은 빌딩 이름
            @RequestParam(defaultValue = "false") boolean avoidStair,
            @RequestParam(defaultValue = "false") boolean avoidCrub) {

        Graph g = graphService.getGraph();

        // --- 1. 출발지 처리 (GPS -> 가장 가까운 노드) ---
        // (이것은 파일에 이미 있는 findNearestNode 헬퍼 메소드를 사용합니다)
        String startNodeId = findNearestNode(g, lat, lng);
        if (startNodeId == null) {
            return ResponseEntity.badRequest().build(); // 그래프에 노드 없음
        }
        System.out.println("GPS -> 시작 노드: " + startNodeId);

        // --- 2. 목적지 처리 (빌딩 이름 -> 유효한 게이트 목록) ---
        // [중요] 하드코딩 대신 GraphService에서 빌딩 정보를 가져옵니다.
        Map<String, List<String>> buildingGatesMap = graphService.getBuildingGateMap();
        List<String> targetGateIds = buildingGatesMap.get(building);

        if (targetGateIds == null || targetGateIds.isEmpty()) {
            System.out.println("요청한 빌딩(" + building + ")을 building.json에서 찾을 수 없음");
            return ResponseEntity.badRequest().build(); // building.json에 없는 빌딩 이름
        }

        // --- 3. [핵심] 목적지 게이트 필터링 (사용자 옵션 반영) ---
        // "비마관"의 모든 게이트("BH-MG", "BH-SG", "BH-BG")를 하나씩 검사
        List<String> validTargetIds = new ArrayList<>();
        for (String gateId : targetGateIds) {
            Node node = g.getNode(gateId);
            if (node == null)
                continue;

            // 사용자가 "계단 회피"를 원했는데, 이 게이트(노드) 자체가 계단이면
            if (avoidStair && node.isStair()) {
                System.out.println("목적지 필터링: " + gateId + " (계단 O) 탈락");
                continue; // 이 게이트는 사용 못함 (탈락)
            }
            // 사용자가 "턱 회피"를 원했는데, 이 게이트(노드) 자체가 턱이면
            if (avoidCrub && node.isCrub()) {
                System.out.println("목적지 필터링: " + gateId + " (턱 O) 탈락");
                continue; // 이 게이트는 사용 못함 (탈락)
            }

            // 위 조건을 통과한 게이트만 "유효한 목적지"가 됨
            validTargetIds.add(gateId);
        }

        if (validTargetIds.isEmpty()) {
            // 예: "계단 없는" 게이트를 원했지만, "비마관"의 모든 게이트가 계단일 경우
            System.out.println("옵션을 만족하는 목적지 게이트가 없음");
            return ResponseEntity.notFound().build();
        }
        System.out.println("유효한 목적지 게이트 후보: " + validTargetIds);

        // --- 4. 모든 유효한 게이트까지의 경로를 각각 탐색 ---
        Router.Options edgeOptions = new Router.Options();
        edgeOptions.avoidStair = avoidStair; // (경로상 '엣지'의 계단/턱도 피함)
        edgeOptions.avoidCrub = avoidCrub; //

        Router.RouteResult bestResult = null;

        for (String targetId : validTargetIds) {
            // (예) "BH-SG"까지의 최단 경로 탐색
            Router.RouteResult currentResult = Router.shortestPath(g, startNodeId, targetId, edgeOptions);

            if (currentResult != null) {
                // (예) "BH-BG"까지의 경로와 비교
                if (bestResult == null || currentResult.cost < bestResult.cost) {
                    bestResult = currentResult; // "BH-SG"보다 "BH-BG"가 더 가까우면 교체
                }
            }
        }

        // --- 5. 가장 짧은 경로 반환 ---
        if (bestResult == null) {
            // 유효한 게이트는 있었지만, 가는 '길(edge)'이 모두 막혔을 경우
            System.out.println("유효한 게이트는 있으나, 경로(Edge)를 찾을 수 없음");
            return ResponseEntity.notFound().build();
        }

        System.out
                .println("최종 안내 게이트: " + bestResult.path.get(bestResult.path.size() - 1) + ", 비용: " + bestResult.cost);
        return ResponseEntity.ok(bestResult);
    }

    @GetMapping("/edges")
    public List<Edge> getAllEdges() {
        Graph g = graphService.getGraph();
        List<Edge> allEdges = new ArrayList<>();

        // 그래프의 모든 노드를 돌면서, 연결된 모든 간선을 수집
        for (Node node : g.nodes()) {
            allEdges.addAll(g.outgoing(node.getId()));
        }
        return allEdges;
    }
}