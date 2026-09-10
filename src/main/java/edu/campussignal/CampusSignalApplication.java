package edu.campussignal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CampusSignalApplication {
    public static void main(String[] args) {
        var context = SpringApplication.run(CampusSignalApplication.class, args);
        if (context.getEnvironment().containsProperty("gmail.command")) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
