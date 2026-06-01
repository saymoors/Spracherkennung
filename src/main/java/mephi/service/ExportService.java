package mephi.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import mephi.entity.SemanticBlock;
import mephi.repository.SemanticBlockRepository;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class ExportService {
    private static final String GOTHIC_FONT_PATH = "/fonts/RuslanDisplay-Regular.ttf";

    private final SemanticBlockRepository semanticBlockRepository;

    public ExportService(SemanticBlockRepository semanticBlockRepository) {
        this.semanticBlockRepository = semanticBlockRepository;
    }

    public byte[] generatePdf(Integer transcriptionId) throws Exception {
        List<SemanticBlock> blocks = semanticBlockRepository.findByTranscriptionIdOrderByOrderIndexAsc(transcriptionId);

        if (blocks.isEmpty()) {
            throw new Exception("Нет данных для генерации PDF");
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);
            PdfFont gothicFont = createGothicFont();

            document.setFont(gothicFont);
            document.add(new Paragraph("Аудиорасшифровка").setFontSize(16).setFontColor(new DeviceRgb(120, 23, 23)));

            for (SemanticBlock block : blocks) {
                if (block.getTextContent() != null && !block.getTextContent().isEmpty()) {
                    document.add(new Paragraph(block.getTextContent()).setFontSize(14).setFontColor(new DeviceRgb(120, 23, 23)));
                }
            }

            document.close();
            return out.toByteArray();
        } catch (Exception exception) {
            throw new Exception("Ошибка генерации PDF: " + exception.getMessage());
        }
    }

    private PdfFont createGothicFont() throws Exception {
        try (InputStream fontStream = getClass().getResourceAsStream(GOTHIC_FONT_PATH)) {
            if (fontStream == null) {
                throw new Exception("Шрифт для PDF не найден");
            }
            return PdfFontFactory.createFont(
                    fontStream.readAllBytes(),
                    PdfEncodings.IDENTITY_H,
                    EmbeddingStrategy.PREFER_EMBEDDED
            );
        }
    }
}
