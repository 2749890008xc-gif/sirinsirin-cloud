package com.sirinsirin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.sirinsirin", exclude = DataSourceAutoConfiguration.class)
@EnableFeignClients
public class SirinsirinCloudResourceRunApplication {
    public static void main(String[] args) {
        SpringApplication.run(SirinsirinCloudResourceRunApplication.class, args);
    }
}
