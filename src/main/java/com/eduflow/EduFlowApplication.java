package com.eduflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EduFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduFlowApplication.class, args);
    }
}
