package mephi.validation.file;

import mephi.enums.AudioFormat;
import mephi.validation.BaseValidationRule;
import org.springframework.web.multipart.MultipartFile;

public class SupportedAudioFormatValidationRule extends BaseValidationRule<MultipartFile> {
    @Override
    protected void check(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename.lastIndexOf(".");

        if (dotIndex == -1 || dotIndex == originalFilename.length() - 1) {
            throw new Exception("Неподдерживаемый формат файла");
        }

        String audioFormat = originalFilename.substring(dotIndex + 1).toUpperCase();

        if (!AudioFormat.isSupported(audioFormat)) {
            throw new Exception("Неподдерживаемый формат файла");
        }
    }
}
