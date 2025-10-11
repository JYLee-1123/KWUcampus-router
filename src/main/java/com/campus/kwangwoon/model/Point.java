package com.campus.kwangwoon.model;

public class Point {
    private double lat; // 위도
    private double lng; // 경도

    public Point() {
    }

    public Point(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
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

    @Override
    public String toString() {
        return "Point{lat=" + lat + ", lng=" + lng + "}";
    }
}
