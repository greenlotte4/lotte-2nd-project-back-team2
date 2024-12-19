package com.backend.service;

import com.backend.document.chat.ChatMapperDocument;
import com.backend.document.chat.ChatMemberDocument;
import com.backend.document.chat.ChatMessageDocument;
import com.backend.document.chat.ChatRoomDocument;
import com.backend.dto.chat.ChatMapperDTO;
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

    @Transactional
    public String createChatRoom(ChatRoomDTO chatRoomDTO) {
        log.info("Creating chat room with DTO: {}", chatRoomDTO);
        try {
            String leader = chatRoomDTO.getLeader();
            if (leader == null || leader.isEmpty()) {
                return "error:리더 정보가 필요합니다.";
            }

            List<String> members = new ArrayList<>(chatRoomDTO.getMembers());
            members.add(leader);

            // 중복 제거 및 정렬
            Set<String> uniqueMembersSet = new HashSet<>(members);
            List<String> newCombinedList = new ArrayList<>(uniqueMembersSet);
            Collections.sort(newCombinedList);

            // 기존 채팅방 모두 조회
            List<ChatRoomDocument> existingRooms = chatRoomRepository.findAll();

            for (ChatRoomDocument room : existingRooms) {
                List<String> existingMembers = new ArrayList<>(room.getMembers());
                existingMembers.add(room.getLeader());

                Set<String> existingUniqueMembersSet = new HashSet<>(existingMembers);
                List<String> existingCombinedList = new ArrayList<>(existingUniqueMembersSet);
                Collections.sort(existingCombinedList);

                if (existingCombinedList.equals(newCombinedList)) {
                    log.error("중복된 채팅방 생성 시도: 리더={}, 멤버={}", leader, chatRoomDTO.getMembers());
                    return "error:이미 동일한 멤버로 구성된 채팅방이 존재합니다.";
                }
            }

            // 중복 없음, 채팅방 생성 진행
            ChatRoomDocument chatRoomDocument = chatRoomDTO.toDocument();
            log.info("chatRoomDocument: {}", chatRoomDocument);
            ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);
            String savedLeader = savedDocument.getLeader();
            List<String> savedMembers = new ArrayList<>(chatRoomDTO.getMembers());
            savedMembers.add(savedLeader);

            if (savedMembers != null && savedLeader != null) {
                for (String uid : savedMembers) {
                    // ChatMapperDocument 생성 및 저장
                    ChatMapperDocument mapperDocument = ChatMapperDocument.builder()
                            .userId(uid)
                            .chatRoomId(savedDocument.getId())
                            .joinedAt(LocalDateTime.now())
                            .build();
                    chatMapperRepository.save(mapperDocument);

                    // ChatMemberDocument 업데이트
                    ChatMemberDocument memberDocument = chatMemberRepository.findByUid(uid);
                    if (memberDocument.getRoomIds() != null) {
                        memberDocument.getRoomIds().add(savedDocument.getId());
                    } else {
                        memberDocument.setRoomIds(new ArrayList<>(Arrays.asList(savedDocument.getId())));
                    }
                    chatMemberRepository.save(memberDocument);
                }
            }

            // 시스템 메시지 생성 및 저장
            ChatMessageDocument message = ChatMessageDocument.builder()
                    .roomId(savedDocument.getId())
                    .sender("System")
                    .content("채팅방이 생성되었습니다.")
                    .type("CREATE")
                    .build();
            ChatMessageDocument savedMessage = chatMessageRepository.save(message);

            // 시스템 메시지 브로드캐스트
            messagingTemplate.convertAndSend("/topic/chat/" + savedDocument.getId(), message);

            // 알림을 위한 멤버 리스트 (리더 제외)
            savedMembers.remove(savedLeader);
            for (String member : savedMembers) {
                NotificationResponse response = NotificationResponse.builder()
                        .type("newRoom")
                        .lastMessage(savedMessage.getContent())
                        .lastTimeStamp(savedMessage.getTimeStamp())
                        .chatRoomId(savedMessage.getRoomId())
                        .unreadCount(1)
                        .leader(leader)
                        .members(savedMembers)
                        .chatRoomName(savedDocument.getChatRoomName())
                        .build();
                messagingTemplate.convertAndSend("/topic/notifications/" + member, response);
            }

            return savedDocument.getId();
        } catch (Exception e) {
            log.error("채팅방 생성 실패: {}", e.getMessage());
            return "error:채팅방 생성 실패";
        }
    }

    public ChatMemberDocument getChatMember(String uid) {
        return chatMemberRepository.findByUid(uid);
    }

    public ChatRoomDTO getChatRoomInfo(String chatRoomId) {
        if (chatRoomRepository.findById(chatRoomId).isPresent()) {
            ChatRoomDocument chatRoomDocument = chatRoomRepository.findById(chatRoomId).get();
            return chatRoomDocument.toDTO();
        }
        return null;
    }

    public ChatRoomDocument updateRoomName(String chatRoomId, String newName) {
        Optional<ChatRoomDocument> chatRoomOpt = chatRoomRepository.findById(chatRoomId);
        if (chatRoomOpt.isPresent()) {
            ChatRoomDocument chatRoomDocument = chatRoomOpt.get();
            chatRoomDocument.setChatRoomName(newName);
            ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);
            return savedDocument;
        }
        return null;
    }

    public List<ChatMemberDocument> getChatMembers(String chatRoomId) {
        ChatRoomDTO chatRoomDTO = getChatRoomInfo(chatRoomId);
        List<String> members = chatRoomDTO.getMembers();
        members.add(chatRoomDTO.getLeader());
        List<ChatMemberDocument> membersList = new ArrayList<>();
        for (String uid : members) {
            ChatMemberDocument memberDocument = chatMemberRepository.findByUid(uid);
            membersList.add(memberDocument);
        }
        return membersList;
    }

    @Transactional
    public ChatRoomDocument updateChatMembers(String chatRoomId, List<String> members) {
        Optional<ChatRoomDocument> chatRoomOpt = chatRoomRepository.findById(chatRoomId);
        if (chatRoomOpt.isPresent()) {
            ChatRoomDocument chatRoomDocument = chatRoomOpt.get();
            chatRoomDocument.getMembers().addAll(members);
            ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);

            // 채팅방 입장 알림 메시지를 위한 username 리스트
            List<String> userNames = new ArrayList<>();

            for (String uid : members) {
                ChatMemberDocument memberDocument = chatMemberRepository.findByUid(uid);
                memberDocument.getRoomIds().add(savedDocument.getId());
                chatMemberRepository.save(memberDocument);

                ChatMapperDocument chatMapperDocument = ChatMapperDocument.builder()
                        .userId(uid)
                        .chatRoomId(savedDocument.getId())
                        .joinedAt(LocalDateTime.now())
                        .build();
                chatMapperRepository.save(chatMapperDocument);

                userNames.add(memberDocument.getName());
            }

            // 시스템 알림 메시지 생성
            ChatMessageDocument message = ChatMessageDocument.builder()
                        .roomId(savedDocument.getId())
                        .sender("System")
                        .content(userNames + "님이 채팅방에 입장하였습니다.")
                        .type("JOIN")
                        .build();
            ChatMessageDocument savedMessage = chatMessageRepository.save(message);

            // 실시간 알림 전송
            messagingTemplate.convertAndSend("/topic/chat/" + savedDocument.getId(), message);

            List<String> allMembers = savedDocument.getMembers();
            allMembers.add(savedDocument.getLeader());

            for (String member : allMembers) {

                long count = getUnreadMessageCount(member, savedDocument.getId()).getCount();

                NotificationResponse unreadCount = NotificationResponse.builder()
                            .type("unreadCount")
                            .chatRoomId(savedMessage.getRoomId())
                            .unreadCount((int) count)
                            .build();
                NotificationResponse lastMessage = NotificationResponse.builder()
                            .type("lastMessage")
                            .chatRoomId(savedMessage.getRoomId())
                            .lastMessage(savedMessage.getContent())
                            .build();
                messagingTemplate.convertAndSend("/topic/notifications/" + member, unreadCount);
                messagingTemplate.convertAndSend("/topic/notifications/" + member, lastMessage);
            }
            return savedDocument;
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

    public List<ChatMapperDTO> getAllChatMappersByUserId(String userId) {
        List<ChatMapperDocument> chatMapperList = chatMapperRepository.findAllByUserId(userId);
        return chatMapperList.stream().map(ChatMapperDocument::toDTO).toList();
    }

    public ChatMemberDocument saveChatMember(ChatMemberDocument chatMemberDocument) {
        if (chatMemberDocument != null) {
            return chatMemberRepository.save(chatMemberDocument);
        }
        return null;
    }

    public ChatMapperDocument updateChatMapperIsFrequent(ChatMapperDTO chatMapperDTO) {

        String userId = chatMapperDTO.getUserId();
        String chatRoomId = chatMapperDTO.getChatRoomId();
        int isFrequent = chatMapperDTO.getIsFrequent();

        Optional<ChatMapperDocument> chatMapperOpt = chatMapperRepository.findByUserIdAndChatRoomId(userId, chatRoomId);
        if (chatMapperOpt.isPresent()) {
            ChatMapperDocument chatMapperDocument = chatMapperOpt.get();
            chatMapperDocument.setIsFrequent(isFrequent);
            ChatMapperDocument savedDocument = chatMapperRepository.save(chatMapperDocument);
            return savedDocument;
        }
        return null;
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
    public String deleteChatMember(String uid, String roomId) {
        log.info("uid: {}", uid);
        log.info("roomId: {}", roomId);
        try {
            // 채팅방 정보 조회
            Optional<ChatRoomDocument> optionalChatRoomDocument = chatRoomRepository.findById(roomId);
            if (!optionalChatRoomDocument.isPresent()) {
                log.error("채팅방을 찾을 수 없습니다: roomId={}", roomId);
                throw new Exception("채팅방을 찾을 수 없습니다.");
            }

            ChatRoomDocument chatRoomDocument = optionalChatRoomDocument.get();
            List<String> members = new ArrayList<>(chatRoomDocument.getMembers()); // 복사본 생성
            String leader = chatRoomDocument.getLeader();
            log.info("방 정보: {}", chatRoomDocument);

            // 사용자가 채팅방의 멤버인지 리더인지 확인
            boolean isMember = members.contains(uid);
            boolean isLeader = leader.equals(uid);

            if (isMember) {
                if (members.size() > 1) {
                    // 멤버가 나가는 경우
                    log.info("멤버가 나갔을 때: uid={}", uid);
                    members.remove(uid);
                    chatRoomDocument.setMembers(members);
                    chatRoomRepository.save(chatRoomDocument);
                } else if (members.size() == 1) {
                    // 마지막 멤버가 나가는 경우
                    log.info("마지막 멤버가 나가는 경우: uid={}", uid);
                    members.remove(uid);
                    chatRoomDocument.setMembers(members);
                    // 리더가 방에 남아 있는지 확인
                    if (!leader.isEmpty()) {
                        log.info("리더가 방에 남아 있으므로 방을 삭제하지 않습니다: leader={}", leader);
                        chatRoomRepository.save(chatRoomDocument);
                    } else {
                        chatRoomRepository.delete(chatRoomDocument);
                        chatMessageRepository.deleteAllByRoomId(roomId);
                    }
                }
            } else if (isLeader) {
                if (!members.isEmpty()) {
                    // 리더가 나가면서 다른 멤버가 있는 경우, 새 리더 지정
                    log.info("리더가 나갔을 때: uid={}", uid);
                    String nextLeader = members.get(0);
                    chatRoomDocument.setLeader(nextLeader);
                    members.remove(nextLeader);
                    chatRoomDocument.setMembers(members);
                    chatRoomRepository.save(chatRoomDocument);
                    log.info("새 리더: {}", nextLeader);
                } else {
                    // 리더가 나가면서 다른 멤버가 없는 경우, 방 삭제
                    log.info("리더가 나가면서 다른 멤버가 없는 경우: uid={}", uid);
                    chatRoomRepository.delete(chatRoomDocument);
                    chatMessageRepository.deleteAllByRoomId(roomId);
                }
            } else {
                log.error("사용자가 채팅방의 멤버나 리더가 아닙니다: uid={}", uid);
                throw new RuntimeException("사용자가 채팅방의 멤버나 리더가 아닙니다.");
            }

            // 채팅맵퍼 삭제
            chatMapperRepository.deleteByUserIdAndChatRoomId(uid, roomId);



            // 사용자 정보 조회 및 사용자 정보에서 해당 채팅방 ID 삭제
            ChatMemberDocument chatMemberDocument = chatMemberRepository.findByUid(uid);
            String userName = chatMemberDocument.getName();

            chatMemberDocument.getRoomIds().remove(roomId);
            chatMemberRepository.save(chatMemberDocument);

            if (chatRoomRepository.findById(roomId).isPresent()) {

                // 시스템 알림 메시지 생성
                ChatMessageDocument message = ChatMessageDocument.builder()
                        .roomId(roomId)
                        .sender("System")
                        .content(userName + "님이 채팅방을 나갔습니다.")
                        .type("LEAVE")
                        .build();
                ChatMessageDocument savedMessage = chatMessageRepository.save(message);

                // 실시간 알림 전송
                messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);

                members.add(leader);
                members.remove(uid);

                for (String member : members) {

                    long count = getUnreadMessageCount(member, roomId).getCount();

                    NotificationResponse unreadCount = NotificationResponse.builder()
                            .type("unreadCount")
                            .chatRoomId(savedMessage.getRoomId())
                            .unreadCount((int) count)
                            .build();
                    NotificationResponse lastMessage = NotificationResponse.builder()
                            .type("lastMessage")
                            .chatRoomId(savedMessage.getRoomId())
                            .lastMessage(savedMessage.getContent())
                            .build();
                    messagingTemplate.convertAndSend("/topic/notifications/" + member, unreadCount);
                    messagingTemplate.convertAndSend("/topic/notifications/" + member, lastMessage);
                }
            }
            return "success";
        } catch (Exception e) {
            log.error("채팅방 나가기 실패: uid={}, roomId={}, error={}", uid, roomId, e.getMessage());
            throw new RuntimeException("채팅방 나가기 실패", e);
        }
    }

    public ChatMessageDocument saveMessage(ChatMessageDTO chatMessageDTO) {
        log.info("chatMessageDTO: {}", chatMessageDTO);
        String userName = chatMemberRepository.findByUid(chatMessageDTO.getSender()).getName();
        ChatMessageDocument chatMessageDocument = chatMessageDTO.toDocument();
        chatMessageDocument.setSenderName(userName);

        ChatMessageDocument savedDocument = chatMessageRepository.save(chatMessageDocument);
        Optional<ChatMapperDocument> chatMapperOpt = chatMapperRepository.findByUserIdAndChatRoomId(savedDocument.getSender(), savedDocument.getRoomId());
        if (chatMapperOpt.isPresent()) {
            ChatMapperDocument chatMapperDocument = chatMapperOpt.get();
            chatMapperDocument.setLastReadTimeStamp(savedDocument.getTimeStamp());
            chatMapperRepository.save(chatMapperDocument);
        }
        return savedDocument;
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
