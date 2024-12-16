package com.backend.service;

import com.backend.document.chat.ChatMapperDocument;
import com.backend.document.chat.ChatMemberDocument;
import com.backend.document.chat.ChatMessageDocument;
import com.backend.document.chat.ChatRoomDocument;
import com.backend.dto.chat.ChatMessageDTO;
import com.backend.dto.chat.ChatRoomDTO;
import com.backend.dto.chat.NotificationResponse;
import com.backend.repository.chat.ChatMapperRepository;
import com.backend.repository.chat.ChatMemberRepository;
import com.backend.repository.chat.ChatMessageRepository;
import com.backend.repository.chat.ChatRoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Log4j2
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMapperRepository chatMapperRepository;
    public static final int PAGE_SIZE = 20;

    public String createChatRoom(ChatRoomDTO chatRoomDTO) {

        ChatRoomDocument chatRoomDocument = chatRoomDTO.toDocument();
        log.info("chatRoomDocument: " + chatRoomDocument);
        ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);
        String leader = savedDocument.getLeader();
        List<String> members = chatRoomDTO.getMembers();
        members.add(leader);

        if (members != null && leader != null) {
            members.forEach(uid -> {
                ChatMapperDocument mapperDocument = ChatMapperDocument.builder()
                        .userId(uid)
                        .chatRoomId(savedDocument.getId())
                        .joinedAt(LocalDateTime.now())
                        .build();
                chatMapperRepository.save(mapperDocument);

                ChatMemberDocument memberDocument = chatMemberRepository.findByUid(uid);
                if (memberDocument.getRoomIds() != null) {
                    memberDocument.getRoomIds().add(savedDocument.getId());
                    chatMemberRepository.save(memberDocument);
                }


            });
        }

        // 채팅방 생성 시 채팅방 구성원 전체에게 시스템 메시지 전송
        // 시스템 알림 메시지 설정
        ChatMessageDocument message = ChatMessageDocument.builder()
                .roomId(savedDocument.getId())
                .sender("System")
                .content("채팅방이 생성되었습니다.")
                .type("CREATE")
                .build();

        // 메시지를 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chat/" + savedDocument.getId(), message);

        return savedDocument.getId();
    }

    public ChatRoomDTO getChatRoomInfo(String chatRoomId) {
        if (chatRoomRepository.findById(chatRoomId).isPresent()) {
            ChatRoomDocument chatRoomDocument = chatRoomRepository.findById(chatRoomId).get();
            return chatRoomDocument.toDTO();
        }
        return null;
    }

    public List<ChatRoomDTO> getAllChatRoomsByUserId(String userId) {
        List<ChatRoomDocument> chatRoomList = chatRoomRepository.findAllByLeaderOrMembersAndStatusIsNot(userId, userId, 0);
        log.info("chatRoomList: " + chatRoomList);
        List<ChatRoomDTO> chatRoomDTOS = chatRoomList.stream().map(ChatRoomDocument::toDTO).toList();
        log.info("chatRoomDTOS: " + chatRoomDTOS);
        return chatRoomDTOS;
    }

    public ChatMemberDocument saveChatMember(ChatMemberDocument chatMemberDocument) {
        if (chatMemberDocument != null) {
            ChatMemberDocument savedDocument = chatMemberRepository.save(chatMemberDocument);
            return savedDocument;
        }
        return null;
    }

    public ChatRoomDocument updateChatRoomFavorite(ChatRoomDTO chatRoomDTO) {
        ChatRoomDocument chatRoomDocument = chatRoomRepository.findById(chatRoomDTO.getId()).orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. ID: " + chatRoomDTO.getId()));
        if (chatRoomDTO.getChatRoomFavorite() == 0 || chatRoomDTO.getChatRoomFavorite() == 1) {
            chatRoomDocument.setChatRoomFavorite(chatRoomDTO.getChatRoomFavorite());
        }
        ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);
        return savedDocument;
    }

    public String updateChatMemberFavorite(String email, ChatMemberDocument frequentMember, String type) {
        ChatMemberDocument loginUser = chatMemberRepository.findByEmail(email);
        if (loginUser != null && Objects.equals(type, "insert")) {
            if (!loginUser.getFrequent_members().contains(frequentMember)) {
                loginUser.getFrequent_members().add(frequentMember);
            } else {
                return "duplicate";
            }
            chatMemberRepository.save(loginUser);
            return "success";
        } else if (loginUser != null && Objects.equals(type, "delete")) {
            loginUser.getFrequent_members().remove(frequentMember);
            chatMemberRepository.save(loginUser);
            return "success";
        }
        return "failure";
    }

    public ChatMemberDocument findChatMember(String uid) {
        if (chatMemberRepository.findByUid(uid) != null) {
            ChatMemberDocument document = chatMemberRepository.findByUid(uid);
            return document;
        }
        return null;
    }

    @Transactional
    public void deleteChatMember(String uid, String roomId) {
        Optional<ChatRoomDocument> optionalChatRoomDocument = chatRoomRepository.findById(roomId);
        if (optionalChatRoomDocument.isPresent()) {
            ChatRoomDocument chatRoomDocument = optionalChatRoomDocument.get();
            if (chatRoomDocument.getMembers().contains(uid)) {
                chatRoomDocument.getMembers().remove(uid);
            } else if (chatRoomDocument.getLeader().equals(uid)) {
                chatRoomDocument.setLeader(null);
            }
            chatRoomRepository.save(chatRoomDocument);
        }
        chatMapperRepository.deleteByUserIdAndChatRoomId(uid, roomId);
    }

    public ChatMessageDocument saveMessage(ChatMessageDTO chatMessageDTO) {
        ChatMessageDocument chatMessageDocument = chatMessageDTO.toDocument();
        if (chatMessageDocument != null) {
            ChatMessageDocument savedDocument = chatMessageRepository.save(chatMessageDocument);
            return savedDocument;
        }
        return null;
    }

    // 특정 채팅방의 마지막으로 읽은 메시지부터 로드
    public List<ChatMessageDocument> getLatestMessages(String chatRoomId, String uid) {
        // 사용자와 채팅방에 대한 매퍼 정보 조회
        Optional<ChatMapperDocument> chatMapperOpt = chatMapperRepository.findByUserIdAndChatRoomId(uid, chatRoomId);
        // 채팅방의 가장 최신 메시지 조회
        Optional<ChatMessageDocument> chatMessageOpt = chatMessageRepository.findFirstByRoomIdOrderByTimeStampDesc(chatRoomId);

        if (chatMapperOpt.isPresent() && chatMessageOpt.isPresent()) {
            ChatMapperDocument chatMapperDocument = chatMapperOpt.get();
            LocalDateTime lastReadTime = chatMapperDocument.getLastReadTimeStamp();

            // 읽지 않은 메시지 조회 (최신 순으로 최대 20개)
            Pageable unreadPage = PageRequest.of(0, 20);
            List<ChatMessageDocument> unreadMessages = chatMessageRepository.findByRoomIdAndTimeStampAfterOrderByTimeStampDesc(chatRoomId, lastReadTime, unreadPage);
            Collections.reverse(unreadMessages);

            if (!unreadMessages.isEmpty()) {
                unreadMessages.get(0).setStatus(2);
            }

            if (!unreadMessages.isEmpty()) {
                // 읽지 않은 메시지가 20개 미만일 경우 추가 메시지 불러오기
                if (unreadMessages.size() < 20) {
                    int additionalCount = 20 - unreadMessages.size();
                    LocalDateTime firstUnreadTimestamp = unreadMessages.get(0).getTimeStamp();

                    Pageable additionalPage = PageRequest.of(0, additionalCount);
                    List<ChatMessageDocument> additionalMessages = chatMessageRepository.findByRoomIdAndTimeStampBeforeOrderByTimeStampDesc(chatRoomId, firstUnreadTimestamp, additionalPage);

                    // 추가 메시지를 역순으로 정렬하여 올바른 순서로 합치기
                    Collections.reverse(additionalMessages);
                    unreadMessages.addAll(0, additionalMessages);
                }
                return unreadMessages;
            } else {
                // 읽지 않은 메시지가 없을 경우 최신 20개 메시지 불러오기
                Pageable latestPage = PageRequest.of(0, 20);
                List<ChatMessageDocument> latestMessages = chatMessageRepository.findByRoomIdOrderByTimeStampDesc(chatRoomId, latestPage);

                // 올바른 시간 순서로 정렬
                Collections.reverse(latestMessages);
                return latestMessages;
            }
        }
        // 매퍼 정보 또는 메시지가 없을 경우 빈 리스트 반환
        return Collections.emptyList();
    }


    // 특정 채팅방의 이전 메시지 로드
    public List<ChatMessageDocument> getOlderMessages(String chatRoomId, LocalDateTime beforeTimestamp) {
        log.info("이전 메시지 로드 서비스 호출");

        // beforeTimestamp보다 작은 메시지들 중 최신 순으로 PAGE_SIZE만큼 조회
        PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE, Sort.by("timeStamp").descending());
        List<ChatMessageDocument> olderMessages = chatMessageRepository.findByRoomIdAndTimeStampBeforeOrderByTimeStampDesc(chatRoomId, beforeTimestamp, pageRequest);

        return olderMessages;
    }


    // 사용자가 채팅방을 읽었다고 표시
    public void markAsRead(String userId, String chatRoomId) {
        Optional<ChatMessageDocument> chatMessageOpt = chatMessageRepository.findFirstByRoomIdOrderByTimeStampDesc(chatRoomId);

        if (chatMessageOpt.isPresent()) {
            ChatMessageDocument latestMessage = chatMessageOpt.get();
            LocalDateTime newLastReadTimestamp;
            if (latestMessage != null) {
                newLastReadTimestamp = latestMessage.getTimeStamp();
                log.info("마지막 메시지 : " + newLastReadTimestamp);
            } else {
                newLastReadTimestamp = LocalDateTime.now();
            }
            chatMapperRepository.findByUserIdAndChatRoomId(userId, chatRoomId).ifPresent(chatMapper -> {
                chatMapper.setLastReadTimeStamp(newLastReadTimestamp);
                chatMapperRepository.save(chatMapper);
                log.info("markAsRead called for userId: " + userId + ", chatRoomId: " + chatRoomId + ", newLastReadTimestamp: " + newLastReadTimestamp);
            });
        }

    }

    // 사용자가 읽지 않은 메시지 수 가져오기
    public ChatMessageDTO getUnreadMessageCount(String userId, String chatRoomId) {

        Optional<ChatMapperDocument> chatMapperOpt = chatMapperRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
        Optional<ChatMessageDocument> chatMessageOpt = chatMessageRepository.findFirstByRoomIdOrderByTimeStampDesc(chatRoomId);

        if (chatMapperOpt.isPresent() && chatMessageOpt.isPresent()) {
            ChatMapperDocument chatMapperDocument = chatMapperOpt.get();
            LocalDateTime lastReadTimestamp = chatMapperDocument.getLastReadTimeStamp() != null
                    ? chatMapperDocument.getLastReadTimeStamp()
                    : chatMapperDocument.getJoinedAt(); // lastReadTimestamp가 없으면 joinedAt을 기준으로

            ChatMessageDocument chatMessageDocument = chatMessageOpt.get();
            ChatMessageDTO chatMessageDTO = chatMessageDocument.toDTO();
            long count = chatMessageRepository.countByRoomIdAndTimeStampAfter(chatRoomId, lastReadTimestamp);
            log.info("안읽은 수 : " + count);
            chatMessageDTO.setCount(count);
            return chatMessageDTO;
        }
        return ChatMessageDTO.builder()
                .count(0)
                .build();
    }

    public void updateUnreadCountsAndLastMessageAndLastTimeStamp(String chatRoomId, String senderId) {
        // 사용자가 참여한 채팅방 가져오기
        List<ChatMapperDocument> userChatMappings = chatMapperRepository.findByChatRoomId(chatRoomId);
        log.info("userChatMappings: " + userChatMappings);
        Optional<ChatMessageDocument> chatMessageOpt = chatMessageRepository.findFirstByRoomIdOrderByTimeStampDesc(chatRoomId);
        if (chatMessageOpt.isPresent()) {
            ChatMessageDocument chatMessageDocument = chatMessageOpt.get();
            LocalDateTime lastTimeStamp = chatMessageDocument.getTimeStamp();

            for (ChatMapperDocument mapper : userChatMappings) {
                String userId = mapper.getUserId();
                log.info("userId: " + userId);
                if (!userId.equals(senderId)) {
                    // 해당 사용자의 읽지 않은 메시지 수 업데이트
                    long count = getUnreadMessageCount(userId, chatRoomId).getCount();
                    log.info("count: " + count);
                    // 읽지 않은 메시지 수 알림 전송
                    NotificationResponse notification = new NotificationResponse();
                    notification.setType("unreadCount");
                    notification.setChatRoomId(chatRoomId);
                    notification.setUnreadCount((int) count);
                    messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
                }

                // 마지막 메시지 알림 전송
                log.info("lastMessage: " + chatMessageDocument);
                NotificationResponse lastMessageNotification = new NotificationResponse();
                lastMessageNotification.setType("lastMessage");
                lastMessageNotification.setChatRoomId(chatRoomId);
                lastMessageNotification.setLastTimeStamp(chatMessageDocument.getTimeStamp());
                lastMessageNotification.setLastMessage(chatMessageDocument.getContent());
                messagingTemplate.convertAndSend("/topic/notifications/" + userId, lastMessageNotification);
            }
        }
    }

}
