package edu.campussignal.config;

import java.nio.file.Path;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("gmail")
public record GmailProperties(
        @DefaultValue("credentials.json") Path credentialsFile,
        @DefaultValue(".gmail-tokens") Path tokenDirectory,
        @Min(1) @Max(65535) @DefaultValue("8888") int callbackPort,
        @Min(1) @Max(500) @DefaultValue("10") int maxResults) {
}
