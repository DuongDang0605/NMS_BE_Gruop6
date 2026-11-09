package com.mvc.nms_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication()
@EnableScheduling
public class NmsBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NmsBeApplication.class, args);
    }

}
