package com.sirinsirin;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.sirinsirin"})
@MapperScan(basePackages = {"com.sirinsirin.mappers"})
@EnableFeignClients
public class SirinsirinCloudAdminRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(SirinsirinCloudAdminRunApplication.class, args);
    }
}
