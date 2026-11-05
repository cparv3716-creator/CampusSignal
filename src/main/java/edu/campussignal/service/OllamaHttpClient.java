package edu.campussignal.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaHttpClient implements LocalOllamaClient {
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final URI endpoint;
    private final String model;
    private final Duration timeout;

    public OllamaHttpClient(
            ObjectMapper mapper,
            @Value("${campussignal.local-ai.ollama-endpoint:http://127.0.0.1:11434}") String endpoint,
            @Value("${campussignal.local-ai.model:llama3.2:3b-instruct}") String model,
            @Value("${campussignal.local-ai.timeout-ms:10000}") long timeoutMs) {
        this.mapper = mapper;
        this.endpoint = loopbackEndpoint(endpoint);
        this.model = model;
        this.timeout = Duration.ofMillis(Math.max(1000, Math.min(timeoutMs, 30000)));
        this.http = HttpClient.newBuilder().connectTimeout(this.timeout).build();
    }

    @Override
    public String summarize(String prompt) throws IOException, InterruptedException {
        var payload = mapper.createObjectNode()
                .put("model", model)
                .put("prompt", prompt)
                .put("stream", false)
                .put("format", "json");
        HttpRequest request = HttpRequest.newBuilder(endpoint.resolve("/api/generate"))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Local Ollama request failed with status " + response.statusCode());
        }
        return mapper.readTree(response.body()).path("response").asText("");
    }

    private static URI loopbackEndpoint(String value) {
        URI uri = URI.create(value);
        boolean loopback;
        try {
            loopback = java.net.InetAddress.getByName(uri.getHost()).isLoopbackAddress();
        } catch (java.net.UnknownHostException exception) {
            throw new IllegalArgumentException("Local Ollama endpoint host is invalid", exception);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getQuery() != null
                || uri.getFragment() != null
                || !loopback) {
            throw new IllegalArgumentException("Local Ollama endpoint must be plain HTTP loopback");
        }
        return uri;
    }
}
