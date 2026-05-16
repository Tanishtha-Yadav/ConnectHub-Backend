package com.connecthub.auth.dto.request;

import com.connecthub.auth.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private UserStatus status;

    // Constructors
    public UpdateStatusRequest() {
    }

    public UpdateStatusRequest(UserStatus status) {
        this.status = status;
    }

    // Getters and Setters
    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }
}
