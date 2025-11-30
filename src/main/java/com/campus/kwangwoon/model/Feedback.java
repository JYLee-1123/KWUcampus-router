package com.campus.kwangwoon.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity // DB 테이블이 될 클래스
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 번호

    private String category;
    private String content;
    private double lat;
    private double lng;
    private LocalDateTime createdAt; // 신고 시간

    public Feedback() {
    } // 기본 생성자 필수

    public Feedback(String category, String content, double lat, double lng) {
        this.category = category;
        this.content = content;
        this.lat = lat;
        this.lng = lng;
        this.createdAt = LocalDateTime.now();
    }

    // Getter
    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}