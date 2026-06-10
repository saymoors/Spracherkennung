package mephi.service;

import mephi.enums.AudioFormat;
import mephi.entity.AudioFile;
import mephi.repository.AudioFileRepository;
import mephi.validation.AudioFileValidationChain;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AudioFileService {
    private static final long SBER_REQUEST_FILE_ID_LIFETIME_HOURS = 72;

    @Value("${spracherkennung.upload-dir}")
    private String uploadDir;

    private final AudioFileRepository audioFileRepository;
    private final AudioFileValidationChain audioFileValidationChain;

    public AudioFileService(AudioFileRepository audioFileRepository,
                            AudioFileValidationChain audioFileValidationChain) {
        this.audioFileRepository = audioFileRepository;
        this.audioFileValidationChain = audioFileValidationChain;
    }

    public AudioFile getFileForRecognition(Integer userId, MultipartFile file, boolean isAnewRecognition) throws Exception {
        validateFile(file);
        String originalFilename = getOriginalFileName(file);
        AudioFormat audioFormat = getAudioFormat(file);
        String fileHash = getFileHash(file);

        List<AudioFile> sameHashAudioFiles = audioFileRepository.findByUserIdAndFileHashOrderByUploadAtDesc(userId, fileHash);

        AudioFile audioFile = audioFileRepository.findFirstByUserIdAndFileHashAndNameOrderByUploadAtDesc(userId, fileHash, originalFilename);

        if (isAnewRecognition && audioFile == null) {
            throw new Exception("Файл еще не существует в системе. Сначала распознайте его");
        }

        if (audioFile == null) {
            String systemPath = saveFile(file);
            AudioFile reusableAudioFile = findReusableAudioFile(sameHashAudioFiles);

            audioFile = createAudioFile(
                    userId,
                    originalFilename,
                    systemPath,
                    audioFormat,
                    fileHash,
                    file.getSize(),
                    reusableAudioFile != null ? reusableAudioFile.getSberRequestFileId() : null,
                    reusableAudioFile != null ? reusableAudioFile.getUploadAt() : null
            );
        } else {
            checkFilePath(audioFile, file);
        }

        return audioFile;
    }

    private void validateFile(MultipartFile file) throws Exception {
        audioFileValidationChain.validate(file);
    }

    private String getOriginalFileName(MultipartFile file) {
        return file.getOriginalFilename();
    }

    private AudioFormat getAudioFormat(MultipartFile file) throws Exception {
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toUpperCase();
        return AudioFormat.fromExtension(extension);
    }

    private String getFileHash(MultipartFile file) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;

            try (var inputStream = file.getInputStream()) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception exception) {
            throw new Exception("Ошибка расчета хэша файла: " + exception.getMessage());
        }
    }

    private void checkFilePath(AudioFile audioFile, MultipartFile file) throws Exception {
        Path filePath = Paths.get(audioFile.getSystemPath());
        if (!Files.exists(filePath)) {
            audioFile.setSystemPath(saveFile(file));
            audioFileRepository.save(audioFile);
        }
    }

    private String saveFile(MultipartFile file) throws Exception {
        try {
            String originalFilename = getOriginalFileName(file);

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(uploadPath);

            String savedFilename = UUID.randomUUID() + "_" + originalFilename;
            Path filePath = uploadPath.resolve(savedFilename);

            file.transferTo(filePath.toFile());
            return filePath.toString();
        } catch (Exception exception) {
            throw new Exception("Ошибка сохранения файла: " + exception.getMessage());
        }
    }

    private AudioFile createAudioFile(Integer userId,
                                      String originalFilename,
                                      String systemPath,
                                      AudioFormat audioFormat,
                                      String fileHash,
                                      long sizeBytes,
                                      UUID sberRequestFileId,
                                      LocalDateTime uploadAt) {
        AudioFile audioFile = new AudioFile();
        audioFile.setUserId(userId);
        audioFile.setName(originalFilename);
        audioFile.setSystemPath(systemPath);
        audioFile.setFormat(audioFormat);
        audioFile.setFileHash(fileHash);
        audioFile.setSizeBytes(sizeBytes);
        audioFile.setSberRequestFileId(sberRequestFileId);
        audioFile.setUploadAt(uploadAt);
        return audioFileRepository.save(audioFile);
    }

    private AudioFile findReusableAudioFile(List<AudioFile> sameHashAudioFiles) {
        for (AudioFile sameHashAudioFile : sameHashAudioFiles) {
            if (isSberRequestFileIdAlive(sameHashAudioFile)) {
                return sameHashAudioFile;
            }
        }
        return null;
    }

    private boolean isSberRequestFileIdAlive(AudioFile audioFile) {
        return audioFile.getSberRequestFileId() != null
                && audioFile.getUploadAt() != null
                && audioFile.getUploadAt().isAfter(LocalDateTime.now().minusHours(SBER_REQUEST_FILE_ID_LIFETIME_HOURS));
    }
}
