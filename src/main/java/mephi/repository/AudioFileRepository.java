package mephi.repository;

import mephi.entity.AudioFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AudioFileRepository extends JpaRepository<AudioFile, Integer> {
    List<AudioFile> findByUserIdAndFileHashOrderByUploadAtDesc(Integer userId, String fileHash);

    Optional<AudioFile> findFirstByUserIdAndFileHashAndNameOrderByUploadAtDesc(Integer userId, String fileHash, String name);
}
