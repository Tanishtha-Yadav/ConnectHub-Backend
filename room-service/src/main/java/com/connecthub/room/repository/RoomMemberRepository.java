package com.connecthub.room.repository;

import com.connecthub.room.model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for RoomMember join-table.
 */
@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    /** Get all members of a specific room */
    List<RoomMember> findByRoomId(UUID roomId);

    /** Get all rooms a specific user belongs to */
    List<RoomMember> findByUserId(UUID userId);

    /** Find a specific user's membership in a specific room */
    Optional<RoomMember> findByRoomIdAndUserId(UUID roomId, UUID userId);

    /** Check if a user is already a member of a room */
    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    /** Remove a user from a room */
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);

    /** Remove a user from all rooms */
    void deleteByUserId(UUID userId);

    /** Remove all members from a room (used when deleting a room) */
    void deleteAllByRoomId(UUID roomId);

    /** Count members in a room */
    long countByRoomId(UUID roomId);
}
