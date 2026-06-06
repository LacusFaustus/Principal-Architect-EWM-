package ru.practicum.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyzerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyzerServiceApplication.class, args);
        System.out.println("RECOMMENDATION-ANALYZER RUNNING");
    }
}