package com.backend.repository.chat;

import com.backend.document.chat.ChatMapperDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatMapperRepository extends MongoRepository<ChatMapperDocument, String> {
    Optional<ChatMapperDocument> findByUserIdAndChatRoomId(String userId, String chatRoomId);
    void deleteByUserIdAndChatRoomId(String userId, String chatRoomId);
}
