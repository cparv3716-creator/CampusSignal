package edu.campussignal.config;

import java.nio.file.Path;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Validated
@ConfigurationProperties("gmail")
public record GmailProperties(
        @DefaultValue("credentials.json") Path credentialsFile,
        @DefaultValue(".gmail-tokens") Path tokenDirectory,
        @Min(1) @Max(65535) @DefaultValue("8888") int callbackPort,
        @Min(1) @Max(500) @DefaultValue("10") int maxResults,
        @DefaultValue("me") String userId,
        @DefaultValue("") String googleCloudProject,
        @DefaultValue("") String pubsubTopic,
        @DefaultValue("") String pubsubSubscription,
        @Min(1) @Max(500) @DefaultValue("10") int recoveryMaxResults) {
    @ConstructorBinding
    public GmailProperties {
    }

    public GmailProperties(Path credentialsFile, Path tokenDirectory, int callbackPort, int maxResults) {
        this(credentialsFile, tokenDirectory, callbackPort, maxResults,
                "me", "", "", "", 10);
    }

    public String topicPath() {
        return qualifiedPubsubName(pubsubTopic, "topics", "GMAIL_PUBSUB_TOPIC");
    }

    public String subscriptionPath() {
        return qualifiedPubsubName(pubsubSubscription, "subscriptions", "GMAIL_PUBSUB_SUBSCRIPTION");
    }

    private String qualifiedPubsubName(String value, String resource, String settingName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(settingName + " is not configured");
        }
        String trimmed = value.strip();
        if (trimmed.startsWith("projects/")) {
            return trimmed;
        }
        if (googleCloudProject == null || googleCloudProject.isBlank()) {
            throw new IllegalStateException("GOOGLE_CLOUD_PROJECT is required when "
                    + settingName + " is not fully qualified");
        }
        return "projects/" + googleCloudProject.strip() + "/" + resource + "/" + trimmed;
    }
}
