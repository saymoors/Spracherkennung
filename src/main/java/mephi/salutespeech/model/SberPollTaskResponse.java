package mephi.salutespeech.model;

import mephi.enums.TranscriptionStatus;

import java.util.UUID;

public record SberPollTaskResponse(TranscriptionStatus status, UUID responseFileId, int httpStatus) {
}
