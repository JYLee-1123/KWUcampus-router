package com.campus.kwangwoon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // Spring Boot의 자동 설정을 활성화
public class DemoApplication {

    public static void main(String[] args) {
        // Spring Boot 애플리케이션(내장 웹 서버)을 실행시킵니다.
        SpringApplication.run(DemoApplication.class, args);
    }
}