package mephi.dto;

import java.util.List;

public class TranscriptionHistoryResponse {
    private List<TranscriptionHistoryItem> content;
    private Boolean hasMore;

    public List<TranscriptionHistoryItem> getContent() {
        return content;
    }

    public void setContent(List<TranscriptionHistoryItem> content) {
        this.content = content;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}
