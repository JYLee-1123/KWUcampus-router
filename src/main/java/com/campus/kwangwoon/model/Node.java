package com.campus.kwangwoon.model;
/*
 * 캠퍼스 지도 상의 노드(건물, 장소, 포인트)를 표현하는 클래스
 */

public class Node {
    private String id; // 노드 고유 ID (예: 비마관 정문 -> "BHMG")
    private String name; // 노드 이름 (예: 비마관 정문 -> "BimaHallMainGate")
    private Point location; // 좌표 정보 (lat, lng)
    private boolean stair; // 계단 여부 (true면 계단 있음)
    private boolean crub; // 턱 여부

    // 기본 생성자
    public Node() {
    }

    // 전체 필드 초기화 생성자
    public Node(String id, String name, Point location, boolean stair, boolean crub) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.stair = stair;
        this.crub = crub;
    }

    // Getter & Setter
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

    public boolean isStair() {
        return stair;
    }

    public void setStair(boolean stair) {
        this.stair = stair;
    }

    public boolean isCrub() {
        return crub;
    }

    public void setCrub(boolean crub) {
        this.crub = crub;
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", location=" + location +
                ", stair=" + stair +
                ", crub=" + crub +
                " }";
    }
}
