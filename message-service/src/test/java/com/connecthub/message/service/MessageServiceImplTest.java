package com.connecthub.message.service;

import com.connecthub.message.dto.request.EditMessageRequest;
import com.connecthub.message.dto.request.SendMessageRequest;
import com.connecthub.message.dto.response.MessageResponse;
import com.connecthub.message.exception.MessageNotFoundException;
import com.connecthub.message.exception.UnauthorizedMessageAccessException;
import com.connecthub.message.model.Message;
import com.connecthub.message.model.MessageType;
import com.connecthub.message.repository.MessageRepository;
import com.connecthub.message.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessageServiceImpl.
 * Tests message CRUD operations with mocked repository.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageServiceImpl messageService;

    private UUID roomId;
    private UUID senderId;
    private UUID messageId;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        messageId = UUID.randomUUID();

        testMessage = new Message(roomId, senderId, "Test User", "Hello World", MessageType.TEXT);
        testMessage.setMessageId(messageId);
    }

    @Test
    @DisplayName("sendMessage — should save and return the message")
    void sendMessage_success() {
        SendMessageRequest request = new SendMessageRequest();
        request.setRoomId(roomId);
        request.setSenderId(senderId);
        request.setContent("Hello World");
        request.setType(MessageType.TEXT);

        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        MessageResponse response = messageService.sendMessage(request);

        assertNotNull(response);
        assertEquals("Hello World", response.getContent());
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("getMessageById — should return message when it exists")
    void getMessageById_found() {
        when(messageRepository.findByMessageIdAndIsDeletedFalse(messageId))
                .thenReturn(Optional.of(testMessage));

        MessageResponse response = messageService.getMessageById(messageId);

        assertNotNull(response);
        assertEquals(messageId, response.getMessageId());
    }

    @Test
    @DisplayName("getMessageById — should throw when message not found")
    void getMessageById_notFound() {
        UUID fakeId = UUID.randomUUID();
        when(messageRepository.findByMessageIdAndIsDeletedFalse(fakeId))
                .thenReturn(Optional.empty());

        assertThrows(MessageNotFoundException.class,
                () -> messageService.getMessageById(fakeId));
    }

    @Test
    @DisplayName("editMessage — sender should be able to edit their message")
    void editMessage_asSender() {
        EditMessageRequest request = new EditMessageRequest();
        request.setNewContent("Updated content");

        when(messageRepository.findByMessageIdAndIsDeletedFalse(messageId))
                .thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        MessageResponse response = messageService.editMessage(messageId, senderId, request);

        assertNotNull(response);
        assertTrue(testMessage.getIsEdited());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("editMessage — non-sender should be denied")
    void editMessage_asOtherUser() {
        UUID otherUser = UUID.randomUUID();
        EditMessageRequest request = new EditMessageRequest();
        request.setNewContent("Hacked!");

        when(messageRepository.findByMessageIdAndIsDeletedFalse(messageId))
                .thenReturn(Optional.of(testMessage));

        assertThrows(UnauthorizedMessageAccessException.class,
                () -> messageService.editMessage(messageId, otherUser, request));
    }

    @Test
    @DisplayName("deleteMessage — sender should be able to soft-delete")
    void deleteMessage_asSender() {
        when(messageRepository.findByMessageIdAndIsDeletedFalse(messageId))
                .thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        MessageResponse response = messageService.deleteMessage(messageId, senderId);

        assertTrue(testMessage.getIsDeleted());
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    @DisplayName("deleteMessage — non-sender should be denied")
    void deleteMessage_asOtherUser() {
        UUID otherUser = UUID.randomUUID();
        when(messageRepository.findByMessageIdAndIsDeletedFalse(messageId))
                .thenReturn(Optional.of(testMessage));

        assertThrows(UnauthorizedMessageAccessException.class,
                () -> messageService.deleteMessage(messageId, otherUser));
    }
}
