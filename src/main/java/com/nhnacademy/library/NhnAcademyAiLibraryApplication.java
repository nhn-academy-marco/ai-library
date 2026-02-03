package com.nhnacademy.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NhnAcademyAiLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(NhnAcademyAiLibraryApplication.class, args);
    }

}
