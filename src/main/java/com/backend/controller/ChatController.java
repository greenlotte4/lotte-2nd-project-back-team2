package com.backend.controller;

import com.backend.document.chat.ChatMemberDocument;
import com.backend.document.chat.ChatMessageDocument;
import com.backend.document.chat.ChatRoomDocument;
import com.backend.dto.chat.ChatMessageDTO;
import com.backend.dto.chat.ChatRoomDTO;
import com.backend.entity.user.User;
import com.backend.service.ChatService;
import com.backend.service.GroupService;
import com.backend.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;


@Log4j2
@RequestMapping("/api/message")
@RequiredArgsConstructor
@RestController
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;
    private final GroupService groupService;
    private final SimpMessagingTemplate messagingTemplate;

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
    public ResponseEntity<?> updateFrequentMembers(
            @RequestParam("uid") String uid,
            @RequestParam("frequentUid") String frequentUid,
            @RequestParam("type") String type) {

        log.info("uid = " + uid + " frequentUid = " + frequentUid + " type = " + type);

        String email = userService.getUserByuid(uid).getEmail();

        ChatMemberDocument frequentMember = chatService.findChatMember(frequentUid);

        String status = chatService.updateChatMemberFavorite(email, frequentMember, type);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/saveMessage")
    public ResponseEntity<ChatMessageDocument> saveMessage(@RequestBody ChatMessageDTO chatMessageDTO) {
        log.info("chatMessageDTO = " + chatMessageDTO);
        ChatMessageDocument savedDocument = chatService.saveMessage(chatMessageDTO);
        if (savedDocument != null) {
            return ResponseEntity.ok(savedDocument);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getMessage/{roomId}")
    public ResponseEntity<?> getMessage(@PathVariable("roomId") String roomId) {
        log.info("roomId = " + roomId);
        List<ChatMessageDTO> chatMessageDTOS = chatService.getAllMessagesByChatRoomId(roomId);
        if (!chatMessageDTOS.isEmpty()) {
            return ResponseEntity.ok(chatMessageDTOS);
        }
        return ResponseEntity.noContent().build();
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO chatMessageDTO) {

        ChatMessageDocument chatMessageDocument = chatMessageDTO.toDocument();
        log.info("Sending message to topic: /topic/chat/" + chatMessageDocument.getRoomId());

        // 메시지를 해당 채팅방의 구성원들에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat/" + chatMessageDocument.getRoomId(), chatMessageDocument);

        log.info("Message sent to topic: /topic/chat/" + chatMessageDocument.getRoomId());
    }

}
