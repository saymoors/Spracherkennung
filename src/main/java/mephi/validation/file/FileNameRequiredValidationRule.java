package mephi.validation.file;

import mephi.validation.BaseValidationRule;
import org.springframework.web.multipart.MultipartFile;

public class FileNameRequiredValidationRule extends BaseValidationRule<MultipartFile> {

    @Override
    protected void check(MultipartFile file) throws Exception {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new Exception("У файла отсутствует имя");
        }
    }
}
