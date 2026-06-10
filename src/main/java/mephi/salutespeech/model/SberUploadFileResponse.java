package mephi.salutespeech.model;

import java.util.UUID;

public record SberUploadFileResponse(UUID requestFileId, int httpStatus) {
}
