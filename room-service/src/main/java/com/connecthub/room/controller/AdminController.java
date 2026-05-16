package com.connecthub.room.controller;

import com.connecthub.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.connecthub.room.model.RoomType;

@RestController
@RequestMapping("/api/rooms/admin")
@Tag(name = "Admin Rooms", description = "Admin Room management endpoints")
public class AdminController {

    private final RoomService roomService;

    @Autowired
    public AdminController(RoomService roomService) {
        this.roomService = roomService;
    }

    @DeleteMapping("/{roomId}")
    @Operation(summary = "Delete a room (Admin only)")
    public ResponseEntity<Map<String, String>> deleteRoomAsAdmin(@PathVariable UUID roomId) {
        roomService.deleteRoomAsAdmin(roomId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Room deleted successfully by admin");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

@DeleteMapping("/users/{userId}")
    @Operation(summary = "Remove user from all rooms (Admin only)")
    public ResponseEntity<Map<String, String>> removeUserFromAllRooms(@PathVariable UUID userId) {
        roomService.removeUserFromAllRooms(userId);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User removed from all rooms");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getRoomStats() {
        Map<String, Long> response = new HashMap<>();
        response.put("activeRooms",  roomService.getTotalRoomsCount());
        response.put("directRooms",  roomService.countRoomsByType(RoomType.DIRECT));
        response.put("groupRooms",   roomService.countRoomsByType(RoomType.GROUP));
        response.put("channelRooms", roomService.countRoomsByType(RoomType.CHANNEL));
        return ResponseEntity.ok(response);
    }
}
