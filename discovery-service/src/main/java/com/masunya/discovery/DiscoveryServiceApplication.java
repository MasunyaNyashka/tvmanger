package com.masunya.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        // Точка входа сервера service discovery (Eureka).
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
