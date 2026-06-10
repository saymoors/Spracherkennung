package mephi.salutespeech.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.enums.AudioFormat;
import mephi.salutespeech.model.SberDownloadResponse;
import mephi.salutespeech.model.SberUploadFileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.util.UUID;

@Component
public class SberFileClient {
    @Value("${salutespeech.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SberUploadFileResponse uploadFile(String systemPath, AudioFormat audioFormat, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(audioFormat.getContentType()));
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());

        FileSystemResource resource = new FileSystemResource(Path.of(systemPath).toFile());
        HttpEntity<FileSystemResource> entity = new HttpEntity<>(resource, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl + "/data:upload", HttpMethod.POST, entity, String.class);
        JsonNode json = objectMapper.readTree(response.getBody());
        UUID requestFileId = UUID.fromString(json.get("result").get("request_file_id").asText());

        return new SberUploadFileResponse(requestFileId, response.getStatusCode().value());
    }

    public SberDownloadResponse downloadResult(UUID responseFileId, String token) {
        String url = apiUrl + "/data:download?response_file_id=" + responseFileId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
        return new SberDownloadResponse(response.getBody(), response.getStatusCode().value());
    }
}
