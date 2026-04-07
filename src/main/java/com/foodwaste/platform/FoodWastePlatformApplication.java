package com.foodwaste.platform;

import com.foodwaste.platform.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FoodWastePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodWastePlatformApplication.class, args);
    }
}
