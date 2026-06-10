package mephi.service;

import mephi.entity.ExternalCallLog;
import mephi.repository.ExternalCallLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
public class ExternalCallLogService {
    private final ExternalCallLogRepository externalCallLogRepository;

    public ExternalCallLogService(ExternalCallLogRepository externalCallLogRepository) {
        this.externalCallLogRepository = externalCallLogRepository;
    }

    public void save(Integer transcriptionId, String operationType, String httpMethod, int httpStatus, String message) {
        try {
            ExternalCallLog log = new ExternalCallLog();
            log.setTranscriptionId(transcriptionId);
            log.setOperationType(operationType);
            log.setHttpMethod(httpMethod);
            log.setHttpStatus(httpStatus);
            log.setMessage(message != null ? message.substring(0, Math.min(message.length(), 1000)) : null);
            externalCallLogRepository.save(log);
        } catch (Exception exception) {
            System.err.println("Не удалось сохранить лог: " + exception.getMessage());
        }
    }

    public int statusFromException(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            return responseException.getStatusCode().value();
        }
        return 500;
    }

    public String messageFromException(Exception exception) {
        if (exception instanceof RestClientResponseException responseException) {
            String responseBody = responseException.getResponseBodyAsString();
            if (!responseBody.isBlank()) {
                return exception.getMessage() + "; body=" + responseBody;
            }
        }
        return exception.getMessage();
    }
}
