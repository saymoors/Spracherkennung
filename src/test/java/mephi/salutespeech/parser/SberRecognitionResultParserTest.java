package mephi.salutespeech.parser;

import mephi.salutespeech.model.SberRecognitionResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SberRecognitionResultParserTest {
    private final SberRecognitionResultParser parser = new SberRecognitionResultParser();

    @Test
    void parseReadsRealisticSaluteSpeechDownloadResponse() throws Exception {
        String responseBody = """
                [
                  {
                    "results": [
                      {
                        "text": "герберт уэллс история покойного мистера элвишема",
                        "normalized_text": "Герберт уэллс. История покойного мистера элвишема.",
                        "start": "0.560s",
                        "end": "6.840s",
                        "word_alignments": [
                          {
                            "word": "герберт",
                            "start": "0.560s",
                            "end": "1.160s"
                          },
                          {
                            "word": "уэллс",
                            "start": "1.400s",
                            "end": "1.880s"
                          },
                          {
                            "word": "история",
                            "start": "3.640s",
                            "end": "4.120s"
                          }
                        ]
                      }
                    ],
                    "eou": true,
                    "emotions_result": {
                      "positive": 0.0033682967,
                      "neutral": 0.9957911,
                      "negative": 0.00084061106
                    },
                    "processed_audio_start": "0s",
                    "processed_audio_end": "8.602999808s",
                    "backend_info": {
                      "model_name": "transcribation_hq",
                      "model_version": "M-03.007.00-transcribation_hq-02",
                      "server_version": "03.007.01-rh8-trt10-cuda12-01"
                    },
                    "channel": 0,
                    "speaker_info": {
                      "speaker_id": -1,
                      "main_speaker_confidence": 0
                    },
                    "eou_reason": "ORGANIC",
                    "insight": "",
                    "person_identity": {
                      "age": "AGE_NONE",
                      "gender": "GENDER_NONE",
                      "age_score": 0,
                      "gender_score": 0
                    }
                  }
                ]
                """;

        SberRecognitionResult result = parser.parse(responseBody.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of("Герберт уэллс. История покойного мистера элвишема."), result.textBlocks());
        assertEquals(BigDecimal.valueOf(8.602999808), result.durationSeconds());
        assertEquals(45, result.characterCount());
        assertEquals(2, result.sentenceCount());
    }

    @Test
    void parseReadsSeveralOuterBlocksAndUsesMaximumAudioEnd() throws Exception {
        String responseBody = """
                [
                  {
                    "results": [
                      {
                        "normalized_text": "Герберт уэллс. История покойного мистера элвишема."
                      }
                    ],
                    "processed_audio_end": "8.602999808s"
                  },
                  {
                    "results": [
                      {
                        "normalized_text": "Он был человек одинокий, самоучка, его знали в бермингеме как предприимчивого журналиста."
                      }
                    ],
                    "processed_audio_end": "57.050001408s"
                  }
                ]
                """;

        SberRecognitionResult result = parser.parse(responseBody.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of(
                "Герберт уэллс. История покойного мистера элвишема.",
                "Он был человек одинокий, самоучка, его знали в бермингеме как предприимчивого журналиста."
        ), result.textBlocks());
        assertEquals(BigDecimal.valueOf(57.050001408), result.durationSeconds());
        assertEquals(123, result.characterCount());
        assertEquals(3, result.sentenceCount());
    }

    @Test
    void parseIgnoresResultsWithoutNormalizedText() throws Exception {
        String responseBody = """
                [
                  {
                    "processed_audio_end": "1.0s",
                    "results": [
                      {
                        "text": "Этот текст не должен попасть в результат"
                      }
                    ]
                  }
                ]
                """;

        SberRecognitionResult result = parser.parse(responseBody.getBytes(StandardCharsets.UTF_8));

        assertEquals(List.of(), result.textBlocks());
        assertEquals(BigDecimal.valueOf(1.0), result.durationSeconds());
        assertEquals(0, result.characterCount());
        assertEquals(0, result.sentenceCount());
    }
}
