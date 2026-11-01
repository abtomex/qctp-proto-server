package ru.abtomex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@PropertySource("classpath:config/version.properties")
public class QctpProtoApplication {

    public static void main(String[] args) {
        var springApplication = new SpringApplication(QctpProtoApplication.class);
        Environment environment = springApplication.run(args).getEnvironment();

        String protocol = "http";
        String endpoint = environment.getProperty("app.qctp.serviceName");
        log.info("\n----------------------------------------------------------\n\t" +
                        "Application '{}' is running! Access URLs:\n\t" +
                        "Local: \t\t{}://localhost:{}/{}\n\t" +
                        "Version: \t{}\n\t" +
                        "Profile(s): \t{}\n----------------------------------------------------------",
                QctpProtoApplication.class.getSimpleName(),
                protocol,
                environment.getProperty("server.port"),
                endpoint,
                environment.getProperty("app.qctp.version"),
                environment.getActiveProfiles());

    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

}
