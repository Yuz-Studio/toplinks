package com.yuz.toplinks;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yuz.toplinks.mapper")
public class ToplinksApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToplinksApplication.class, args);
	}

}
