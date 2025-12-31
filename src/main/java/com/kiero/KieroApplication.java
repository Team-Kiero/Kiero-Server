package com.kiero;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class KieroApplication {
	public static void main(String[] args) {
		SpringApplication.run(KieroApplication.class, args);
	}
}
