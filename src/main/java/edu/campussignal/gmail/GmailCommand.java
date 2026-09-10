package edu.campussignal.gmail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import edu.campussignal.config.GmailProperties;
import edu.campussignal.service.EmailRetrievalService;
import edu.campussignal.service.GmailListenerService;
import edu.campussignal.service.GmailWatchService;

@Component
@ConditionalOnProperty(prefix = "gmail", name = "command")
public class GmailCommand implements ApplicationRunner, ExitCodeGenerator {
    private final String command;
    private final GmailProperties settings;
    private final GmailAuthorizationService authorization;
    private final EmailRetrievalService retrieval;
    private final GmailWatchService watch;
    private final GmailListenerService listener;
    private int exitCode;

    public GmailCommand(@Value("${gmail.command}") String command, GmailProperties settings,
                        GmailAuthorizationService authorization, EmailRetrievalService retrieval,
                        GmailWatchService watch, GmailListenerService listener) {
        this.command = command;
        this.settings = settings;
        this.authorization = authorization;
        this.retrieval = retrieval;
        this.watch = watch;
        this.listener = listener;
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
                case "watch" -> {
                    var result = watch.register(authorization.client(), settings.topicPath());
                    System.out.println("Watch registered");
                    System.out.println("History ID: " + result.historyId());
                    System.out.println("Expiration: " + result.expiresAt());
                }
                case "listen" -> {
                    String subscription = settings.subscriptionPath();
                    System.out.println("Listening on " + subscription);
                    listener.listen(authorization.client(), subscription, settings.recoveryMaxResults());
                }
                default -> {
                    System.err.println("Use --gmail.command=authorize, retrieve, watch, or listen.");
                    exitCode = 1;
                }
            }
        } catch (Exception exception) {
            // Never print provider exceptions, credential files, Pub/Sub payloads, or private email content.
            System.err.println("Gmail command failed. Check OAuth, Pub/Sub configuration, and connectivity.");
            exitCode = 1;
        }
    }

    @Override
    public int getExitCode() { return exitCode; }
}
