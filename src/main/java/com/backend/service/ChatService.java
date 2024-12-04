package com.backend.service;

import com.backend.document.chat.ChatMemberDocument;
import com.backend.document.chat.ChatRoomDocument;
import com.backend.dto.chat.ChatRoomDTO;
import com.backend.entity.user.User;
import com.backend.repository.chat.ChatMemberRepository;
import com.backend.repository.chat.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class ChatService {

    private final ChatMemberRepository chatMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserService userService;

    public String createChatRoom(ChatRoomDTO chatRoomDTO) {

        ChatRoomDocument chatRoomDocument = chatRoomDTO.toDocument();
        log.info("chatRoomDocument: " + chatRoomDocument);
        ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);
        return savedDocument.getId();
    }

    public ChatRoomDTO getChatRoomInfo(String chatRoomId) {
        log.info("chatRoomId: " + chatRoomId);
        if (chatRoomRepository.findById(chatRoomId).isPresent()) {
            ChatRoomDocument chatRoomDocument = chatRoomRepository.findById(chatRoomId).get();
            return chatRoomDocument.toDTO();
        }
        return null;
    }

    public List<ChatRoomDTO> getAllChatRoomsByUserId(String userId) {
        List<ChatRoomDocument> chatRoomList = chatRoomRepository.findAllByLeaderOrMembers(userId, userId);
        log.info("chatRoomList: " + chatRoomList);
        List<ChatRoomDTO> chatRoomDTOS = chatRoomList.stream().map(ChatRoomDocument::toDTO).toList();
        log.info("chatRoomDTOS: " + chatRoomDTOS);
        return chatRoomDTOS;
    }

    public ChatMemberDocument saveChatMember(String chatRoomId, User user, String groupName) {
        ChatMemberDocument chatMemberDocument = new ChatMemberDocument();
        chatMemberDocument.setUid(user.getUid());
        chatMemberDocument.setEmail(user.getEmail());
        chatMemberDocument.setHp(user.getHp());
        chatMemberDocument.setLevel(user.getLevel());
        chatMemberDocument.setName(user.getName());
        chatMemberDocument.setGroup(groupName);
        chatMemberDocument.getRoomIds().add(chatRoomId);

        ChatMemberDocument savedDocument = chatMemberRepository.save(chatMemberDocument);
        return savedDocument;
    }

    public ChatRoomDocument updateChatRoomFavorite(ChatRoomDTO chatRoomDTO) {
        ChatRoomDocument chatRoomDocument = chatRoomRepository.findById(chatRoomDTO.getId()).orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다. ID: " + chatRoomDTO.getId()));
        if (chatRoomDTO.getChatRoomFavorite() == 0 || chatRoomDTO.getChatRoomFavorite() == 1) {
            chatRoomDocument.setChatRoomFavorite(chatRoomDTO.getChatRoomFavorite());
        }
        ChatRoomDocument savedDocument = chatRoomRepository.save(chatRoomDocument);
        return savedDocument;
    }

    public ChatMemberDocument updateChatMemberFavorite(String email, ChatMemberDocument frequentMember)  {
        log.info("frequentMember: " + frequentMember);
        ChatMemberDocument loginUser = chatMemberRepository.findByEmail(email);
        log.info("loginUser: " + loginUser);
        if (loginUser != null) {
            if (!loginUser.getFrequent_members().contains(frequentMember)) {
                log.info("중복 없음 - 추가");
                loginUser.getFrequent_members().add(frequentMember);
            } else {
                log.info("중복 있음 - 제거");
                loginUser.getFrequent_members().remove(frequentMember);
            }
            return chatMemberRepository.save(loginUser);
        } else {
            log.error("해당 이메일의 사용자를 찾을 수 없습니다: {}", email);
        }
        return null;
    }

    public ChatMemberDocument findChatMember(String uid) {
        if (chatMemberRepository.findByUid(uid) != null) {
            ChatMemberDocument document = chatMemberRepository.findByUid(uid);
            log.info("document: " + document);
            return document;
        }
        return null;
    }

}
