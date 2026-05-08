package com.lks;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan(basePackages = {"com.lks.mapper"})
@SpringBootApplication
public class MovieratingApplication {


    public static void main(String[] args) {
        SpringApplication.run(MovieratingApplication.class, args);
    }

}
