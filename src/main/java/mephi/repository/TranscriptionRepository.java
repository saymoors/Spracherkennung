package mephi.repository;

import mephi.entity.Transcription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranscriptionRepository extends JpaRepository<Transcription, Integer> {
    List<Transcription> findByAudioFileIdOrderByCreatedAtDesc(Integer audioFileId);

    List<Transcription> findByStatus(String status);

    Page<Transcription> findByAudioFileUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
