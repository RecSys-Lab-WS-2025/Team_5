package de.tum.moodtrip_backend.adapter_user.dto;

import jakarta.validation.constraints.NotBlank;

public class MessageContentRequest {
    @NotBlank(message = "Content cannot be blank")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
