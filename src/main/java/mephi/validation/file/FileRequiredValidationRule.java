package mephi.validation.file;

import mephi.validation.BaseValidationRule;
import org.springframework.web.multipart.MultipartFile;

public class FileRequiredValidationRule extends BaseValidationRule<MultipartFile> {

    @Override
    protected void check(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new Exception("Файл не выбран");
        }
    }
}
