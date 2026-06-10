package mephi.salutespeech.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Component
public class SberAuthClient {
    @Value("${salutespeech.client-id}")
    private String clientId;

    @Value("${salutespeech.client-secret}")
    private String clientSecret;

    @Value("${salutespeech.auth-url}")
    private String authUrl;

    @Value("${salutespeech.scope}")
    private String scope;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String accessToken;
    private long tokenExpiry;

    public synchronized String getAccessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + getApiKey());
        headers.set("RqUID", UUID.randomUUID().toString());

        HttpEntity<String> entity = new HttpEntity<>("scope=" + scope, headers);
        ResponseEntity<String> response = restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new Exception("Ошибка получения токена: " + response.getStatusCode());
        }

        JsonNode json = objectMapper.readTree(response.getBody());
        accessToken = json.get("access_token").asText();
        tokenExpiry = json.get("expires_at").asLong() - 60_000;

        return accessToken;
    }

    private String getApiKey() {
        String apiKey = clientId + ":" + clientSecret;
        return Base64.getEncoder().encodeToString(apiKey.getBytes(StandardCharsets.UTF_8));
    }
}
