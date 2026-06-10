package mephi.service;

import mephi.entity.AudioFile;
import mephi.entity.Transcription;
import mephi.salutespeech.client.SberAuthClient;
import mephi.salutespeech.client.SberFileClient;
import mephi.salutespeech.client.SberTaskClient;
import mephi.salutespeech.model.SberRecognitionResult;
import mephi.salutespeech.model.SberCreateTaskResponse;
import mephi.salutespeech.model.SberDownloadResponse;
import mephi.salutespeech.model.SberUploadFileResponse;
import mephi.salutespeech.model.SberPollTaskResponse;
import mephi.salutespeech.parser.SberRecognitionResultParser;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SaluteSpeechService {
    private static final long SBER_REQUEST_FILE_ID_LIFETIME_HOURS = 72;
    private static final long SBER_RESPONSE_FILE_ID_LIFETIME_HOURS = 72;

    private final SberAuthClient sberAuthClient;
    private final SberFileClient sberFileClient;
    private final SberTaskClient sberTaskClient;
    private final SberRecognitionResultParser sberRecognitionResultParser;
    private final SaluteSpeechDataService saluteSpeechDataService;
    private final ExternalCallLogService externalCallLogService;

    public SaluteSpeechService(SberAuthClient sberAuthClient,
                               SberFileClient sberFileClient,
                               SberTaskClient sberTaskClient,
                               SberRecognitionResultParser sberRecognitionResultParser,
                               SaluteSpeechDataService saluteSpeechDataService,
                               ExternalCallLogService externalCallLogService) {
        this.sberAuthClient = sberAuthClient;
        this.sberFileClient = sberFileClient;
        this.sberTaskClient = sberTaskClient;
        this.sberRecognitionResultParser = sberRecognitionResultParser;
        this.saluteSpeechDataService = saluteSpeechDataService;
        this.externalCallLogService = externalCallLogService;
    }

    public void startRecognition(Integer transcriptionId) {
        try {
            Transcription transcription = saluteSpeechDataService.getTranscription(transcriptionId);
            AudioFile audioFile = saluteSpeechDataService.getAudioFile(transcription.getAudioFileId());

            String token = sberAuthClient.getAccessToken();
            externalCallLogService.save(transcriptionId, "get_token", "POST", 200, "Токен получен");

            if (isSberResponseFileIdAlive(transcription)) {
                downloadAndSaveResult(transcriptionId, transcription.getSberResponseFileId(), token);
                return;
            }

            if (isSberRequestFileIdAlive(audioFile)) {
                startTaskWithExistingSberFile(transcriptionId, token, audioFile);
                return;
            }

            startTaskWithNewSberFile(transcriptionId, token, audioFile);
        } catch (Exception exception) {
            externalCallLogService.save(
                    transcriptionId,
                    "start_recognition",
                    "POST",
                    externalCallLogService.statusFromException(exception),
                    externalCallLogService.messageFromException(exception)
            );
            saluteSpeechDataService.markAsError(transcriptionId);
        }
    }

    public void pollSberTask(Integer transcriptionId) {
        try {
            Transcription transcription = saluteSpeechDataService.getTranscription(transcriptionId);
            if (!"RUNNING".equals(transcription.getStatus())) {
                return;
            }

            String token = sberAuthClient.getAccessToken();
            SberPollTaskResponse pollResponse = sberTaskClient.pollTask(transcription.getSberTaskId(), token);

            externalCallLogService.save(
                    transcriptionId,
                    "poll_sber_task",
                    "GET",
                    pollResponse.httpStatus(),
                    "Статус: " + pollResponse.status()
            );

            switch (pollResponse.status()) {
                case "DONE" -> {
                    saluteSpeechDataService.saveSberResponseFileId(transcriptionId, pollResponse.responseFileId());
                    downloadAndSaveResult(transcriptionId, pollResponse.responseFileId(), token);
                }
                case "ERROR" -> saluteSpeechDataService.markAsError(transcriptionId);
                case "CANCELED" -> saluteSpeechDataService.markAsCanceled(transcriptionId);
            }
        } catch (Exception exception) {
            externalCallLogService.save(
                    transcriptionId,
                    "poll_sber_task",
                    "GET",
                    externalCallLogService.statusFromException(exception),
                    externalCallLogService.messageFromException(exception)
            );
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

    private void startTaskWithExistingSberFile(Integer transcriptionId, String token, AudioFile audioFile) throws Exception {
        UUID sberRequestFileId = audioFile.getSberRequestFileId();

        try {
            SberCreateTaskResponse createTaskResponse = createSberTask(transcriptionId, token, sberRequestFileId);
            saluteSpeechDataService.saveSberTaskId(transcriptionId, createTaskResponse.taskId());
        } catch (Exception exception) {
            externalCallLogService.save(
                    transcriptionId,
                    "create_sber_task",
                    "POST",
                    externalCallLogService.statusFromException(exception),
                    externalCallLogService.messageFromException(exception)
            );
            startTaskWithNewSberFile(transcriptionId, token, audioFile);
        }
    }

    private void startTaskWithNewSberFile(Integer transcriptionId, String token, AudioFile audioFile) throws Exception {
        SberUploadFileResponse uploadResponse;
        try {
            uploadResponse = sberFileClient.uploadFile(audioFile.getSystemPath(), token);
        } catch (Exception exception) {
            externalCallLogService.save(
                    transcriptionId,
                    "upload_file_to_sber",
                    "POST",
                    externalCallLogService.statusFromException(exception),
                    externalCallLogService.messageFromException(exception)
            );
            saluteSpeechDataService.markAsError(transcriptionId);
            return;
        }

        saluteSpeechDataService.saveSberRequestFileId(transcriptionId, uploadResponse.requestFileId());
        externalCallLogService.save(
                transcriptionId,
                "upload_file_to_sber",
                "POST",
                uploadResponse.httpStatus(),
                "Загружен: " + uploadResponse.requestFileId()
        );

        try {
            SberCreateTaskResponse createTaskResponse = createSberTask(transcriptionId, token, uploadResponse.requestFileId());
            saluteSpeechDataService.saveSberTaskId(transcriptionId, createTaskResponse.taskId());
        } catch (Exception exception) {
            externalCallLogService.save(
                    transcriptionId,
                    "create_sber_task",
                    "POST",
                    externalCallLogService.statusFromException(exception),
                    externalCallLogService.messageFromException(exception)
            );
            saluteSpeechDataService.markAsError(transcriptionId);
        }
    }

    private SberCreateTaskResponse createSberTask(Integer transcriptionId, String token, UUID sberRequestFileId) throws Exception {
        String audioLanguage = saluteSpeechDataService.getAudioLanguage(transcriptionId);
        String audioFormat = saluteSpeechDataService.getAudioFormat(transcriptionId);
        SberCreateTaskResponse createTaskResponse = sberTaskClient.createTask(sberRequestFileId, audioLanguage, audioFormat, token);

        externalCallLogService.save(
                transcriptionId,
                "create_sber_task",
                "POST",
                createTaskResponse.httpStatus(),
                "Создана: " + createTaskResponse.taskId()
        );

        return createTaskResponse;
    }

    private void downloadAndSaveResult(Integer transcriptionId, UUID sberResponseFileId, String token) {
        try {
            SberDownloadResponse downloadResponse = sberFileClient.downloadResult(sberResponseFileId, token);
            SberRecognitionResult sberRecognitionResult = sberRecognitionResultParser.parse(downloadResponse.body());

            if (sberRecognitionResult.textBlocks().isEmpty()) {
                externalCallLogService.save(
                        transcriptionId,
                        "download_sber_result",
                        "GET",
                        downloadResponse.httpStatus(),
                        "Результат распознавания пустой"
                );
                saluteSpeechDataService.markAsError(transcriptionId);
                return;
            }

            saluteSpeechDataService.saveDoneResult(transcriptionId, sberRecognitionResult);
            externalCallLogService.save(
                    transcriptionId,
                    "download_sber_result",
                    "GET",
                    downloadResponse.httpStatus(),
                    "Сохранено блоков: " + sberRecognitionResult.textBlocks().size()
            );
        } catch (Exception exception) {
            externalCallLogService.save(
                    transcriptionId,
                    "download_sber_result",
                    "GET",
                    externalCallLogService.statusFromException(exception),
                    externalCallLogService.messageFromException(exception)
            );
            saluteSpeechDataService.markAsError(transcriptionId);
        }
    }
}
