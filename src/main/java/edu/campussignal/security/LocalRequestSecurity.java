package edu.campussignal.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalRequestSecurity {
    private final Set<String> allowedOrigins;
    private final String configuredServerAddress;

    public LocalRequestSecurity(
            @Value("${campussignal.allowed-origin:http://127.0.0.1:5174}") String allowedOrigin,
            @Value("${server.address:127.0.0.1}") String configuredServerAddress) {
        this.allowedOrigins = new HashSet<>(Set.of("http://127.0.0.1:5173",
                "http://localhost:5173", "http://localhost:5174"));
        this.allowedOrigins.add(allowedOrigin);
        this.configuredServerAddress = configuredServerAddress;
    }

    public boolean isLoopback(HttpServletRequest request) {
        try {
            return java.net.InetAddress.getByName(configuredServerAddress).isLoopbackAddress()
                    && java.net.InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()
                    && java.net.InetAddress.getByName(request.getLocalAddr()).isLoopbackAddress()
                    && isLoopbackHost(request.getServerName());
        } catch (java.net.UnknownHostException exception) {
            return false;
        }
    }

    public boolean hasAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return origin == null || allowedOrigins.contains(origin);
    }

    private boolean isLoopbackHost(String host) {
        try {
            return "localhost".equalsIgnoreCase(host)
                    || java.net.InetAddress.getByName(host).isLoopbackAddress();
        } catch (java.net.UnknownHostException exception) {
            return false;
        }
    }

    public boolean isAllowedOrigin(String origin) {
        try {
            URI parsed = URI.create(origin);
            return allowedOrigins.contains(origin)
                    && ("http".equalsIgnoreCase(parsed.getScheme())
                    || "https".equalsIgnoreCase(parsed.getScheme()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
