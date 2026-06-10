package mephi.salutespeech.model;

import java.util.UUID;

public record SberPollTaskResponse(String status, UUID responseFileId, int httpStatus) {
}
