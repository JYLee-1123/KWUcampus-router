package com.campus.kwangwoon.repository;

import com.campus.kwangwoon.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // 아무것도 안 써도 알아서 저장/조회 기능이 생김
}