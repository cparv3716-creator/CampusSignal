package edu.campussignal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import edu.campussignal.demo.SyntheticDemoDataInitializer;
import edu.campussignal.repository.EmailActionRepository;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.repository.UserProfileRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
            "spring.datasource.url=jdbc:h2:mem:production-demo;DB_CLOSE_DELAY=-1",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "campussignal.demo-data.enabled=true",
            "gmail.credentials-file=target/no-real-oauth-credentials.json",
            "gmail.token-directory=target/no-real-oauth-tokens"
        })
@ActiveProfiles("prod")
class ProductionDemoStartupTest {
    @Autowired EmailRepository emails;
    @Autowired EmailActionRepository actions;
    @Autowired UserProfileRepository profiles;
    @Autowired SyntheticDemoDataInitializer initializer;

    @Test
    void productionProfileCanStartWithSyntheticDataAndNoGmailCredentials() {
        assertThat(profiles.findByEmail(SyntheticDemoDataInitializer.DEMO_PROFILE_EMAIL)).isPresent();
        assertThat(emails.findByGmailMessageId("demo-career-internship")).isPresent();
        assertThat(emails.findByGmailMessageId("demo-campus-hackathon")).isPresent();
        assertThat(emails.count()).isEqualTo(2);
        assertThat(actions.count()).isEqualTo(2);

        initializer.run(new DefaultApplicationArguments(new String[0]));

        assertThat(profiles.count()).isEqualTo(1);
        assertThat(emails.count()).isEqualTo(2);
        assertThat(actions.count()).isEqualTo(2);
    }
}
