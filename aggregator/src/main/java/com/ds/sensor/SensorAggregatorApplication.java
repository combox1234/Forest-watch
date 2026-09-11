package com.ds.sensor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SensorAggregatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SensorAggregatorApplication.class, args);
    }
}
