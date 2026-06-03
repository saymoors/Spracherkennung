package mephi.service;

import mephi.repository.*;
import org.springframework.stereotype.Service;

@Service
public class TranscriptionService {
    private static final String RECOGNITION_LANGUAGE = "ru-RU";

    private final AudioFileService audioFileService;
    private final AudioFileRepository audioFileRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final SemanticBlockRepository semanticBlockRepository;
    private final ExternalCallLogRepository externalCallLogRepository;
    private final ExportService exportService;
    private final SaluteSpeechService saluteSpeechService;

    public TranscriptionService(AudioFileService audioFileService,
                                AudioFileRepository audioFileRepository,
                                TranscriptionRepository transcriptionRepository,
                                SemanticBlockRepository semanticBlockRepository,
                                ExternalCallLogRepository externalCallLogRepository,
                                ExportService exportService,
                                SaluteSpeechService saluteSpeechService) {
        this.audioFileService = audioFileService;
        this.audioFileRepository = audioFileRepository;
        this.transcriptionRepository = transcriptionRepository;
        this.semanticBlockRepository = semanticBlockRepository;
        this.externalCallLogRepository = externalCallLogRepository;
        this.exportService = exportService;
        this.saluteSpeechService = saluteSpeechService;
    }
}
