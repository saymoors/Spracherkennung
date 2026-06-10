package mephi.service;

import mephi.entity.AudioFile;
import mephi.entity.Transcription;
import mephi.salutespeech.client.SberAuthClient;
import mephi.salutespeech.client.SberFileClient;
import mephi.salutespeech.client.SberTaskClient;
import mephi.salutespeech.model.SberCreateTaskResponse;
import mephi.salutespeech.model.SberUploadFileResponse;
import mephi.salutespeech.parser.SberRecognitionResultParser;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaluteSpeechServiceTest {

    @Test
    void startRecognitionUploadsFileAndCreatesSberTask() throws Exception {
        SberAuthClient sberAuthClient = mock(SberAuthClient.class);
        SberFileClient sberFileClient = mock(SberFileClient.class);
        SberTaskClient sberTaskClient = mock(SberTaskClient.class);
        SberRecognitionResultParser parser = mock(SberRecognitionResultParser.class);
        SaluteSpeechDataService dataService = mock(SaluteSpeechDataService.class);
        ExternalCallLogService logService = mock(ExternalCallLogService.class);
        SaluteSpeechService saluteSpeechService = new SaluteSpeechService(
                sberAuthClient,
                sberFileClient,
                sberTaskClient,
                parser,
                dataService,
                logService
        );

        Transcription transcription = new Transcription();
        transcription.setId(1);
        transcription.setAudioFileId(10);
        transcription.setLanguage("ru-RU");

        AudioFile audioFile = new AudioFile();
        audioFile.setId(10);
        audioFile.setSystemPath("uploads/lecture.mp3");
        audioFile.setFormat("MP3");

        UUID requestFileId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(dataService.getTranscription(1)).thenReturn(transcription);
        when(dataService.getAudioFile(10)).thenReturn(audioFile);
        when(sberAuthClient.getAccessToken()).thenReturn("access-token");
        when(sberFileClient.uploadFile("uploads/lecture.mp3", "MP3", "access-token"))
                .thenReturn(new SberUploadFileResponse(requestFileId, 200));
        when(dataService.getAudioLanguage(1)).thenReturn("ru-RU");
        when(dataService.getAudioFormat(1)).thenReturn("MP3");
        when(sberTaskClient.createTask(requestFileId, "ru-RU", "MP3", "access-token"))
                .thenReturn(new SberCreateTaskResponse("task-1", 200));

        saluteSpeechService.startRecognition(1);

        verify(dataService).saveSberRequestFileId(1, requestFileId);
        verify(dataService).saveSberTaskId(1, "task-1");
    }
}
