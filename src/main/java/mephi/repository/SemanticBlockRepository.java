package mephi.repository;

import mephi.entity.SemanticBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SemanticBlockRepository extends JpaRepository<SemanticBlock, Integer> {
    List<SemanticBlock> findByTranscriptionIdOrderByOrderIndexAsc(Integer transcriptionId);
}
