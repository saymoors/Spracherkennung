package mephi.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transcriptions")
public class Transcription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "audio_file_id", nullable = false)
    private Integer audioFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audio_file_id", insertable = false, updatable = false)
    private AudioFile audioFile;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "sber_task_id")
    private String sberTaskId;

    @Column(name = "sber_response_file_id")
    private UUID sberResponseFileId;

    @Column(name = "sber_response_file_received_at")
    private LocalDateTime sberResponseFileReceivedAt;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "duration_seconds", precision = 10, scale = 2)
    private BigDecimal durationSeconds;

    @Column(name = "character_count")
    private Integer characterCount;

    @Column(name = "sentence_count")
    private Integer sentenceCount;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAudioFileId() {
        return audioFileId;
    }

    public void setAudioFileId(Integer audioFileId) {
        this.audioFileId = audioFileId;
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSberTaskId() {
        return sberTaskId;
    }

    public void setSberTaskId(String sberTaskId) {
        this.sberTaskId = sberTaskId;
    }

    public UUID getSberResponseFileId() {
        return sberResponseFileId;
    }

    public void setSberResponseFileId(UUID sberResponseFileId) {
        this.sberResponseFileId = sberResponseFileId;
    }

    public LocalDateTime getSberResponseFileReceivedAt() {
        return sberResponseFileReceivedAt;
    }

    public void setSberResponseFileReceivedAt(LocalDateTime sberResponseFileReceivedAt) {
        this.sberResponseFileReceivedAt = sberResponseFileReceivedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public BigDecimal getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(BigDecimal durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getCharacterCount() {
        return characterCount;
    }

    public void setCharacterCount(Integer characterCount) {
        this.characterCount = characterCount;
    }

    public Integer getSentenceCount() {
        return sentenceCount;
    }

    public void setSentenceCount(Integer sentenceCount) {
        this.sentenceCount = sentenceCount;
    }
}
