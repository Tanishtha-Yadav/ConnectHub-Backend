package com.connecthub.room.service;

import com.connecthub.room.dto.request.CreateRoomRequest;
import com.connecthub.room.dto.response.RoomResponse;
import com.connecthub.room.exception.RoomNotFoundException;
import com.connecthub.room.model.MemberRole;
import com.connecthub.room.model.Room;
import com.connecthub.room.model.RoomMember;
import com.connecthub.room.model.RoomType;
import com.connecthub.room.repository.RoomMemberRepository;
import com.connecthub.room.repository.RoomRepository;
import com.connecthub.room.service.impl.RoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RoomServiceImpl.
 *
 * WHY @ExtendWith(MockitoExtension.class)?
 *   This tells JUnit 5 to initialize all @Mock and @InjectMocks fields
 *   automatically before each test, without needing MockitoAnnotations.openMocks().
 *
 * WHY mock the repository?
 *   Unit tests should be fast and isolated. We don't want to start a
 *   database. By mocking the repository, we test ONLY the service logic.
 */
@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMemberRepository memberRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private UUID creatorId;
    private UUID roomId;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        roomId = UUID.randomUUID();

        testRoom = new Room("Test Room", RoomType.GROUP, creatorId);
        testRoom.setRoomId(roomId);
        testRoom.setCreatedAt(LocalDateTime.now());
        testRoom.setUpdatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // createRoom() tests
    // =========================================================================

    @Test
    @DisplayName("createRoom — should create a room and add creator as OWNER")
    void createRoom_success() {
        // ARRANGE: set up mock behavior
        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("My Group");
        request.setType(RoomType.GROUP);

        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(memberRepository.save(any(RoomMember.class))).thenReturn(new RoomMember());
        when(memberRepository.countByRoomId(any())).thenReturn(1L);

        // ACT: call the method under test
        RoomResponse response = roomService.createRoom(creatorId, request);

        // ASSERT: verify results
        assertNotNull(response);
        assertEquals("Test Room", response.getName());
        assertEquals(1L, response.getMemberCount());

        // Verify the room was saved
        verify(roomRepository, times(1)).save(any(Room.class));
        // Verify the OWNER membership was created
        verify(memberRepository, times(1)).save(any(RoomMember.class));
    }

    @Test
    @DisplayName("createRoom — should add initial members from the request")
    void createRoom_withMembers() {
        UUID member1 = UUID.randomUUID();
        UUID member2 = UUID.randomUUID();

        CreateRoomRequest request = new CreateRoomRequest();
        request.setName("Team Chat");
        request.setType(RoomType.GROUP);
        request.setMemberIds(List.of(member1, member2));

        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(memberRepository.save(any(RoomMember.class))).thenReturn(new RoomMember());
        when(memberRepository.countByRoomId(any())).thenReturn(3L);

        RoomResponse response = roomService.createRoom(creatorId, request);

        assertEquals(3L, response.getMemberCount());
        // 1 OWNER + 2 MEMBERs = 3 saves
        verify(memberRepository, times(3)).save(any(RoomMember.class));
    }

    // =========================================================================
    // getRoomById() tests
    // =========================================================================

    @Test
    @DisplayName("getRoomById — should return room when it exists")
    void getRoomById_found() {
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.countByRoomId(roomId)).thenReturn(5L);
        when(memberRepository.findByRoomId(roomId)).thenReturn(List.of());

        RoomResponse response = roomService.getRoomById(roomId);

        assertNotNull(response);
        assertEquals(roomId, response.getRoomId());
        assertEquals(5L, response.getMemberCount());
    }

    @Test
    @DisplayName("getRoomById — should throw RoomNotFoundException when not found")
    void getRoomById_notFound() {
        UUID fakeId = UUID.randomUUID();
        when(roomRepository.findById(fakeId)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomService.getRoomById(fakeId));
    }

    // =========================================================================
    // deleteRoom() tests
    // =========================================================================

    @Test
    @DisplayName("deleteRoom — OWNER should be able to delete the room")
    void deleteRoom_asOwner() {
        RoomMember ownerMember = new RoomMember(roomId, creatorId, MemberRole.OWNER);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(roomId, creatorId))
                .thenReturn(Optional.of(ownerMember));

        assertDoesNotThrow(() -> roomService.deleteRoom(roomId, creatorId));

        verify(memberRepository).deleteAllByRoomId(roomId);
        verify(roomRepository).delete(testRoom);
    }

    @Test
    @DisplayName("deleteRoom — non-OWNER should be denied")
    void deleteRoom_asMember() {
        UUID memberId = UUID.randomUUID();
        RoomMember member = new RoomMember(roomId, memberId, MemberRole.MEMBER);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(testRoom));
        when(memberRepository.findByRoomIdAndUserId(roomId, memberId))
                .thenReturn(Optional.of(member));

        assertThrows(Exception.class, () -> roomService.deleteRoom(roomId, memberId));
        verify(roomRepository, never()).delete(any());
    }
}
