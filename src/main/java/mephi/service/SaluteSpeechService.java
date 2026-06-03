package mephi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.repository.AudioFileRepository;
import mephi.repository.ExternalCallLogRepository;
import mephi.repository.SemanticBlockRepository;
import mephi.repository.TranscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class SaluteSpeechService {
    private static final String DEFAULT_AUDIO_FORMAT = "MP3";
    private static final String DEFAULT_AUDIO_LANGUAGE = "ru-RU";
    private static final long SBER_REQUEST_FILE_ID_LIFETIME_HOURS = 72;
    private static final long SBER_RESPONSE_FILE_ID_LIFETIME_HOURS = 72;

    @Value("${salutespeech.client-id}")
    private String clientId;

    @Value("${salutespeech.client-secret}")
    private String clientSecret;

    @Value("${salutespeech.auth-url}")
    private String authUrl;

    @Value("${salutespeech.api-url}")
    private String apiUrl;

    @Value("${salutespeech.scope}")
    private String scope;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AudioFileRepository audioFileRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final ExternalCallLogRepository externalCallLogRepository;
    private final SemanticBlockRepository semanticBlockRepository;

    private String accessToken;
    private long tokenExpiry;

    public SaluteSpeechService(AudioFileRepository audioFileRepository,
                               TranscriptionRepository transcriptionRepository,
                               ExternalCallLogRepository externalCallLogRepository,
                               SemanticBlockRepository semanticBlockRepository) {
        this.audioFileRepository = audioFileRepository;
        this.transcriptionRepository = transcriptionRepository;
        this.externalCallLogRepository = externalCallLogRepository;
        this.semanticBlockRepository = semanticBlockRepository;
    }

    private synchronized String getAccessToken() throws Exception {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.set("RqUID", UUID.randomUUID().toString());

        String body = "scope=" + scope;
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(authUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new Exception("Ошибка получения токена: " + response.getStatusCode());
        }

        JsonNode json = objectMapper.readTree(response.getBody());
        accessToken = json.get("access_token").asText();
        tokenExpiry = json.get("expires_at").asLong() - 60_000;

        return accessToken;
    }
}
