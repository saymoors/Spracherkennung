package mephi.repository;

import mephi.entity.ExternalCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalCallLogRepository extends JpaRepository<ExternalCallLog, Integer> {
    Optional<ExternalCallLog> findTopByTranscriptionIdOrderByCreatedAtDesc(Integer transcriptionId);
}
