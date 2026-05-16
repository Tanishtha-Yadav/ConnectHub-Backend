package com.connecthub.message.controller;

import com.connecthub.message.dto.request.EditMessageRequest;
import com.connecthub.message.dto.request.SendMessageRequest;
import com.connecthub.message.dto.response.MessageResponse;
import com.connecthub.message.exception.GlobalExceptionHandler;
import com.connecthub.message.exception.MessageNotFoundException;
import com.connecthub.message.exception.UnauthorizedMessageAccessException;
import com.connecthub.message.model.DeliveryStatus;
import com.connecthub.message.model.MessageType;
import com.connecthub.message.service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc integration tests for MessageResource.
 * Tests HTTP layer: routing, JSON serialisation, and status codes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageResource MockMvc Tests")
class MessageResourceTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageResource messageResource;

    private UUID roomId;
    private UUID senderId;
    private UUID messageId;
    private MessageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(messageResource)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        roomId      = UUID.randomUUID();
        senderId    = UUID.randomUUID();
        messageId   = UUID.randomUUID();

        sampleResponse = new MessageResponse();
        sampleResponse.setMessageId(messageId);
        sampleResponse.setRoomId(roomId);
        sampleResponse.setSenderId(senderId);
        sampleResponse.setContent("Hello ConnectHub!");
        sampleResponse.setType(MessageType.TEXT);
        sampleResponse.setDeliveryStatus(DeliveryStatus.SENT);
        sampleResponse.setIsEdited(false);
        sampleResponse.setIsDeleted(false);
        sampleResponse.setSentAt(LocalDateTime.now());
    }

    // =========================================================================
    // POST /api/messages/send
    // =========================================================================

    @Test
    @DisplayName("POST /api/messages/send — returns 201 on success")
    void sendMessage_Returns201() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setRoomId(roomId);
        req.setSenderId(senderId);
        req.setContent("Hello ConnectHub!");
        req.setType(MessageType.TEXT);

        when(messageService.sendMessage(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()))
                .andExpect(jsonPath("$.content").value("Hello ConnectHub!"))
                .andExpect(jsonPath("$.type").value("TEXT"))
                .andExpect(jsonPath("$.deliveryStatus").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/messages/send — returns 400 when roomId missing")
    void sendMessage_Returns400WhenRoomIdMissing() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setSenderId(senderId);
        req.setContent("test");
        // roomId intentionally omitted

        mockMvc.perform(post("/api/messages/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /api/messages/{messageId}
    // =========================================================================

    @Test
    @DisplayName("GET /api/messages/{id} — returns 200 when found")
    void getMessageById_Returns200() throws Exception {
        when(messageService.getMessageById(messageId)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/messages/" + messageId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageId").value(messageId.toString()));
    }

    @Test
    @DisplayName("GET /api/messages/{id} — returns 404 when not found")
    void getMessageById_Returns404() throws Exception {
        when(messageService.getMessageById(messageId))
                .thenThrow(new MessageNotFoundException("Message not found: " + messageId));

        mockMvc.perform(get("/api/messages/" + messageId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    // =========================================================================
    // GET /api/messages/room/{roomId}
    // =========================================================================

    @Test
    @DisplayName("GET /api/messages/room/{roomId} — returns 200 with page")
    void getMessagesByRoom_Returns200() throws Exception {
        Page<MessageResponse> page = new PageImpl<>(List.of(sampleResponse),
                PageRequest.of(0, 20), 1);
        when(messageService.getMessagesByRoom(any(UUID.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/messages/room/" + roomId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].messageId").value(messageId.toString()));
    }

    // =========================================================================
    // PUT /api/messages/{messageId}/edit
    // =========================================================================

    @Test
    @DisplayName("PUT /api/messages/{id}/edit — returns 200 on success")
    void editMessage_Returns200() throws Exception {
        EditMessageRequest editReq = new EditMessageRequest();
        editReq.setNewContent("Updated content");

        sampleResponse.setContent("Updated content");
        sampleResponse.setIsEdited(true);

        when(messageService.editMessage(eq(messageId), eq(senderId), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(put("/api/messages/" + messageId + "/edit")
                        .param("requesterId", senderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isEdited").value(true));
    }

    @Test
    @DisplayName("PUT /api/messages/{id}/edit — returns 403 when not owner")
    void editMessage_Returns403WhenNotOwner() throws Exception {
        UUID otherUser = UUID.randomUUID();
        EditMessageRequest editReq = new EditMessageRequest();
        editReq.setNewContent("hacked");

        when(messageService.editMessage(eq(messageId), eq(otherUser), any()))
                .thenThrow(new UnauthorizedMessageAccessException("Not allowed"));

        mockMvc.perform(put("/api/messages/" + messageId + "/edit")
                        .param("requesterId", otherUser.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editReq)))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // DELETE /api/messages/{messageId}
    // =========================================================================

    @Test
    @DisplayName("DELETE /api/messages/{id} — returns 200 on soft delete")
    void deleteMessage_Returns200() throws Exception {
        sampleResponse.setIsDeleted(true);
        when(messageService.deleteMessage(eq(messageId), eq(senderId)))
                .thenReturn(sampleResponse);

        mockMvc.perform(delete("/api/messages/" + messageId)
                        .param("requesterId", senderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDeleted").value(true));
    }

    // =========================================================================
    // GET /api/messages/room/{roomId}/search
    // =========================================================================

    @Test
    @DisplayName("GET /api/messages/room/{roomId}/search — returns matching results")
    void searchMessages_Returns200() throws Exception {
        Page<MessageResponse> page = new PageImpl<>(List.of(sampleResponse),
                PageRequest.of(0, 20), 1);
        when(messageService.searchMessages(any(UUID.class), eq("Hello"), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/messages/room/" + roomId + "/search")
                        .param("keyword", "Hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].content").value("Hello ConnectHub!"));
    }

    // =========================================================================
    // GET /api/messages/room/{roomId}/count
    // =========================================================================

    @Test
    @DisplayName("GET /api/messages/room/{roomId}/count — returns count")
    void getMessageCount_Returns200() throws Exception {
        when(messageService.getMessageCount(roomId)).thenReturn(42L);

        mockMvc.perform(get("/api/messages/room/" + roomId + "/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(42));
    }

    // =========================================================================
    // GET /api/messages/health
    // =========================================================================

    @Test
    @DisplayName("GET /api/messages/health — returns status OK")
    void healthCheck_Returns200() throws Exception {
        mockMvc.perform(get("/api/messages/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Message Service is running"));
    }
}
