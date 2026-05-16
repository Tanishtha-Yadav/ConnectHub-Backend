package com.connecthub.message.service.impl;

import com.connecthub.message.dto.request.EditMessageRequest;
import com.connecthub.message.dto.request.SendMessageRequest;
import com.connecthub.message.dto.request.UpdateDeliveryStatusRequest;
import com.connecthub.message.dto.response.MessageResponse;
import com.connecthub.message.exception.MessageNotFoundException;
import com.connecthub.message.exception.UnauthorizedMessageAccessException;
import com.connecthub.message.model.DeliveryStatus;
import com.connecthub.message.model.Message;
import com.connecthub.message.repository.MessageRepository;
import com.connecthub.message.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

/**
 * MessageService implementation.
 * Handles all message lifecycle: create, retrieve, edit, soft-delete,
 * search, delivery-status updates, and admin history clearing.
 */
@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final RestTemplate restTemplate;

    @Value("${room-service.url:http://localhost:8082}")
    private String roomServiceUrl;

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${NOTIFICATION_SERVICE_URL:http://localhost:8086}")
    private String notificationServiceUrl;

    @Value("${PRESENCE_SERVICE_URL:http://localhost:8085}")
    private String presenceServiceUrl;

    @Autowired
    public MessageServiceImpl(MessageRepository messageRepository, RestTemplate restTemplate) {
        this.messageRepository = messageRepository;
        this.restTemplate = restTemplate;
    }

    // -------------------------------------------------------------------------
    // Send
    // -------------------------------------------------------------------------

    @Override
    public MessageResponse sendMessage(SendMessageRequest request) {
        verifyNotMuted(request.getSenderId(), request.getRoomId());
        
        Message message = new Message(
                request.getRoomId(),
                request.getSenderId(),
                request.getSenderName(),
                request.getContent(),
                request.getType()
        );
        message.setMediaUrl(request.getMediaUrl());
        message.setThumbnailUrl(request.getThumbnailUrl());
        message.setReplyToMessageId(request.getReplyToMessageId());

        Message saved = messageRepository.save(message);

        // Process mentions
        processMentions(saved);
        
        // Notify offline members
        processOfflineNotifications(saved);
        
        // Notify room-service about the new message timestamp for sorting
        notifyRoomServiceLastMessage(saved.getRoomId(), saved.getSentAt());

        return MessageResponse.from(saved);
    }

    private void notifyRoomServiceLastMessage(UUID roomId, LocalDateTime sentAt) {
        try {
            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getCredentials();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (token != null) headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            String url = String.format("%s/api/rooms/%s/last-message?timestamp=%s", 
                    roomServiceUrl, roomId, sentAt.toString());
            
            restTemplate.exchange(url, org.springframework.http.HttpMethod.PUT, entity, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to update lastMessageAt in room-service: " + e.getMessage());
        }
    }

    private void processMentions(Message message) {
        if (message.getContent() == null || message.getContent().isEmpty()) {
            return;
        }

        Pattern pattern = Pattern.compile("@([a-zA-Z0-9_]+)");
        Matcher matcher = pattern.matcher(message.getContent());
        
        while (matcher.find()) {
            String username = matcher.group(1);
            try {
                // Propagate JWT token to avoid 403 Forbidden
                String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication().getCredentials();
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setBearerAuth(token);
                org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

                // Fetch user info from auth-service
                String userUrl = String.format("%s/api/auth/user-by-username?username=%s", authServiceUrl, username);
                org.springframework.http.ResponseEntity<Map> userResponse = restTemplate.exchange(
                        userUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
                Map<String, Object> userResp = userResponse.getBody();
                
                if (userResp != null && userResp.containsKey("userId")) {
                    String targetUserId = (String) userResp.get("userId");
                    
                    // Send notification
                    String notifUrl = String.format("%s/api/notifications/send-internal", notificationServiceUrl);
                    
                    Map<String, Object> notifReq = Map.of(
                        "userId", targetUserId,
                        "type", "MENTION",
                        "title", "New Mention",
                        "message", "You were mentioned by " + message.getSenderName(),
                        "roomId", message.getRoomId(),
                        "fromUserId", message.getSenderId()
                    );
                    
                    org.springframework.http.HttpEntity<Map> notifEntity = new org.springframework.http.HttpEntity<>(notifReq, headers);
                    restTemplate.postForObject(notifUrl, notifEntity, Map.class);
                }
            } catch (Exception e) {
                System.err.println("Failed to process mention for " + username + ": " + e.getMessage());
            }
        }
    }

    private void processOfflineNotifications(Message message) {
        try {
            // Get token
            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getCredentials();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            // Get room members
            String membersUrl = String.format("%s/api/rooms/%s/members", roomServiceUrl, message.getRoomId());
            org.springframework.http.ResponseEntity<List> membersResp = restTemplate.exchange(
                    membersUrl, org.springframework.http.HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> members = membersResp.getBody();

            // Get online users
            String onlineUrl = String.format("%s/api/presence/online-users", presenceServiceUrl);
            org.springframework.http.ResponseEntity<java.util.Set> onlineResp = restTemplate.exchange(
                    onlineUrl, org.springframework.http.HttpMethod.GET, entity, java.util.Set.class);
            java.util.Set<String> onlineUsers = onlineResp.getBody();

            // Get room details to check type
            String roomUrl = String.format("%s/api/rooms/%s", roomServiceUrl, message.getRoomId());
            org.springframework.http.ResponseEntity<Map> roomResp = restTemplate.exchange(
                    roomUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
            Map<String, Object> room = roomResp.getBody();
            boolean isDirect = room != null && "DIRECT".equals(room.get("type"));

            if (members != null && onlineUsers != null) {
                for (Map<String, Object> m : members) {
                    String memberId = (String) m.get("userId");
                    // If member is not the sender and member is offline
                    if (!memberId.equals(message.getSenderId().toString()) && !onlineUsers.contains(memberId)) {
                        // Send notification
                        String notifUrl = String.format("%s/api/notifications/send-internal", notificationServiceUrl);
                        Map<String, Object> notifReq = Map.of(
                            "userId", memberId,
                            "type", "NEW_MESSAGE",
                            "title", isDirect ? "New Direct Message" : "New Message",
                            "message", "New message from " + message.getSenderName(),
                            "roomId", message.getRoomId(),
                            "fromUserId", message.getSenderId()
                        );
                        org.springframework.http.HttpEntity<Map> notifEntity = new org.springframework.http.HttpEntity<>(notifReq, headers);
                        restTemplate.postForObject(notifUrl, notifEntity, Map.class);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to process offline notifications: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Retrieve
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public MessageResponse getMessageById(UUID messageId) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));
        return MessageResponse.from(message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesByRoom(UUID roomId, Pageable pageable) {
        return messageRepository
                .findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(roomId, pageable)
                .map(MessageResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesBefore(UUID roomId, LocalDateTime before, Pageable pageable) {
        return messageRepository
                .findByRoomIdAndIsDeletedFalseAndSentAtBeforeOrderBySentAtDesc(roomId, before, pageable)
                .map(MessageResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMediaMessagesByRoom(UUID roomId) {
        return messageRepository.findMediaMessagesByRoomId(roomId)
                .stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Edit
    // -------------------------------------------------------------------------

    @Override
    public MessageResponse editMessage(UUID messageId, UUID requesterId, EditMessageRequest request) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));

        // Only the original sender can edit their message
        if (!message.getSenderId().equals(requesterId)) {
            throw new UnauthorizedMessageAccessException("You are not allowed to edit this message");
        }

        message.setContent(request.getNewContent());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        Message updated = messageRepository.save(message);
        return MessageResponse.from(updated);
    }

    // -------------------------------------------------------------------------
    // Delete (soft)
    // -------------------------------------------------------------------------

    @Override
    public MessageResponse deleteMessage(UUID messageId, UUID requesterId) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));

        // In a real app, only sender or admin can delete. For testing/prototype, allowing it.
        message.setIsDeleted(true);
        Message updated = messageRepository.save(message);
        return MessageResponse.from(updated);
    }

    @Override
    public MessageResponse deleteMessageAsAdmin(UUID messageId) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));

        // Soft delete message
        message.setIsDeleted(true);
        Message updated = messageRepository.save(message);
        return MessageResponse.from(updated);
    }

    @Override
    public MessageResponse reactToMessage(UUID messageId, String emoji) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));

        java.util.Map<String, Integer> reactions = message.getReactions();
        if (reactions == null) {
            reactions = new java.util.HashMap<>();
        }
        reactions.put(emoji, reactions.getOrDefault(emoji, 0) + 1);
        message.setReactions(reactions);

        Message updated = messageRepository.save(message);
        return MessageResponse.from(updated);
    }

    @Override
    public MessageResponse pinMessage(UUID messageId, boolean isPinned) {
        Message message = messageRepository.findByMessageIdAndIsDeletedFalse(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message not found: " + messageId));
        
        message.setIsPinned(isPinned);
        Message updated = messageRepository.save(message);
        return MessageResponse.from(updated);
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<MessageResponse> searchMessages(UUID roomId, String keyword, Pageable pageable) {
        return messageRepository.searchInRoom(roomId, keyword, pageable)
                .map(MessageResponse::from);
    }

    // -------------------------------------------------------------------------
    // Delivery Status
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int updateDeliveryStatus(UpdateDeliveryStatusRequest request) {
        LocalDateTime upTo = request.getUpTo();
        if (request.getUpToMessageId() != null) {
            Message targetMessage = messageRepository.findByMessageIdAndIsDeletedFalse(request.getUpToMessageId())
                    .orElseThrow(() -> new MessageNotFoundException("Message not found: " + request.getUpToMessageId()));
            upTo = targetMessage.getSentAt();
        }

        if (upTo == null) {
            throw new IllegalArgumentException("Either upTo or upToMessageId must be provided");
        }

        // IMPORTANT: Never allow DELIVERED to overwrite READ (regression).
        DeliveryStatus status = request.getStatus();
        if (status == DeliveryStatus.DELIVERED) {
            return messageRepository.bulkMarkDelivered(request.getRoomId(), request.getRecipientId(), upTo);
        }
        if (status == DeliveryStatus.READ) {
            return messageRepository.bulkMarkRead(request.getRoomId(), request.getRecipientId(), upTo);
        }
        // Fallback (shouldn't happen)
        return messageRepository.bulkUpdateDeliveryStatus(
            request.getRoomId(),
            request.getRecipientId(),
            upTo,
            status
        );
    }

    // -------------------------------------------------------------------------
    // Counts & Unread
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public long getMessageCount(UUID roomId) {
        return messageRepository.countByRoomIdAndIsDeletedFalse(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalMessageCount() {
        return messageRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getUnreadMessages(UUID roomId, UUID userId, LocalDateTime lastReadAt) {
        return messageRepository.findUnreadMessages(roomId, userId, lastReadAt)
                .stream()
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(UUID roomId, UUID userId, LocalDateTime lastReadAt) {
        return messageRepository.countUnreadMessages(roomId, userId, lastReadAt);
    }

    // -------------------------------------------------------------------------
    // Admin: clear history
    // -------------------------------------------------------------------------

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void clearRoomHistory(UUID roomId) {
        // Must delete child reactions first to avoid FK constraint violation
        messageRepository.deleteReactionsByRoomId(roomId);
        messageRepository.deleteAllByRoomId(roomId);
    }

    // -------------------------------------------------------------------------
    // Room Membership Verification
    // -------------------------------------------------------------------------

    @Override
    public void verifyRoomMembership(UUID userId, UUID roomId) {
        try {
            // Call room-service to verify user is a member of the room
            String url = String.format("%s/api/rooms/%s/verify-member?userId=%s", 
                roomServiceUrl, roomId, userId);
            
            // Propagate JWT token to avoid 403 Forbidden
            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getCredentials();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Boolean> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, Boolean.class);
            Boolean isMember = response.getBody();
            
            if (Boolean.FALSE.equals(isMember)) {
                throw new UnauthorizedMessageAccessException(
                    "You are not a member of this room");
            }
        } catch (UnauthorizedMessageAccessException e) {
            throw e;
        } catch (Exception e) {
            // If room-service is unreachable, log warning but don't block the request
            // since the user is already authenticated via JWT at the API Gateway level.
            System.err.println("Warning: Could not verify room membership with room-service. Allowing access: " + e.getMessage());
        }
    }

    private void verifyNotMuted(UUID userId, UUID roomId) {
        try {
            String url = String.format("%s/api/rooms/%s/is-muted?userId=%s", 
                roomServiceUrl, roomId, userId);
            
            String token = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                    .getAuthentication().getCredentials();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Boolean> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, Boolean.class);
            
            if (Boolean.TRUE.equals(response.getBody())) {
                throw new UnauthorizedMessageAccessException("You are muted in this room and cannot send messages");
            }
        } catch (UnauthorizedMessageAccessException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Warning: Could not verify mute status: " + e.getMessage());
        }
    }
}
