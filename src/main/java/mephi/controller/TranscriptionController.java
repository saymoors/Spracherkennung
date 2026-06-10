package mephi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import mephi.dto.TranscriptionDetails;
import mephi.dto.TranscriptionHistoryResponse;
import mephi.enums.TranscriptionLanguage;
import mephi.service.TranscriptionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/transcriptions")
@Tag(name = "Transcriptions", description = "Распознавание, просмотр, экспорт")
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping("/recognize")
    @Operation(summary = "Распознать файл")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Распознавание начато"),
            @ApiResponse(responseCode = "400", description = "Файл не подходит для распознавания"),
            @ApiResponse(responseCode = "401", description = "Нет токена")
    })
    public ResponseEntity<?> recognize(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "language", defaultValue = TranscriptionLanguage.DEFAULT_CODE) String language) {
        try {
            Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            transcriptionService.recognize(userId, file, language);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Распознавание начато");
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @PostMapping("/recognize-again")
    @Operation(summary = "Распознать существующий файл заново")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Повторное распознавание начато"),
            @ApiResponse(responseCode = "400", description = "Файл еще не существует в системе или не подходит для распознавания"),
            @ApiResponse(responseCode = "401", description = "Нет токена")
    })
    public ResponseEntity<?> recognizeAgain(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "language", defaultValue = TranscriptionLanguage.DEFAULT_CODE) String language) {
        try {
            Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            transcriptionService.recognizeAgain(userId, file, language);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Повторное распознавание начато");
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "История")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ОК"),
            @ApiResponse(responseCode = "400", description = "Некорректные параметры страницы"),
            @ApiResponse(responseCode = "401", description = "Нет токена")
    })
    public ResponseEntity<?> getHistory(@RequestParam int page,
                                        @RequestParam int size) {
        try {
            Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            TranscriptionHistoryResponse response = transcriptionService.getHistory(userId, page, size);
            return ResponseEntity.ok(response);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали и текст")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ОК"),
            @ApiResponse(responseCode = "401", description = "Нет токена"),
            @ApiResponse(responseCode = "404", description = "Детали транскрипции недоступны")
    })
    public ResponseEntity<?> getDetails(@PathVariable Integer id) {
        try {
            Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            TranscriptionDetails details = transcriptionService.getDetails(userId, id);
            return ResponseEntity.ok(details);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
        }
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Скачать конспект")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Файл"),
            @ApiResponse(responseCode = "400", description = "Ошибка экспорта"),
            @ApiResponse(responseCode = "401", description = "Нет токена")
    })
    public ResponseEntity<?> export(@PathVariable Integer id) {
        try {
            Integer userId = (Integer) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Resource file = transcriptionService.export(userId, id);

            String filename = "transcript_" + id + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(file);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
        }
    }
}
