package mephi.salutespeech.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import mephi.salutespeech.model.SberRecognitionResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class SberRecognitionResultParser {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SberRecognitionResult parse(byte[] responseBody) throws Exception {
        JsonNode body = objectMapper.readTree(responseBody);

        List<String> textBlocks = new ArrayList<>();
        double durationSeconds = 0.0;
        int characterCount = 0;
        int sentenceCount = 0;

        for (JsonNode resultBlock : body) {
            JsonNode results = resultBlock.get("results");
            if (results != null) {
                for (JsonNode result : results) {
                    String textContent = getTextContent(result);
                    if (!textContent.isEmpty()) {
                        textBlocks.add(textContent);
                        characterCount += getCountOfCharacters(textContent);
                        sentenceCount += getCountOfSentences(textContent);
                    }
                }
            }

            JsonNode audioEndJsonNode = resultBlock.get("processed_audio_end");
            if (audioEndJsonNode != null) {
                String audioEndText = audioEndJsonNode.asText();
                durationSeconds = Math.max(durationSeconds, Double.parseDouble(audioEndText.replace("s", "")));
            }
        }

        return new SberRecognitionResult(
                textBlocks,
                BigDecimal.valueOf(durationSeconds),
                characterCount,
                sentenceCount
        );
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
}
