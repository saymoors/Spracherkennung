package mephi.repository;

import mephi.entity.Transcription;
import mephi.enums.TranscriptionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, Integer> {
    List<Transcription> findByAudioFileIdOrderByCreatedAtDesc(Integer audioFileId);

    List<Transcription> findByStatus(TranscriptionStatus status);

    Slice<Transcription> findByAudioFileUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
