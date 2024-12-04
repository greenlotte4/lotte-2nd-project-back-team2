package com.backend.controller;

import com.backend.document.chat.ChatMemberDocument;
import com.backend.document.chat.ChatRoomDocument;
import com.backend.dto.chat.ChatRoomDTO;
import com.backend.entity.user.User;
import com.backend.service.ChatService;
import com.backend.service.GroupService;
import com.backend.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;


@Log4j2
@RequestMapping("/api/message")
@CrossOrigin(origins = "http://localhost:8010")
@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final GroupService groupService;

    @GetMapping("/room/{userUid}") // 유저 uid로 해당 유저가 속한 모든 채팅방 조회
    public ResponseEntity<?> getAllChatRooms(@PathVariable String userUid) {
        log.info("userUid = " + userUid);

        List<ChatRoomDTO> chatRoomDTOS = chatService.getAllChatRoomsByUserId(userUid);

        if (chatRoomDTOS.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(chatRoomDTOS);
        }
    }

    @GetMapping("/roomInfo/{roomId}") // 채팅방 id로 해당 채팅방의 정보 조회
    public ResponseEntity<?> getChatRoomInfo(@PathVariable String roomId) {
        log.info("roomId = " + roomId);
        ChatRoomDTO chatRoomDTO = chatService.getChatRoomInfo(roomId);
        if (chatRoomDTO == null) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(chatRoomDTO);
        }
    }

    @Transactional
    @PostMapping("/room")
    public ResponseEntity<?> createRoom(
            @ModelAttribute ChatRoomDTO chatRoomDTO) {

        log.info("chatRoomDTO : " + chatRoomDTO);

        String roomId = chatService.createChatRoom(chatRoomDTO);
        log.info("roomId = " + roomId);

        User user = userService.getUserByuid(chatRoomDTO.getLeader());
        log.info("user = " + user);

        String groupName = groupService.findGroupNameByUser(user);
        log.info("groupName = " + groupName);

        ChatMemberDocument savedDocument = chatService.saveChatMember(roomId, user, groupName);

        if (savedDocument != null) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.ok("error");
        }
    }

    @PatchMapping("/frequentRoom")
    public ResponseEntity<?> updateFrequent(@RequestBody ChatRoomDTO chatRoomDTO) {
        log.info("chatRoomDTO : " + chatRoomDTO);

        ChatRoomDocument savedDocument = chatService.updateChatRoomFavorite(chatRoomDTO);
        if (savedDocument != null) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.ok("error");
        }
    }

    @GetMapping("/member/{uid}")
    public ResponseEntity<?> getAllFrequentMembers(@PathVariable String uid) {
        log.info("uid = " + uid);
        ChatMemberDocument chatMemberDocument = chatService.findChatMember(uid);
        log.info("chatMemberDocument = " + chatMemberDocument);
        return ResponseEntity.ok(Objects.requireNonNullElse(chatMemberDocument.toDTO(), "error"));
    }

    @PatchMapping("/frequentMembers")
    public ResponseEntity<?> updateFrequentMembers(@RequestParam("uid") String uid, @RequestParam("frequentUid") String frequentUid) {
        log.info("uid = " + uid + " frequentUid = " + frequentUid);

        String email = userService.getUserByuid(uid).getEmail();

        ChatMemberDocument frequentMember = chatService.findChatMember(frequentUid);

        ChatMemberDocument savedDocument = chatService.updateChatMemberFavorite(email, frequentMember);
        if (savedDocument != null) {
            return ResponseEntity.ok("success");
        } else {
            return ResponseEntity.ok("error");
        }
    }
}
