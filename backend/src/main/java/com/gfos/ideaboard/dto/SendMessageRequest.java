package com.gfos.ideaboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {

    @NotNull(message = "Empfänger-ID ist erforderlich")
    private Long recipientId;

    @NotBlank(message = "Nachrichteninhalt ist erforderlich")
    @Size(min = 1, max = 2000, message = "Nachricht muss zwischen 1 und 2000 Zeichen lang sein")
    private String content;

    private Long ideaId;

    public SendMessageRequest() {}

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getIdeaId() {
        return ideaId;
    }

    public void setIdeaId(Long ideaId) {
        this.ideaId = ideaId;
    }
}
