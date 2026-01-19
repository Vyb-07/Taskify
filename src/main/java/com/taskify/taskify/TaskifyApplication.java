package com.taskify.taskify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TaskifyApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskifyApplication.class, args);
	}

}
