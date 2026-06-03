package mephi.service;

import mephi.dto.*;
import mephi.entity.AudioFile;
import mephi.entity.SemanticBlock;
import mephi.entity.Transcription;
import mephi.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

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

    public void recognize(Integer userId, MultipartFile file) throws Exception {
        AudioFile audioFile = audioFileService.getFileForRecognition(userId, file, false);
        List<AudioFile> sameHashAudioFiles = audioFileRepository
                .findByUserIdAndFileHashOrderByUploadAtDesc(userId, audioFile.getFileHash());
        Transcription reusableTranscription = findReusableTranscription(sameHashAudioFiles);

        Transcription newTranscription = createNewTranscription(audioFile);

        if (hasSemanticBlocks(reusableTranscription)) {
            copySberResponseFileId(reusableTranscription, newTranscription);
            copyCompletedResult(reusableTranscription, newTranscription);
            return;
        }

        if (hasSberResponseFileId(reusableTranscription)) {
            copySberResponseFileId(reusableTranscription, newTranscription);
        }

        transcriptionRepository.save(newTranscription);
        saluteSpeechService.startRecognition(newTranscription.getId());
    }

    public void recognizeAgain(Integer userId, MultipartFile file) throws Exception {
        AudioFile audioFile = audioFileService.getFileForRecognition(userId, file, true);

        Transcription newTranscription = createNewTranscription(audioFile);
        saluteSpeechService.startRecognition(newTranscription.getId());
    }

    private Transcription findReusableTranscription(List<AudioFile> sameHashAudioFiles) {
        Transcription reusableResponseFileTranscription = null;

        for (AudioFile sameHashAudioFile : sameHashAudioFiles) {
            List<Transcription> sameHashAudioFileTranscriptions = transcriptionRepository
                    .findByAudioFileIdOrderByCreatedAtDesc(sameHashAudioFile.getId());

            for (Transcription sameHashAudioFileTranscription : sameHashAudioFileTranscriptions) {
                if (hasSemanticBlocks(sameHashAudioFileTranscription)) {
                    return sameHashAudioFileTranscription;
                }
                if (reusableResponseFileTranscription == null && hasSberResponseFileId(sameHashAudioFileTranscription)) {
                    reusableResponseFileTranscription = sameHashAudioFileTranscription;
                }
            }
        }

        return reusableResponseFileTranscription;
    }

    private Transcription createNewTranscription(AudioFile audioFile) {
        Transcription transcription = new Transcription();
        transcription.setAudioFileId(audioFile.getId());
        transcription.setLanguage(RECOGNITION_LANGUAGE);
        transcription.setStatus("NEW");
        transcription.setCreatedAt(LocalDateTime.now());
        transcription.setUpdatedAt(LocalDateTime.now());
        return transcriptionRepository.save(transcription);
    }

    private boolean hasSemanticBlocks(Transcription transcription) {
        return transcription != null
                && "DONE".equals(transcription.getStatus())
                && !semanticBlockRepository.findByTranscriptionIdOrderByOrderIndexAsc(transcription.getId()).isEmpty();
    }

    private boolean hasSberResponseFileId(Transcription transcription) {
        return transcription != null
                && transcription.getSberResponseFileId() != null
                && transcription.getSberResponseFileReceivedAt() != null;
    }

    private void copySberResponseFileId(Transcription oldTranscription, Transcription newTranscription) {
        newTranscription.setSberResponseFileId(oldTranscription.getSberResponseFileId());
        newTranscription.setSberResponseFileReceivedAt(oldTranscription.getSberResponseFileReceivedAt());
    }

    private void copyCompletedResult(Transcription oldTranscription, Transcription newTranscription) {
        newTranscription.setStatus(oldTranscription.getStatus());
        newTranscription.setDurationSeconds(oldTranscription.getDurationSeconds());
        newTranscription.setCharacterCount(oldTranscription.getCharacterCount());
        newTranscription.setSentenceCount(oldTranscription.getSentenceCount());
        newTranscription.setUpdatedAt(LocalDateTime.now());
        newTranscription = transcriptionRepository.save(newTranscription);

        List<SemanticBlock> oldTranscriptionSemanticBlocks = semanticBlockRepository
                .findByTranscriptionIdOrderByOrderIndexAsc(oldTranscription.getId());
        for (SemanticBlock oldTranscriptionSemanticBlock : oldTranscriptionSemanticBlocks) {
            SemanticBlock newTranscriptionSemanticBlock = new SemanticBlock();
            newTranscriptionSemanticBlock.setTranscriptionId(newTranscription.getId());
            newTranscriptionSemanticBlock.setOrderIndex(oldTranscriptionSemanticBlock.getOrderIndex());
            newTranscriptionSemanticBlock.setTextContent(oldTranscriptionSemanticBlock.getTextContent());
            semanticBlockRepository.save(newTranscriptionSemanticBlock);
        }
    }
}
