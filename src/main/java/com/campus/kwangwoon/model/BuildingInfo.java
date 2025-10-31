package com.campus.kwangwoon.model;

import java.util.List;

/**
 * building.json 파일의 구조를 반영하는 모델 클래스
 */
public class BuildingInfo {
    private String id; // API 호출 시 사용할 ID (예: "BimaHall")
    private String name; // 프론트엔드에 표시될 이름 (예: "비마관")
    private Point location; // 빌딩의 대표 좌표 (지도 핀 표시용)
    private List<String> gates; // 이 빌딩에 속한 게이트 노드 ID 목록

    // (Jackson이 JSON 파싱에 사용할 수 있도록)
    // 기본 생성자, Getter, Setter가 필요합니다.
    // (Lombok을 사용 중이라면 @Data 어노테이션으로 대체 가능)

    public BuildingInfo() {
    }

    // (Getter와 Setter들...)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public List<String> getGates() {
        return gates;
    }

    public void setGates(List<String> gates) {
        this.gates = gates;
    }
}