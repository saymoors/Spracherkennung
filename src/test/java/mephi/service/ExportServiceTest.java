package mephi.service;

import mephi.entity.SemanticBlock;
import mephi.repository.SemanticBlockRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExportServiceTest {

    @Test
    void generatePdfCreatesPdfWithSemanticBlocks() throws Exception {
        SemanticBlockRepository semanticBlockRepository = mock(SemanticBlockRepository.class);
        ExportService exportService = new ExportService(semanticBlockRepository);

        SemanticBlock semanticBlock = new SemanticBlock();
        semanticBlock.setTranscriptionId(1);
        semanticBlock.setOrderIndex(0);
        semanticBlock.setTextContent("Lecture text");

        when(semanticBlockRepository.findByTranscriptionIdOrderByOrderIndexAsc(1))
                .thenReturn(List.of(semanticBlock));

        byte[] pdf = exportService.generatePdf(1);

        assertTrue(new String(pdf, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF"));
        assertTrue(pdf.length > 100);
    }
}
