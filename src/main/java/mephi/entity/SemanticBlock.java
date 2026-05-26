package mephi.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "semantic_blocks")
public class SemanticBlock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transcription_id", nullable = false)
    private Integer transcriptionId;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "text_content", nullable = false)
    private String textContent;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTranscriptionId() {
        return transcriptionId;
    }

    public void setTranscriptionId(Integer transcriptionId) {
        this.transcriptionId = transcriptionId;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }
}
