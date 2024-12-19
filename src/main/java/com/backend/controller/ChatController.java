package com.backend.controller;

import com.backend.document.chat.*;
import com.backend.dto.chat.*;
import com.backend.service.ChatService;
import com.backend.service.GroupService;
import com.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


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
        List<ChatMapperDTO> chatMapperDTOS = chatService.getAllChatMappersByUserId(userUid);

        // ChatMapperDTO 리스트를 chatRoomId를 키로 하는 맵으로 변환합니다.
        Map<String, Integer> chatMapperMap = chatMapperDTOS.stream()
                .collect(Collectors.toMap(ChatMapperDTO::getChatRoomId, ChatMapperDTO::getIsFrequent));

        // ChatRoomDTO 리스트를 순회하며 chatRoomFavorite을 설정합니다.
        chatRoomDTOS.forEach(chatRoomDTO -> {
            Integer isFrequent = chatMapperMap.get(chatRoomDTO.getId());
            // isFrequent가 null인 경우 기본값(0) 설정
            chatRoomDTO.setChatRoomFavorite(isFrequent != null ? isFrequent : 0);
        });

        if (chatRoomDTOS.isEmpty()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(chatRoomDTOS);
        }
    }

    @GetMapping("/{uid}")
    public ResponseEntity<ChatMemberDocument> getUser(@PathVariable String uid) {
        log.info("uid = " + uid);
        ChatMemberDocument chatMemberDocument = chatService.getChatMember(uid);
        if (chatMemberDocument == null) {
            return ResponseEntity.notFound().build();
        }else {
            return ResponseEntity.ok(chatMemberDocument);
        }
    }

    @GetMapping("/roomInfo/{roomId}") // 채팅방 id로 해당 채팅방의 정보 조회
    public ResponseEntity<?> getChatRoomInfo(@PathVariable String roomId) {

        ChatRoomDTO chatRoomDTO = chatService.getChatRoomInfo(roomId);
        if (chatRoomDTO == null) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(chatRoomDTO);
        }
    }

    @GetMapping("/roomMembers/{roomId}") // 채팅방 id로 해당 채팅방의 모든 멤버 목록 조회
    public ResponseEntity<List<ChatMemberDocument>> getChatRoomMembers(@PathVariable String roomId) {
        List<ChatMemberDocument> members = chatService.getChatMembers(roomId);
        if (members.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(members);
    }

    @PostMapping("/room")
    public ResponseEntity<?> createChatRoom(@ModelAttribute ChatRoomDTO chatRoomDTO) {
        String result = chatService.createChatRoom(chatRoomDTO);
        if (result.startsWith("error:")) {
            String errorMessage = result.substring(6);
            if (errorMessage.contains("동일한 멤버로 구성된 채팅방")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse("error", errorMessage));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse("error", errorMessage));
        }
        return ResponseEntity.ok(new ApiResponse("success", result));
    }

    @DeleteMapping("/quitRoom")
    public ResponseEntity<?> quitRoom(@RequestBody ChatRoomDTO chatRoomDTO) {
        log.info("chatRoomDTO : " + chatRoomDTO);
        String roomId = chatRoomDTO.getId();
        String uid = chatRoomDTO.getLeader();
        String status = chatService.deleteChatMember(uid, roomId);
        if (status == null) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(status);
        }
    }

    @PatchMapping("/frequentRoom")
    public ResponseEntity<?> updateFrequent(@RequestBody ChatMapperDTO chatMapperDTO) {
        log.info("chatMapperDTO : " + chatMapperDTO);
        ChatMapperDocument savedDocument = chatService.updateChatMapperIsFrequent(chatMapperDTO);
        if (savedDocument == null) {
            return ResponseEntity.ok("failure");
        }else {
            return ResponseEntity.ok("success");
        }
    }

    @PatchMapping("/roomName")
    public ResponseEntity<?> updateRoomName(@RequestBody ChatRoomDTO chatRoomDTO) {
        log.info("chatRoomDTO : " + chatRoomDTO);
        String roomId = chatRoomDTO.getId();
        String newName = chatRoomDTO.getChatRoomName();
        ChatRoomDocument savedDocument = chatService.updateRoomName(roomId, newName);
        log.info("savedDocument : " + savedDocument);
        if (savedDocument != null) {
            return ResponseEntity.ok(savedDocument.getChatRoomName());
        } else {
            return ResponseEntity.ok("error");
        }
    }

    @PatchMapping("/chatMembers")
    public ResponseEntity<?> updateChatMembers(@RequestBody ChatRoomDTO chatRoomDTO) {
        ChatRoomDocument savedDocument = chatService.updateChatMembers(chatRoomDTO.getId(), chatRoomDTO.getMembers());
        if (savedDocument != null) {
            return ResponseEntity.ok("success");
        }
        return ResponseEntity.ok("failure");
    }

    @GetMapping("/member/{uid}")
    public ResponseEntity<?> getAllFrequentMembers(@PathVariable String uid) {
        ChatMemberDocument chatMemberDocument = chatService.findChatMember(uid);
        return ResponseEntity.ok(chatMemberDocument);
    }

    @PatchMapping("/frequentMembers")
    public ResponseEntity<?> updateFrequentMembers(
            @RequestParam("uid") String uid,
            @RequestParam("frequentUid") String frequentUid,
            @RequestParam("type") String type) {


        String email = userService.getUserByuid(uid).getEmail();

        ChatMemberDocument frequentMember = chatService.findChatMember(frequentUid);

        String status = chatService.updateChatMemberFavorite(email, frequentMember, type);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/saveMessage")
    public ResponseEntity<ChatMessageDocument> saveMessage(@RequestBody ChatMessageDTO chatMessageDTO) {
        log.info("시간 : " + chatMessageDTO.getTimeStamp());
        ChatMessageDocument savedDocument = chatService.saveMessage(chatMessageDTO);
        if (savedDocument != null) {
            return ResponseEntity.ok(savedDocument);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getMessage")
    public ChatResponseDocument getMessages(
            @RequestParam String chatRoomId,
            @RequestParam String uid,
            @RequestParam(required = false) String before // ISO 8601 형식의 timestamp
    ) {
        List<ChatMessageDocument> messages;
        boolean hasMore;
        if (before != null) {
            log.info("이전 메시지 호출");
            LocalDateTime beforeTimestamp = LocalDateTime.parse(before);
            messages = chatService.getOlderMessages(chatRoomId, beforeTimestamp);
            hasMore = messages.size() == ChatService.PAGE_SIZE;
        } else {
            log.info("처음 메시지 호출");
            messages = chatService.getLatestMessages(chatRoomId, uid);
            hasMore = true;
        }
        return ChatResponseDocument.builder()
                .messages(messages)
                .hasMore(hasMore)
                .build();
    }

    // 읽지 않은 메시지 수 조회
    @GetMapping("/unreadCount")
    public ChatMessageDTO getUnreadMessageCount(@RequestParam String uid,
                                                @RequestParam String chatRoomId
    ) {
        ChatMessageDTO chatMessageDTO = chatService.getUnreadMessageCount(uid, chatRoomId);
        return chatMessageDTO;
    }

    // 읽음 상태 업데이트
    @PostMapping("/markAsRead")
    public void markAsRead(
            @RequestBody ChatMessageDTO chatMessageDTO
    ) {
        log.info("markAsRead - chatMessageDTO : " + chatMessageDTO);
        String uid = chatMessageDTO.getSender();
        String chatRoomId = chatMessageDTO.getRoomId();
        chatService.markAsRead(uid, chatRoomId);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO chatMessageDTO) {

        ChatMessageDocument chatMessageDocument = chatMessageDTO.toDocument();

        // 메시지를 해당 채팅방의 구성원들에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat/" + chatMessageDocument.getRoomId(), chatMessageDocument);

        // 읽지 않은 메시지 수 및 마지막 메시지 업데이트
        chatService.updateUnreadCountsAndLastMessageAndLastTimeStamp(chatMessageDocument.getRoomId(), chatMessageDocument.getSender());
    }


}
