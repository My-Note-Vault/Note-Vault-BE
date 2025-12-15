package com.example;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

import static org.springframework.boot.SpringApplication.run;


@EnableFeignClients(
        basePackages = "com.example.platformservice"
)
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        run(Application.class, args);
    }

}
