package mephi.dto;

import java.util.List;

public class TranscriptionHistoryResponse {
    private List<TranscriptionListItem> content;
    private Integer page;
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;

    public List<TranscriptionListItem> getContent() {
        return content;
    }

    public void setContent(List<TranscriptionListItem> content) {
        this.content = content;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(Integer totalElements) {
        this.totalElements = totalElements;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }
}
