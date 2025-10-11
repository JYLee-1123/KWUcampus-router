package com.campus.kwangwoon.model;

import java.util.List;

/*
 * 두 노드 간의 연결(길, 도로, 통로)을 표현하는 클래스
 */
public class Edge {
    private String from; // 시작 노드 ID
    private String to; // 도착 노드 ID
    private double weight; // 가중치 (거리, 비용 등)
    private boolean stair; // 진입 경로 내 계단 유무
    private boolean crub; // 진입 경로 내 도보 턱 유무
    private List<Point> geometry; // 매끄러운 경로 표시를 위한 중간 좌표 리스트

    // 기본 생성자
    public Edge() {
    }

    // 전체 필드 초기화 생성자
    public Edge(String from, String to, double weight, boolean stair, boolean crub, List<Point> geometry) {
        this.from = from;
        this.to = to;
        this.weight = weight;
        this.stair = stair;
        this.crub = crub;
        this.geometry = geometry;
    }

    // Getter & Setter
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
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

    public List<Point> getGeometry() {
        return geometry;
    }

    public void setGeometry(List<Point> geometry) {
        this.geometry = geometry;
    }

    @Override
    public String toString() {
        return "Edge{ " +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", weight=" + weight +
                ", stair=" + stair +
                ", crub=" + crub +
                " }";
    }
}
