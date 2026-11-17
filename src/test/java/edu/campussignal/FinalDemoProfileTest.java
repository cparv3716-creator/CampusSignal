package edu.campussignal;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class FinalDemoProfileTest {
    @Autowired MockMvc mockMvc;
    @Autowired Environment environment;

    @Test
    void demoProfileIsSelfContainedAndListsOnlyFictionalEmails() throws Exception {
        assertThat(environment.getProperty("server.address")).isEqualTo("0.0.0.0");
        assertThat(environment.getProperty("server.port")).isEqualTo("80");
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:campussignal-demo");
        assertThat(environment.getProperty("campussignal.auth.required")).isEqualTo("true");
        assertThat(environment.getProperty("campussignal.gmail-sync.enabled")).isEqualTo("false");
        assertThat(environment.getProperty("campussignal.local-ai.enabled")).isEqualTo("false");

        mockMvc.perform(get("/api/v1/me/emails"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/emails"))
                .andExpect(status().isNotFound());

        var login = mockMvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("demo.student@demo.example.com"))
                .andExpect(jsonPath("$.profileComplete").value(true))
                .andExpect(jsonPath("$.gmailConnected").value(false))
                .andReturn();
        Cookie session = login.getResponse().getCookie("CAMPUSSIGNAL_SESSION");
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/v1/me/emails").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].sender", everyItem(endsWith("@demo.example.com"))));
    }
}
