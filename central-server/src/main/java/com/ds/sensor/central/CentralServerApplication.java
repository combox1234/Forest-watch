package com.ds.sensor.central;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;

@SpringBootApplication
@EnableScheduling
public class CentralServerApplication {

    public static void main(String[] args) {
        // Pre-create local data directory for file-based SQLite database
        new File("data").mkdirs();
        SpringApplication.run(CentralServerApplication.class, args);
    }
}
