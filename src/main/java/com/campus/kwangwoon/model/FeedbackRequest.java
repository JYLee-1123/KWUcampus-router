package com.campus.kwangwoon.model;

public class FeedbackRequest {
    private String category; // 신고 유형
    private String content; // 신고 내용
    private double lat; // 위도
    private double lng; // 경도

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}