package com.connecthub.message.repository;

import com.connecthub.message.model.DeliveryStatus;
import com.connecthub.message.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for Message entities.
 * Provides custom queries for pagination, search, delivery status updates,
 * and unread message retrieval as specified in the ConnectHub case study.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find all non-deleted messages in a room, newest first — for infinite scroll pagination.
     */
    Page<Message> findByRoomIdAndIsDeletedFalseOrderBySentAtDesc(UUID roomId, Pageable pageable);

    /**
     * Find all non-deleted messages in a room sent before a given timestamp — for "load more" pagination.
     */
    Page<Message> findByRoomIdAndIsDeletedFalseAndSentAtBeforeOrderBySentAtDesc(
            UUID roomId, LocalDateTime before, Pageable pageable);

    /**
     * Find a message by its ID, only if not soft-deleted.
     */
    Optional<Message> findByMessageIdAndIsDeletedFalse(UUID messageId);

    /**
     * Find all non-deleted messages sent by a specific user.
     */
    List<Message> findBySenderIdAndIsDeletedFalse(UUID senderId);

    /**
     * Find all non-deleted media messages in a room.
     */
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId " +
           "AND m.isDeleted = false " +
           "AND m.mediaUrl IS NOT NULL " +
           "ORDER BY m.sentAt DESC")
    List<Message> findMediaMessagesByRoomId(@Param("roomId") UUID roomId);

    /**
     * Full-text keyword search within a room (case-insensitive LIKE search).
     */
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId " +
           "AND m.isDeleted = false " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.sentAt DESC")
    Page<Message> searchInRoom(@Param("roomId") UUID roomId,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    /**
     * Count non-deleted messages in a room.
     */
    long countByRoomIdAndIsDeletedFalse(UUID roomId);

    /**
     * Find unread messages for a room sent after a given timestamp — used for unread count.
     */
    @Query("SELECT m FROM Message m WHERE m.roomId = :roomId " +
           "AND m.isDeleted = false " +
           "AND m.sentAt > :lastReadAt " +
           "AND m.senderId != :userId " +
           "ORDER BY m.sentAt ASC")
    List<Message> findUnreadMessages(@Param("roomId") UUID roomId,
                                     @Param("userId") UUID userId,
                                     @Param("lastReadAt") LocalDateTime lastReadAt);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.roomId = :roomId " +
           "AND m.isDeleted = false " +
           "AND m.sentAt > :lastReadAt " +
           "AND m.senderId != :userId")
    long countUnreadMessages(@Param("roomId") UUID roomId,
                             @Param("userId") UUID userId,
                             @Param("lastReadAt") LocalDateTime lastReadAt);

    /**
     * Bulk update delivery status for all messages in a room sent up to a timestamp.
     * NOTE: This generic method can downgrade statuses (e.g. READ -> DELIVERED),
     * so prefer the stage-specific methods below.
     */
    @Modifying
    @Query("UPDATE Message m SET m.deliveryStatus = :status " +
           "WHERE m.roomId = :roomId " +
           "AND m.senderId != :recipientId " +
           "AND m.sentAt <= :upTo " +
           "AND m.deliveryStatus <> :status")
    int bulkUpdateDeliveryStatus(@Param("roomId") UUID roomId,
                                  @Param("recipientId") UUID recipientId,
                                  @Param("upTo") LocalDateTime upTo,
                                  @Param("status") DeliveryStatus status);

    /**
     * Stage-safe update to DELIVERED: only upgrade from SENT -> DELIVERED.
     * Never downgrade READ messages.
     */
    @Modifying
    @Query("UPDATE Message m SET m.deliveryStatus = com.connecthub.message.model.DeliveryStatus.DELIVERED " +
           "WHERE m.roomId = :roomId " +
           "AND m.senderId != :recipientId " +
           "AND m.sentAt <= :upTo " +
           "AND m.deliveryStatus = com.connecthub.message.model.DeliveryStatus.SENT")
    int bulkMarkDelivered(@Param("roomId") UUID roomId,
                          @Param("recipientId") UUID recipientId,
                          @Param("upTo") LocalDateTime upTo);

    /**
     * Stage-safe update to READ: upgrade SENT/DELIVERED -> READ.
     */
    @Modifying
    @Query("UPDATE Message m SET m.deliveryStatus = com.connecthub.message.model.DeliveryStatus.READ " +
           "WHERE m.roomId = :roomId " +
           "AND m.senderId != :recipientId " +
           "AND m.sentAt <= :upTo " +
           "AND m.deliveryStatus <> com.connecthub.message.model.DeliveryStatus.READ")
    int bulkMarkRead(@Param("roomId") UUID roomId,
                     @Param("recipientId") UUID recipientId,
                     @Param("upTo") LocalDateTime upTo);

    /**
     * Find the most recent message in a room (for room list preview).
     */
    Optional<Message> findTopByRoomIdAndIsDeletedFalseOrderBySentAtDesc(UUID roomId);

    /**
     * Delete all messages in a room permanently (admin: clear room history).
     * First delete child reactions, then the messages.
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.roomId = :roomId")
    void deleteAllByRoomId(@Param("roomId") UUID roomId);

    /**
     * Delete all reactions for messages in a given room (native query to handle
     * the element collection table before bulk-deleting messages).
     */
    @Modifying
    @Query(value = "DELETE mr FROM message_reactions mr " +
                   "INNER JOIN messages m ON mr.message_id = m.message_id " +
                   "WHERE m.room_id = :roomId", nativeQuery = true)
    void deleteReactionsByRoomId(@Param("roomId") UUID roomId);
}
