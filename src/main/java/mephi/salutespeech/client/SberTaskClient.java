package mephi.salutespeech.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mephi.audio.AudioFormat;
import mephi.salutespeech.model.SberCreateTaskResponse;
import mephi.salutespeech.model.SberPollTaskResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
public class SberTaskClient {
    @Value("${salutespeech.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SberCreateTaskResponse createTask(UUID requestFileId, String audioLanguage, String audioFormat, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());

        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode options = body.putObject("options");
        options.put("model", "general");
        options.put("language", audioLanguage);
        options.put("audio_encoding", AudioFormat.fromExtension(audioFormat).getSberAudioEncoding());
        body.put("request_file_id", requestFileId.toString());

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl + "/speech:async_recognize", HttpMethod.POST, entity, String.class);

        JsonNode json = objectMapper.readTree(response.getBody());
        String taskId = json.get("result").get("id").asText();

        return new SberCreateTaskResponse(taskId, response.getStatusCode().value());
    }

    public SberPollTaskResponse pollTask(String taskId, String token) throws Exception {
        String url = apiUrl + "/task:get?id=" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        JsonNode json = objectMapper.readTree(response.getBody());
        JsonNode result = json.get("result");
        String status = result.get("status").asText();
        UUID responseFileId = null;

        if ("DONE".equals(status)) {
            responseFileId = UUID.fromString(result.get("response_file_id").asText());
        }

        return new SberPollTaskResponse(status, responseFileId, response.getStatusCode().value());
    }
}
