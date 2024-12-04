package com.backend.repository.chat;

import com.backend.document.chat.ChatMemberDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMemberRepository extends MongoRepository<ChatMemberDocument, String> {
    ChatMemberDocument findByUid(String uid);
    ChatMemberDocument findByEmail(String email);
}
