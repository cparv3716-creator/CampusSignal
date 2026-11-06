package edu.campussignal.controller;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.campussignal.config.GmailProperties;
import edu.campussignal.dto.RetrievalSummary;
import edu.campussignal.gmail.GmailApiClient;
import edu.campussignal.gmail.GmailAuthorizationService;
import edu.campussignal.security.AuthenticatedUserService;
import edu.campussignal.service.EmailRetrievalService;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = EmailSyncController.class,
        properties = {
            "campussignal.gmail-sync.enabled=true",
            "campussignal.auth.required=false",
            "campussignal.gmail-sync.token=synthetic-test-token"
        })
class EmailSyncControllerTest {
    @Autowired MockMvc mvc;

    @MockitoBean GmailAuthorizationService authorization;
    @MockitoBean EmailRetrievalService retrieval;
    @MockitoBean GmailProperties settings;
    @MockitoBean AuthenticatedUserService authenticatedUsers;

    private final GmailApiClient client = org.mockito.Mockito.mock(GmailApiClient.class);

    @BeforeEach
    void configureMaximum() {
        when(settings.maxResults()).thenReturn(10);
        when(authenticatedUsers.required()).thenReturn(false);
    }

    @Test
    void rejectsNonLoopbackRequestsBeforeAccessingGmail() throws Exception {
        mvc.perform(post("/api/v1/emails/sync")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.25");
                            return request;
                        })
                        .header("X-CampusSignal-Sync-Token", "synthetic-test-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authorization, retrieval);
    }

    @Test
    void rejectsMissingOrIncorrectTokenBeforeAccessingGmail() throws Exception {
        mvc.perform(post("/api/v1/emails/sync"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/emails/sync")
                        .header("X-CampusSignal-Sync-Token", "incorrect"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(authorization, retrieval);
    }

    @Test
    void returnsCountsOnlyForAuthorizedLoopbackRequest() throws Exception {
        when(authorization.client()).thenReturn(client);
        when(retrieval.retrieve(client, 10)).thenReturn(new RetrievalSummary(4, 2, 1, 1));

        mvc.perform(post("/api/v1/emails/sync")
                        .header("X-CampusSignal-Sync-Token", "synthetic-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fetched").value(4))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.alreadyExisted").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("body"))));
    }

    @Test
    void providerFailureDoesNotExposePrivateDetail() throws Exception {
        when(authorization.client()).thenThrow(new IOException("synthetic-private-mail-detail"));

        mvc.perform(post("/api/v1/emails/sync")
                        .header("X-CampusSignal-Sync-Token", "synthetic-test-token"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("synthetic-private-mail-detail"))));
    }
}
