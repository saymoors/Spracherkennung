package mephi.validation;

import mephi.validation.file.FileNameRequiredValidationRule;
import mephi.validation.file.FileRequiredValidationRule;
import mephi.validation.file.FileSizeLimitValidationRule;
import mephi.validation.file.SupportedAudioFormatValidationRule;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AudioFileValidationChain {
    private final ValidationRule<MultipartFile> rules;

    public AudioFileValidationChain() {
        rules = new FileRequiredValidationRule();
        rules
                .setNext(new FileNameRequiredValidationRule())
                .setNext(new SupportedAudioFormatValidationRule())
                .setNext(new FileSizeLimitValidationRule());
    }

    public void validate(MultipartFile file) throws Exception {
        rules.validate(file);
    }
}
