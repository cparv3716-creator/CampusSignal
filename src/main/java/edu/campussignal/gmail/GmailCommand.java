package edu.campussignal.gmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import edu.campussignal.config.GmailProperties;
import edu.campussignal.service.EmailRetrievalService;

@Component
@ConditionalOnProperty(prefix = "gmail", name = "command")
public class GmailCommand implements ApplicationRunner, ExitCodeGenerator {
    private final String command;
    private final GmailProperties settings;
    private final GmailAuthorizationService authorization;
    private final EmailRetrievalService retrieval;
    private int exitCode;

    public GmailCommand(@Value("${gmail.command}") String command, GmailProperties settings,
                        GmailAuthorizationService authorization, EmailRetrievalService retrieval) {
        this.command = command;
        this.settings = settings;
        this.authorization = authorization;
        this.retrieval = retrieval;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            switch (command) {
                case "authorize" -> {
                    authorization.authorize();
                    System.out.println("Gmail authorization stored.");
                }
                case "retrieve" -> {
                    var result = retrieval.retrieve(authorization.client(), settings.maxResults());
                    System.out.printf("fetched=%d created=%d alreadyExisted=%d failed=%d%n",
                            result.fetched(), result.created(), result.alreadyExisted(), result.failed());
                    exitCode = result.failed() == 0 ? 0 : 1;
                }
                default -> {
                    System.err.println("Use --gmail.command=authorize or --gmail.command=retrieve.");
                    exitCode = 1;
                }
            }
        } catch (Exception exception) {
            // Never print provider exceptions, credential files, or private message content.
            System.err.println("Gmail command failed. Check OAuth configuration, authorization, and connectivity.");
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() { return exitCode; }
}
