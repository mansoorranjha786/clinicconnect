package com.postwork.clinicconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class ClinicconnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicconnectApplication.class, args);

    }

}
