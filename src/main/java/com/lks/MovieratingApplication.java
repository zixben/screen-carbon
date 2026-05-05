package com.lks;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@MapperScan(basePackages = {"com.lks.mapper"})
@SpringBootApplication
//(exclude={DataSourceAutoConfiguration.class})
//@MapperScan("com.lks.mapper")
//@ComponentScan(basePackages = {"com.lks.controller," + "com.lks.service"})
//(exclude = {DataSourceAutoConfiguration.class })
public class MovieratingApplication {


    public static void main(String[] args) {
        SpringApplication.run(MovieratingApplication.class, args);
    }

}
