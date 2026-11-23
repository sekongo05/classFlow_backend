package com.mosek.classFlow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ClassFlowApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClassFlowApplication.class, args);
	}

}
