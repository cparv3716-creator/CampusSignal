package edu.campussignal.gmail;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import com.google.api.client.testing.http.MockHttpTransport;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;

/** In-memory HTTP responses only; no network connections or real credentials. */
class QueuedHttpTransport extends MockHttpTransport {
    record Response(int status, String json) {}
    final Queue<Response> responses = new ArrayDeque<>();
    final List<String> urls = new ArrayList<>();
    final List<MockLowLevelHttpRequest> requests = new ArrayList<>();

    QueuedHttpTransport enqueue(String json) { return enqueue(200, json); }
    QueuedHttpTransport enqueue(int status, String json) {
        responses.add(new Response(status, json));
        return this;
    }

    @Override
    public MockLowLevelHttpRequest buildRequest(String method, String url) {
        urls.add(url);
        var request = new MockLowLevelHttpRequest(url) {
            @Override
            public MockLowLevelHttpResponse execute() throws IOException {
                if (responses.isEmpty()) { throw new IOException("No simulated response queued"); }
                var response = responses.remove();
                return new MockLowLevelHttpResponse().setStatusCode(response.status())
                        .setContentType("application/json").setContent(response.json());
            }
        };
        requests.add(request);
        return request;
    }
}
