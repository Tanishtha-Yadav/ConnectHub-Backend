package com.connecthub.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Standard API Response wrapper for all endpoints
 * Provides consistent response format across all APIs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API Response")
public class ApiResponse<T> {

    @Schema(description = "Response status code", example = "200")
    private int statusCode;

    @Schema(description = "Response message", example = "Success")
    private String message;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Error details (if any)")
    private String error;

    @Schema(description = "Timestamp of the response")
    private LocalDateTime timestamp;

    @Schema(description = "Request path")
    private String path;

    /**
     * Success response builder
     */
    public static <T> ApiResponse<T> success(int statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Error response builder
     */
    public static <T> ApiResponse<T> error(int statusCode, String message, String error) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Success response with default status 200
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return success(200, message, data);
    }

    /**
     * Created response with status 201
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return success(201, message, data);
    }

    /**
     * Bad request response with status 400
     */
    public static <T> ApiResponse<T> badRequest(String message, String error) {
        return error(400, message, error);
    }

    /**
     * Unauthorized response with status 401
     */
    public static <T> ApiResponse<T> unauthorized(String message, String error) {
        return error(401, message, error);
    }

    /**
     * Forbidden response with status 403
     */
    public static <T> ApiResponse<T> forbidden(String message, String error) {
        return error(403, message, error);
    }

    /**
     * Not found response with status 404
     */
    public static <T> ApiResponse<T> notFound(String message, String error) {
        return error(404, message, error);
    }

    /**
     * Internal server error response with status 500
     */
    public static <T> ApiResponse<T> internalServerError(String message, String error) {
        return error(500, message, error);
    }
}
