package mephi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import mephi.entity.AudioFile;
import mephi.entity.ExternalCallLog;
import mephi.entity.SemanticBlock;
import mephi.entity.Transcription;
import mephi.repository.AudioFileRepository;
import mephi.repository.ExternalCallLogRepository;
import mephi.repository.SemanticBlockRepository;
import mephi.repository.TranscriptionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    public void startRecognition(Integer transcriptionId) {
        try {
            Transcription transcription = transcriptionRepository.findById(transcriptionId)
                    .orElseThrow(() -> new Exception("Транскрипция не найдена"));
            AudioFile audioFile = audioFileRepository.findById(transcription.getAudioFileId())
                    .orElseThrow(() -> new Exception("Файл не найден"));

            String token = getAccessToken();
            saveLog(transcriptionId, "get_token", "POST", 200, "Токен получен");

            if (isSberResponseFileIdAlive(transcription)) {
                downloadResult(transcriptionId, transcription.getSberResponseFileId(), token);
                return;
            }

            if (isSberRequestFileIdAlive(audioFile)) {
                startTaskWithExistingSberFile(transcriptionId, token, audioFile);
                return;
            }

            startTaskWithNewSberFile(transcriptionId, token, audioFile);
        } catch (Exception exception) {
            saveLog(transcriptionId, "start_recognition", "POST", statusFromException(exception), messageFromException(exception));
            updateError(transcriptionId);
        }
    }

    private boolean isSberResponseFileIdAlive(Transcription transcription) {
        return transcription.getSberResponseFileId() != null
                && transcription.getSberResponseFileReceivedAt() != null
                && transcription.getSberResponseFileReceivedAt()
                .isAfter(LocalDateTime.now().minusHours(SBER_RESPONSE_FILE_ID_LIFETIME_HOURS));
    }

    private boolean isSberRequestFileIdAlive(AudioFile audioFile) {
        return audioFile.getSberRequestFileId() != null
                && audioFile.getUploadAt() != null
                && audioFile.getUploadAt().isAfter(LocalDateTime.now().minusHours(SBER_REQUEST_FILE_ID_LIFETIME_HOURS));
    }

    private void downloadResult(Integer transcriptionId, UUID sberResponseFileId, String token) {
        String url = apiUrl + "/data:download?response_file_id=" + sberResponseFileId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);

            JsonNode body = objectMapper.readTree(response.getBody());

            double durationSeconds = 0.0;
            int characterCount = 0;
            int sentenceCount = 0;

            List<SemanticBlock> semanticBlocks = new ArrayList<>();
            int order = 0;

            List<JsonNode> resultsBlocks = new ArrayList<>();

            for (JsonNode resultsBlock : body) {
                resultsBlocks.add(resultsBlock);
            }

            for (JsonNode resultsBlock : resultsBlocks) {
                JsonNode results = resultsBlock.get("results");
                if (results != null) {
                    for (JsonNode result : results) {
                        String textContent = getTextContent(result);
                        if (!textContent.isEmpty()) {
                            SemanticBlock semanticBlock = new SemanticBlock();
                            semanticBlock.setTranscriptionId(transcriptionId);
                            semanticBlock.setOrderIndex(order++);
                            semanticBlock.setTextContent(textContent);

                            semanticBlocks.add(semanticBlock);

                            characterCount += getCountOfCharacters(textContent);
                            sentenceCount += getCountOfSentences(textContent);
                        }
                    }
                }

                JsonNode audioEndJsonNode = resultsBlock.get("processed_audio_end");
                if (audioEndJsonNode != null) {
                    String audioEndText = audioEndJsonNode.asText();
                    durationSeconds = Math.max(durationSeconds, Double.parseDouble(audioEndText.replace("s", "")));
                }
            }

            semanticBlockRepository.saveAll(semanticBlocks);

            Transcription transcription = transcriptionRepository.findById(transcriptionId).orElse(null);
            if (transcription != null) {
                transcription.setStatus("DONE");

                transcription.setDurationSeconds(BigDecimal.valueOf(durationSeconds));
                transcription.setCharacterCount(characterCount);
                transcription.setSentenceCount(sentenceCount);

                transcription.setUpdatedAt(LocalDateTime.now());

                transcriptionRepository.save(transcription);
            }

            saveLog(transcriptionId, "download_result", "GET", 200, "Сохранено блоков: " + order);
        } catch (Exception exception) {
            saveLog(transcriptionId, "download_result", "GET", statusFromException(exception), messageFromException(exception));
            updateError(transcriptionId);
        }
    }

    private String getTextContent(JsonNode result) {
        JsonNode normalizedText = result.get("normalized_text");

        if (normalizedText != null) {
            return normalizedText.asText();
        }

        return "";
    }

    private int getCountOfCharacters(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return 0;
        }

        return textContent.replaceAll("\\s+", "").length();
    }

    private int getCountOfSentences(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return 0;
        }

        String[] sentences = textContent.split("[.!?]+");
        int count = 0;

        for (String sentence : sentences) {
            if (!sentence.isBlank()) {
                count++;
            }
        }

        return count;
    }

    private void startTaskWithExistingSberFile(Integer transcriptionId, String token, AudioFile audioFile) throws Exception {
        UUID sberRequestFileId = audioFile.getSberRequestFileId();

        String taskId = createTask(transcriptionId, token, sberRequestFileId);
        if (taskId == null) {
            startTaskWithNewSberFile(transcriptionId, token, audioFile);
        }

        saveSberTaskId(transcriptionId, taskId);
    }

    private void startTaskWithNewSberFile(Integer transcriptionId, String token, AudioFile audioFile) throws Exception {
        UUID sberRequestFileId = uploadFile(transcriptionId, token, Path.of(audioFile.getSystemPath()));
        if (sberRequestFileId == null) {
            updateError(transcriptionId);
            return;
        }

        saveSberRequestFileId(transcriptionId, sberRequestFileId);

        String taskId = createTask(transcriptionId, token, sberRequestFileId);
        if (taskId == null) {
            updateError(transcriptionId);
            return;
        }

        saveSberTaskId(transcriptionId, taskId);
    }

    private UUID uploadFile(Integer transcriptionId, String token, Path filePath) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());

        try {
            FileSystemResource resource = new FileSystemResource(filePath.toFile());
            HttpEntity<FileSystemResource> entity = new HttpEntity<>(resource, headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl + "/data:upload", HttpMethod.POST, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String id = json.get("result").get("request_file_id").asText();

            saveLog(transcriptionId, "upload_file", "POST", response.getStatusCode().value(), "Загружен: " + id);
            return UUID.fromString(id);
        } catch (Exception e) {
            saveLog(transcriptionId, "upload_file", "POST", statusFromException(e), messageFromException(e));
            return null;
        }
    }

    private String createTask(Integer transcriptionId, String token, UUID sberRequestFileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        headers.set("X-Request-ID", UUID.randomUUID().toString());

        ObjectNode body = objectMapper.createObjectNode();
        ObjectNode options = body.putObject("options");
        String audioLanguage = getAudioLanguage(transcriptionId);
        String audioFormat = getAudioFormat(transcriptionId);
        options.put("model", "general");
        options.put("language", audioLanguage);
        options.put("audio_encoding", audioFormat);

        body.put("request_file_id", sberRequestFileId.toString());

        try {
            String requestBody = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl + "/speech:async_recognize", HttpMethod.POST, entity, String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String taskId = json.get("result").get("id").asText();

            saveLog(transcriptionId, "create_task", "POST", response.getStatusCode().value(), "Создана: " + taskId);
            return taskId;
        } catch (Exception exception) {
            saveLog(transcriptionId, "create_task", "POST", statusFromException(exception), messageFromException(exception));
            return null;
        }
    }

    private void saveSberTaskId(Integer transcriptionId, String taskId) throws Exception {
        Transcription transcription = transcriptionRepository.findById(transcriptionId)
                .orElseThrow(() -> new Exception("Транскрипция не найдена"));

        transcription.setSberTaskId(taskId);
        transcription.setStatus("RUNNING");
        transcription.setUpdatedAt(LocalDateTime.now());
        transcriptionRepository.save(transcription);
    }

    private void saveSberRequestFileId(Integer transcriptionId, UUID sberRequestFileId) throws Exception {
        Transcription transcription = transcriptionRepository.findById(transcriptionId)
                .orElseThrow(() -> new Exception("Транскрипция не найдена"));

        AudioFile audioFile = audioFileRepository.findById(transcription.getAudioFileId())
                .orElseThrow(() -> new Exception("Файл не найден"));

        audioFile.setSberRequestFileId(sberRequestFileId);
        audioFile.setUploadAt(LocalDateTime.now());

        audioFileRepository.save(audioFile);
    }

    private String getAudioLanguage(Integer transcriptionId) {
        return DEFAULT_AUDIO_LANGUAGE;
    }

    private String getAudioFormat(Integer transcriptionId) {
        return DEFAULT_AUDIO_FORMAT;
    }

    private int statusFromException(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        return 500;
    }

    private String messageFromException(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            String responseBody = responseException.getResponseBodyAsString();
            if (!responseBody.isBlank()) {
                return exception.getMessage() + "; body=" + responseBody;
            }
        }
        return exception.getMessage();
    }

    private void updateError(Integer transcriptionId) {
        Transcription transcription = transcriptionRepository.findById(transcriptionId).orElse(null);
        if (transcription != null) {
            transcription.setStatus("ERROR");
            transcription.setUpdatedAt(LocalDateTime.now());
            transcriptionRepository.save(transcription);
        }
    }

    private void updateCanceled(Integer transcriptionId) {
        Transcription transcription = transcriptionRepository.findById(transcriptionId).orElse(null);
        if (transcription != null) {
            transcription.setStatus("CANCELED");
            transcription.setUpdatedAt(LocalDateTime.now());
            transcriptionRepository.save(transcription);
        }
    }

    private void saveLog(Integer transcriptionId, String operationType, String httpMethod, int httpStatus, String message) {
        try {
            ExternalCallLog log = new ExternalCallLog();
            log.setTranscriptionId(transcriptionId);
            log.setOperationType(operationType);
            log.setHttpMethod(httpMethod);
            log.setHttpStatus(httpStatus);
            log.setMessage(message != null ? message.substring(0, Math.min(message.length(), 1000)) : null);
            externalCallLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("Не удалось сохранить лог: " + e.getMessage());
        }
    }
}
