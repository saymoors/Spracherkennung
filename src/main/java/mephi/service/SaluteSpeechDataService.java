package mephi.service;

import mephi.entity.AudioFile;
import mephi.entity.SemanticBlock;
import mephi.entity.Transcription;
import mephi.enums.AudioFormat;
import mephi.enums.TranscriptionStatus;
import mephi.repository.AudioFileRepository;
import mephi.repository.SemanticBlockRepository;
import mephi.repository.TranscriptionRepository;
import mephi.salutespeech.model.SberRecognitionResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SaluteSpeechDataService {
    private final AudioFileRepository audioFileRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final SemanticBlockRepository semanticBlockRepository;

    public SaluteSpeechDataService(AudioFileRepository audioFileRepository,
                                   TranscriptionRepository transcriptionRepository,
                                   SemanticBlockRepository semanticBlockRepository) {
        this.audioFileRepository = audioFileRepository;
        this.transcriptionRepository = transcriptionRepository;
        this.semanticBlockRepository = semanticBlockRepository;
    }

    public Transcription getTranscription(Integer transcriptionId) throws Exception {
        return transcriptionRepository.findById(transcriptionId)
                .orElseThrow(() -> new Exception("Транскрипция не найдена"));
    }

    public AudioFile getAudioFile(Integer audioFileId) throws Exception {
        return audioFileRepository.findById(audioFileId)
                .orElseThrow(() -> new Exception("Файл не найден"));
    }

    public void saveSberTaskId(Integer transcriptionId, String sberTaskId) throws Exception {
        Transcription transcription = getTranscription(transcriptionId);
        transcription.setSberTaskId(sberTaskId);
        transcription.setStatus(TranscriptionStatus.RUNNING);
        transcription.setUpdatedAt(LocalDateTime.now());
        transcriptionRepository.save(transcription);
    }

    public void saveSberRequestFileId(Integer transcriptionId, UUID sberRequestFileId) throws Exception {
        Transcription transcription = getTranscription(transcriptionId);
        AudioFile audioFile = getAudioFile(transcription.getAudioFileId());

        audioFile.setSberRequestFileId(sberRequestFileId);
        audioFile.setUploadAt(LocalDateTime.now());
        audioFileRepository.save(audioFile);
    }

    public void saveSberResponseFileId(Integer transcriptionId, UUID sberResponseFileId) throws Exception {
        Transcription transcription = getTranscription(transcriptionId);
        LocalDateTime now = LocalDateTime.now();

        transcription.setSberResponseFileId(sberResponseFileId);
        transcription.setSberResponseFileReceivedAt(now);
        transcription.setUpdatedAt(now);
        transcriptionRepository.save(transcription);
    }

    @Transactional
    public void saveDoneResult(Integer transcriptionId, SberRecognitionResult sberRecognitionResult) throws Exception {
        Transcription transcription = getTranscription(transcriptionId);
        List<SemanticBlock> semanticBlocks = createSemanticBlocks(transcriptionId, sberRecognitionResult.textBlocks());

        semanticBlockRepository.saveAll(semanticBlocks);
        transcription.setStatus(TranscriptionStatus.DONE);
        transcription.setDurationSeconds(sberRecognitionResult.durationSeconds());
        transcription.setCharacterCount(sberRecognitionResult.characterCount());
        transcription.setSentenceCount(sberRecognitionResult.sentenceCount());
        transcription.setUpdatedAt(LocalDateTime.now());
        transcriptionRepository.save(transcription);
    }

    public String getAudioLanguage(Integer transcriptionId) throws Exception {
        return getTranscription(transcriptionId).getLanguage();
    }

    public AudioFormat getAudioFormat(Integer transcriptionId) throws Exception {
        Transcription transcription = getTranscription(transcriptionId);
        AudioFile audioFile = getAudioFile(transcription.getAudioFileId());
        return audioFile.getFormat();
    }

    public void markAsError(Integer transcriptionId) {
        markAsStatus(transcriptionId, TranscriptionStatus.ERROR);
    }

    public void markAsCanceled(Integer transcriptionId) {
        markAsStatus(transcriptionId, TranscriptionStatus.CANCELED);
    }

    private void markAsStatus(Integer transcriptionId, TranscriptionStatus status) {
        Transcription transcription = transcriptionRepository.findById(transcriptionId).orElse(null);
        if (transcription != null) {
            transcription.setStatus(status);
            transcription.setUpdatedAt(LocalDateTime.now());
            transcriptionRepository.save(transcription);
        }
    }

    private List<SemanticBlock> createSemanticBlocks(Integer transcriptionId, List<String> textBlocks) {
        List<SemanticBlock> semanticBlocks = new ArrayList<>();

        for (int i = 0; i < textBlocks.size(); i++) {
            SemanticBlock semanticBlock = new SemanticBlock();
            semanticBlock.setTranscriptionId(transcriptionId);
            semanticBlock.setOrderIndex(i);
            semanticBlock.setTextContent(textBlocks.get(i));
            semanticBlocks.add(semanticBlock);
        }

        return semanticBlocks;
    }
}
