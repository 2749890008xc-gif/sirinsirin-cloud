package com.sirinsirin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sirinsirin"})
public class SirinsirinCloudGatewayRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(SirinsirinCloudGatewayRunApplication.class, args);
    }
}
