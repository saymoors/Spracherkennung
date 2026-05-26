package mephi.dto;

import java.util.List;

public class TranscriptionDetails {
    private String errorMessage;
    private Double durationSeconds;
    private Integer characterCount;
    private Integer sentenceCount;
    private List<SemanticBlock> semanticBlocks;

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Double getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Double durationSeconds) {
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

    public List<SemanticBlock> getSemanticBlocks() {
        return semanticBlocks;
    }

    public void setSemanticBlocks(List<SemanticBlock> semanticBlocks) {
        this.semanticBlocks = semanticBlocks;
    }
}
