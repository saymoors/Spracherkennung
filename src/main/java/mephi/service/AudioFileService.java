package mephi.service;

import mephi.entity.AudioFile;
import mephi.repository.AudioFileRepository;
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
    private static final String SUPPORTED_AUDIOFORMATS = "MP3";
    private static final long SBER_FILE_REQUEST_ID_LIFETIME_HOURS = 72;
    private static final long MAX_FILE_SIZE_BYTES = 200L * 1024 * 1024;

    @Value("${spracherkennung.upload-dir}")
    private String uploadDir;

    private final AudioFileRepository audioFileRepository;

    public AudioFileService(AudioFileRepository audioFileRepository) {
        this.audioFileRepository = audioFileRepository;
    }

    public AudioFile getFileForRecognition(Integer userId, MultipartFile file, boolean isAnewRecognition) throws Exception {
        validateFile(file);
        String originalFilename = getOriginalFilename(file);
        String audioFormat = getAudioformat(file);
        String fileHash = getFilehash(file);

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
        if (file == null || file.isEmpty()) {
            throw new Exception("Файл не выбран");
        }

        String originalFilename = getOriginalFilename(file);
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new Exception("У файла отсутствует имя");
        }
        if (!originalFilename.contains(".") || originalFilename.endsWith(".")) {
            throw new Exception("У файла отсутствует расширение");
        }

        if (!SUPPORTED_AUDIOFORMATS.equals(getAudioformat(file))) {
            throw new Exception("Неподдерживаемый формат файла");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new Exception("Файл слишком большой. Максимальный размер 250 МБ");
        }
    }

    private String getOriginalFilename(MultipartFile file) {
        return file.getOriginalFilename();
    }

    private String getAudioformat(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        return originalFileName.substring(originalFileName.lastIndexOf(".") + 1).toUpperCase();
    }

    private String getFilehash(MultipartFile file) throws Exception {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(file.getBytes()));
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
            String originalFilename = getOriginalFilename(file);

            Path uploadPath = Paths.get(uploadDir);
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
                                      String audioFormat,
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
                && audioFile.getUploadAt().isAfter(LocalDateTime.now().minusHours(SBER_FILE_REQUEST_ID_LIFETIME_HOURS));
    }
}
