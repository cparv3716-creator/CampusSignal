package edu.campussignal;

import java.util.*;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import edu.campussignal.repository.UserProfileRepository;
import edu.campussignal.service.ProfileService;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserProfileRepository repository;
    @Autowired ProfileService profiles;

    @BeforeEach
    void clearProfiles() { repository.deleteAll(); }

    private Map<String, Object> valid() {
        var request = new HashMap<String, Object>();
        request.put("email", " Student@Example.edu ");
        request.put("department", " Computer Science ");
        request.put("programme", " BTech ");
        request.put("year", 3);
        request.put("interests", List.of(" Internships ", "RESEARCH", "internships", "New Topic"));
        return request;
    }

    @Test
    void createsPersistsAndRetrievesNormalizedProfile() throws Exception {
        var response = mvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(valid())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("student@example.edu"))
                .andExpect(jsonPath("$.department").value("Computer Science"))
                .andExpect(jsonPath("$.programme").value("BTech"))
                .andExpect(jsonPath("$.year").value(3))
                .andExpect(jsonPath("$.semester").isEmpty())
                .andExpect(jsonPath("$.interests", org.hamcrest.Matchers.contains("internships", "research", "new topic")))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn();
        long id = json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        assertThat(response.getResponse().getHeader("Location")).isEqualTo("/api/v1/profiles/" + id);
        mvc.perform(get("/api/v1/profiles/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.interests[1]").value("research"));
        mvc.perform(get("/api/v1/profiles/by-email/{email}", "STUDENT@example.edu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(repository.count()).isEqualTo(1);
        assertThat(profiles.get(id).interests()).containsExactly("internships", "research", "new topic");
    }

    @Test
    void acceptsPositiveSemester() throws Exception {
        var request = valid();
        request.put("semester", 5);
        mvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.semester").value(5));
    }

    static Stream<Arguments> invalidValues() {
        return Stream.of(
                Arguments.of("department", ""), Arguments.of("department", " \t"),
                Arguments.of("programme", ""), Arguments.of("programme", " \n"),
                Arguments.of("year", 0), Arguments.of("year", -1), Arguments.of("year", null),
                Arguments.of("year", "three"), Arguments.of("year", 1.5), Arguments.of("interests", List.of(true)), Arguments.of("semester", 0), Arguments.of("semester", -1),
                Arguments.of("interests", List.of()), Arguments.of("interests", List.of(" ")),
                Arguments.of("interests", List.of("research", "")),
                Arguments.of("interests", Arrays.asList((String) null)),
                Arguments.of("interests", List.of(42)), Arguments.of("interests", null),
                Arguments.of("email", ""), Arguments.of("email", "not-an-email"),
                Arguments.of("unexpected", "extra"));
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void rejectsInvalidRequests(String field, Object value) throws Exception {
        var request = valid();
        request.put(field, value);
        mvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        assertThat(repository.count()).isZero();
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        String payload = json.writeValueAsString(valid());
        mvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void missingProfilesReturn404() throws Exception {
        mvc.perform(get("/api/v1/profiles/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/profiles/by-email/missing@example.edu")).andExpect(status().isNotFound());
    }

    @Test
    void categoryScoreEndpointReturnsComponents() throws Exception {
        var response = mvc.perform(post("/api/v1/profiles").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(valid()))).andReturn();
        long id = json.readTree(response.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/v1/profiles/{id}/category-score", id).param("category", " Internships "))
                .andExpect(status().isOk()).andExpect(jsonPath("$.category").value("internships"))
                .andExpect(jsonPath("$.score").value(0.6875))
                .andExpect(jsonPath("$.components.explicitPreference").value(1.0))
                .andExpect(jsonPath("$.components.behavioralPreference").value(0.5))
                .andExpect(jsonPath("$.components.relatedInterestSimilarity").value(0.0));
        mvc.perform(get("/api/v1/profiles/{id}/category-score", id).param("category", " "))
                .andExpect(status().isBadRequest());
    }
}
