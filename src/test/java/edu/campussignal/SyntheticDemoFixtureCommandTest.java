package edu.campussignal;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import edu.campussignal.demo.SyntheticDemoFixtureCommand;
import edu.campussignal.demo.SyntheticDemoFixtureService;

class SyntheticDemoFixtureCommandTest {
    @Test
    void refusesToSeedWithoutEverySafetyGuard() {
        var fixture = org.mockito.Mockito.mock(SyntheticDemoFixtureService.class);
        var environment = new MockEnvironment()
                .withProperty("campussignal.demo.fixture", "phase4")
                .withProperty("campussignal.demo.confirm", "synthetic-only")
                .withProperty("spring.datasource.url", "jdbc:h2:file:./data/campussignal");
        var command = new SyntheticDemoFixtureCommand(fixture, environment);

        command.run(new DefaultApplicationArguments());

        verifyNoInteractions(fixture);
    }

    @Test
    void seedsOnlyWithDemoProfileConfirmationAndIsolatedDatabase() {
        var fixture = org.mockito.Mockito.mock(SyntheticDemoFixtureService.class);
        when(fixture.seed()).thenReturn(new SyntheticDemoFixtureService.FixtureResult(6, 6, 5, 1));
        var environment = new MockEnvironment()
                .withProperty("campussignal.demo.fixture", "phase4")
                .withProperty("campussignal.demo.confirm", "synthetic-only")
                .withProperty("spring.datasource.url", "jdbc:h2:file:./data/campussignal-demo")
                .withProperty("spring.profiles.active", "demo");
        var command = new SyntheticDemoFixtureCommand(fixture, environment);

        command.run(new DefaultApplicationArguments());

        verify(fixture).seed();
    }
}
