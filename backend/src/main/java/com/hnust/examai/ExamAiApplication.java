package com.hnust.examai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 公考 AI 智能刷题笔记系统 - 启动类
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ExamAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamAiApplication.class, args);
    }
}
