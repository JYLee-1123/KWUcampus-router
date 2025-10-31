package com.campus.kwangwoon.service;

import com.campus.kwangwoon.graph.Graph;
import com.campus.kwangwoon.model.BuildingInfo;

// pom.xml의 Spring Boot 버전에 맞춰 jakarta.annotation을 사용합니다.
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service // Spring Boot에게 이 클래스를 '서비스'로 등록하라고 알림
public class GraphService {

    // 로드된 그래프 객체를 보관할 변수
    private Graph graph;
    // 프론트엔드 제공용 '빌딩 정보 리스트'
    private List<BuildingInfo> buildingInfoList;
    // /api/find 로직에서 사용할 '내부용 게이트 맵'
    private Map<String, List<String>> buildingGateMap;

    /**
     * @PostConstruct 어노테이션:
     *                Spring이 GraphService 객체를 만든 직후,
     *                (그리고 다른 요청을 받기 전에) 이 메소드를 딱 한 번 자동 실행합니다.
     */
    @PostConstruct
    public void init() throws IOException, URISyntaxException {
        System.out.println("...[GraphService] 그래프 데이터 로딩을 시작합니다...");

        // Demo.java에 있던 데이터 폴더 찾는 로직을 그대로 가져옵니다.
        Path dataDir = resolveDataDir();

        // Graph.load()를 호출해 그래프를 로드하고 멤버 변수에 저장합니다.
        this.graph = Graph.load(dataDir); //
        System.out.println(this.graph.summary());

        System.out.println("...[GraphService] 빌딩 정보 로딩 시작...");
        ObjectMapper om = new ObjectMapper();
        Path buildingPath = dataDir.resolve("building.json");

        // 1. 'buildings.json'을 List<BuildingInfo>로 파싱
        this.buildingInfoList = om.readValue(
                Files.newBufferedReader(buildingPath),
                new TypeReference<List<BuildingInfo>>() {
                });

        // 2. 파싱한 리스트에서 Map<id, gates> 형태의 내부용 맵을 생성
        this.buildingGateMap = this.buildingInfoList.stream()
                .collect(Collectors.toMap(
                        BuildingInfo::getId, // Key = "BimaHall"
                        BuildingInfo::getGates // Value = ["BH-MG", "BH-SG", ...]
                ));

        System.out.println("...[GraphService] 빌딩 정보 로딩 완료: " + this.buildingInfoList.size() + "개 빌딩");
    }

    /**
     * 다른 클래스(예: RouteController)가 로드된 그래프를
     * 가져다 쓸 수 있도록 제공(return)하는 메소드
     */
    public Graph getGraph() {
        if (this.graph == null) {
            // 서버 시작 시 init()이 실패했을 경우의 방어 코드
            throw new IllegalStateException("그래프가 정상적으로 로드되지 않았습니다.");
        }
        return this.graph;
    }

    // 1. (수정) /api/find 로직이 사용할 Getter
    public Map<String, List<String>> getBuildingGateMap() {
        if (this.buildingGateMap == null) {
            throw new IllegalStateException("빌딩-게이트 맵이 로드되지 않았습니다.");
        }
        return this.buildingGateMap;
    }

    // 2. (추가) 프론트엔드가 사용할 Getter
    public List<BuildingInfo> getBuildingInfoList() {
        if (this.buildingInfoList == null) {
            throw new IllegalStateException("빌딩 정보 리스트가 로드되지 않았습니다.");
        }
        return this.buildingInfoList;
    }

    /**
     * Demo.java에 있던 static 메소드를 그대로 가져옴
     * 'resources/data' 폴더의 실제 경로를 찾아줍니다.
     */
    private static Path resolveDataDir() throws URISyntaxException {
        // (Demo.class -> GraphService.class로 클래스 이름만 변경)
        URL url = GraphService.class.getClassLoader().getResource("data");
        if (url == null) {
            throw new RuntimeException("resources/data 폴더를 찾을 수 없습니다. (경로 확인 필요)");
        }
        return Paths.get(url.toURI());
    }
}