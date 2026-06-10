package mephi.scheduler;

import mephi.entity.Transcription;
import mephi.enums.TranscriptionStatus;
import mephi.repository.TranscriptionRepository;
import mephi.service.SaluteSpeechService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TranscriptionScheduler {

    private final TranscriptionRepository transcriptionRepository;
    private final SaluteSpeechService saluteSpeechService;

    public TranscriptionScheduler(TranscriptionRepository transcriptionRepository,
                                  SaluteSpeechService saluteSpeechService) {
        this.transcriptionRepository = transcriptionRepository;
        this.saluteSpeechService = saluteSpeechService;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollRunningSberTasks() {
        List<Transcription> runningTasks = transcriptionRepository.findByStatus(TranscriptionStatus.RUNNING);
        for (Transcription task : runningTasks) {
            saluteSpeechService.pollSberTask(task.getId());
        }
    }
}
