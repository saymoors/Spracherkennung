package mephi.validation;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;

class AudioFileValidationChainTest {
    private final AudioFileValidationChain validationChain = new AudioFileValidationChain();

    @Test
    void validateAllowsSupportedAudioFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "lecture.mp3",
                "audio/mpeg",
                new byte[]{1, 2, 3}
        );

        assertDoesNotThrow(() -> validationChain.validate(file));
    }

    @Test
    void validateRejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "lecture.mp3",
                "audio/mpeg",
                new byte[]{}
        );

        Exception exception = assertThrows(Exception.class, () -> validationChain.validate(file));

        assertEquals("Файл не выбран", exception.getMessage());
    }

    @Test
    void validateRejectsUnsupportedAudioFormat() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "lecture.m4a",
                "audio/mp4",
                new byte[]{1, 2, 3}
        );

        Exception exception = assertThrows(Exception.class, () -> validationChain.validate(file));

        assertEquals("Неподдерживаемый формат файла", exception.getMessage());
    }
}
