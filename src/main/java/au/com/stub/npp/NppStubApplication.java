package au.com.stub.npp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NppStubApplication {

    public static void main(String[] args) {
        SpringApplication.run(NppStubApplication.class, args);
    }
}
