package com.sirinsirin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {"com.sirinsirin"})
@MapperScan(basePackages = {"com.sirinsirin.mappers"})
@EnableScheduling
public class SirinsirinCloudWebRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(SirinsirinCloudWebRunApplication.class, args);
    }
}
