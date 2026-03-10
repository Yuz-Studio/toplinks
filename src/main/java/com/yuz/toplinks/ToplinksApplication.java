package com.yuz.toplinks;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@MapperScan("com.yuz.toplinks.mapper")
@ServletComponentScan // Added to scan for Filters/Servlets, just in case component scan misses it
public class ToplinksApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToplinksApplication.class, args);
	}

}