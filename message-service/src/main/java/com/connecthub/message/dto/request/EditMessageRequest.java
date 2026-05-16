package com.connecthub.message.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for editing an existing message's content.
 */
public class EditMessageRequest {

    @NotBlank(message = "New content cannot be blank")
    private String newContent;

    public String getNewContent()              { return newContent; }
    public void setNewContent(String content)  { this.newContent = content; }
}
