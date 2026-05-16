package com.connecthub.room.repository;

import com.connecthub.room.model.Room;
import com.connecthub.room.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for Room entities.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    /** Find all rooms created by a specific user */
    List<Room> findByCreatedBy(UUID createdBy);

    /** Find rooms by type (GROUP, DIRECT, CHANNEL) */
    List<Room> findByType(RoomType type);

    /**
     * Find all rooms that a given user is a member of.
     * Uses a subquery on room_members to get the room IDs.
     */
    @Query("SELECT r FROM Room r WHERE r.roomId IN " +
           "(SELECT rm.roomId FROM RoomMember rm WHERE rm.userId = :userId) " +
           "ORDER BY COALESCE(r.lastMessageAt, r.updatedAt) DESC")
    List<Room> findRoomsByUserId(@Param("userId") UUID userId);

    /**
     * Find a DIRECT room between exactly two users.
     * Returns the room where both users are members AND the type is DIRECT.
     */
    @Query("SELECT r FROM Room r WHERE r.type = 'DIRECT' " +
           "AND r.roomId IN (SELECT rm.roomId FROM RoomMember rm WHERE rm.userId = :userId1) " +
           "AND r.roomId IN (SELECT rm.roomId FROM RoomMember rm WHERE rm.userId = :userId2)")
    List<Room> findDirectRoomBetweenUsers(@Param("userId1") UUID userId1,
                                          @Param("userId2") UUID userId2);

    /** Find a room by its invite token */
    java.util.Optional<Room> findByInviteToken(String inviteToken);

    /** Count rooms by type (DIRECT, GROUP, CHANNEL) */
    long countByType(RoomType type);
}
