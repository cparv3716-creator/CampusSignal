package edu.campussignal.demo;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campussignal.demo.fixture", havingValue = "phase4")
public class SyntheticDemoFixtureCommand implements ApplicationRunner, ExitCodeGenerator {
    private static final String CONFIRMATION = "synthetic-only";
    private final SyntheticDemoFixtureService fixture;
    private final Environment environment;
    private int exitCode;

    public SyntheticDemoFixtureCommand(SyntheticDemoFixtureService fixture, Environment environment) {
        this.fixture = fixture;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!isSafeInvocation()) {
            System.err.println("Synthetic demo fixture refused: use the demo profile, explicit confirmation, "
                    + "and an isolated campussignal-demo H2 database.");
            exitCode = 2;
            return;
        }
        var result = fixture.seed();
        System.out.printf("syntheticFixture=phase4 totalEmails=%d createdEmails=%d "
                        + "createdActions=%d profileId=%d%n",
                result.totalEmails(), result.createdEmails(), result.createdActions(), result.profileId());
    }

    private boolean isSafeInvocation() {
        boolean demoProfile = Arrays.asList(environment.getActiveProfiles()).contains("demo");
        String confirmation = environment.getProperty("campussignal.demo.confirm", "");
        String databaseUrl = environment.getProperty("spring.datasource.url", "");
        return demoProfile
                && CONFIRMATION.equals(confirmation)
                && databaseUrl.toLowerCase().contains("campussignal-demo")
                && !databaseUrl.toLowerCase().contains("campussignal;")
                && !databaseUrl.toLowerCase().contains("campussignal?") ;
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
