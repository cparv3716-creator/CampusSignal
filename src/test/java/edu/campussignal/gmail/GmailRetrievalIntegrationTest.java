package edu.campussignal.gmail;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import edu.campussignal.repository.EmailRepository;
import edu.campussignal.service.EmailRetrievalService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GmailRetrievalIntegrationTest {
    @Autowired EmailRepository repository;
    @Autowired EmailRetrievalService retrieval;

    @Test
    void googleHttpResponsesFlowThroughMappingAndPersistenceIdempotently() throws Exception {
        repository.deleteAll();
        String list = "{\"messages\":[{\"id\":\"integration-mail\"}]}";
        String message = """
                {"id":"integration-mail","internalDate":"1788861600000","payload":{"mimeType":"text/plain",
                "headers":[{"name":"Subject","value":"Integration update"},{"name":"From","value":"notices@example.edu"}],
                "body":{"data":"SGVsbG8"}}}
                """;
        var transport = new QueuedHttpTransport().enqueue(list).enqueue(message).enqueue(list).enqueue(message);
        var source = new GmailApiClient(new Gmail.Builder(transport, GsonFactory.getDefaultInstance(),
                request -> request.setNumberOfRetries(0)).setApplicationName("CampusSignal tests").build());
        assertThat(retrieval.retrieve(source, 10).created()).isEqualTo(1);
        assertThat(retrieval.retrieve(source, 10).alreadyExisted()).isEqualTo(1);
        var stored = repository.findByGmailMessageId("integration-mail").orElseThrow();
        assertThat(stored.getSubject()).isEqualTo("Integration update");
        assertThat(stored.getSender()).isEqualTo("notices@example.edu");
        assertThat(stored.getBody()).isEqualTo("Hello");
        assertThat(repository.count()).isEqualTo(1);
    }
}
