package mephi.salutespeech.model;

import java.math.BigDecimal;
import java.util.List;

public record SberRecognitionResult(List<String> textBlocks, BigDecimal durationSeconds, int characterCount, int sentenceCount) {
}
