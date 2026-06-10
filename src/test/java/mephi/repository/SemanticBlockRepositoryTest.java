package mephi.repository;

import mephi.entity.AudioFile;
import mephi.entity.SemanticBlock;
import mephi.entity.Transcription;
import mephi.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SemanticBlockRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AudioFileRepository audioFileRepository;

    @Autowired
    private TranscriptionRepository transcriptionRepository;

    @Autowired
    private SemanticBlockRepository semanticBlockRepository;

    @Test
    void findByTranscriptionIdOrderByOrderIndexAscReturnsBlocksInCorrectOrder() {
        User user = saveUser();
        AudioFile audioFile = saveAudioFile(user.getId());
        Transcription transcription = saveTranscription(audioFile.getId());
        saveSemanticBlock(transcription.getId(), 2, "Второй блок");
        saveSemanticBlock(transcription.getId(), 1, "Первый блок");

        List<SemanticBlock> semanticBlocks = semanticBlockRepository
                .findByTranscriptionIdOrderByOrderIndexAsc(transcription.getId());

        assertEquals(2, semanticBlocks.size());
        assertEquals("Первый блок", semanticBlocks.get(0).getTextContent());
        assertEquals("Второй блок", semanticBlocks.get(1).getTextContent());
    }

    private User saveUser() {
        User user = new User();
        user.setLogin("student");
        user.setEmail("student@gmail.com");
        user.setPasswordHash("hash");
        return userRepository.save(user);
    }

    private AudioFile saveAudioFile(Integer userId) {
        AudioFile audioFile = new AudioFile();
        audioFile.setUserId(userId);
        audioFile.setName("lecture.mp3");
        audioFile.setFormat("MP3");
        audioFile.setFileHash("hash");
        audioFile.setSystemPath("uploads/lecture.mp3");
        audioFile.setSizeBytes(100L);
        return audioFileRepository.save(audioFile);
    }

    private Transcription saveTranscription(Integer audioFileId) {
        Transcription transcription = new Transcription();
        transcription.setAudioFileId(audioFileId);
        transcription.setLanguage("ru-RU");
        transcription.setStatus("DONE");
        return transcriptionRepository.save(transcription);
    }

    private SemanticBlock saveSemanticBlock(Integer transcriptionId, Integer orderIndex, String textContent) {
        SemanticBlock semanticBlock = new SemanticBlock();
        semanticBlock.setTranscriptionId(transcriptionId);
        semanticBlock.setOrderIndex(orderIndex);
        semanticBlock.setTextContent(textContent);
        return semanticBlockRepository.save(semanticBlock);
    }
}
