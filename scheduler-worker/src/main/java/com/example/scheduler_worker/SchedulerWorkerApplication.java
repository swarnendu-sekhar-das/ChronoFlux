package com.example.scheduler_worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "50s")
public class SchedulerWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchedulerWorkerApplication.class, args);
	}

}
