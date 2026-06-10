package mephi.validation.file;

import mephi.validation.BaseValidationRule;
import org.springframework.web.multipart.MultipartFile;

public class FileSizeLimitValidationRule extends BaseValidationRule<MultipartFile> {
    private static final long MAX_FILE_SIZE_BYTES = 250L * 1024 * 1024;

    @Override
    protected void check(MultipartFile file) throws Exception {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new Exception("Файл слишком большой. Максимальный размер 250 МБ");
        }
    }
}
