package com.plover.plover_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PloverBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(PloverBeApplication.class, args);
	}

}
