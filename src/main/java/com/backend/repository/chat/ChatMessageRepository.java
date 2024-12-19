package com.backend.repository.chat;

import com.backend.document.chat.ChatMessageDocument;
import com.backend.entity.message.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessageDocument, String> {
    List<ChatMessageDocument> findAllByRoomId(String roomId);

    // 특정 채팅방의 timestamp 이전 메시지들을 내림차순으로 조회
    List<ChatMessageDocument> findByRoomIdAndTimeStampBeforeOrderByTimeStampDesc(String chatRoomId, LocalDateTime timestamp, Pageable pageable);

    // 특정 채팅방의 마지막 읽은 메시지 이후 메시지들을 내림차순으로 조회
    List<ChatMessageDocument> findByRoomIdAndTimeStampAfterOrderByTimeStampDesc(String chatRoomId, LocalDateTime timestamp, Pageable pageable);

    // 특정 채팅방의 최신 메시지들을 내림차순으로 조회
    List<ChatMessageDocument> findByRoomIdOrderByTimeStampDesc(String chatRoomId, Pageable pageable);

    // 읽지 않은 메시지 카운트
    long countByRoomIdAndTimeStampAfter(String chatRoomId, LocalDateTime timestamp);

    // 특정 채팅방의 최신 메시지 1개를 채팅방 ID로 조회
    Optional<ChatMessageDocument> findFirstByRoomIdOrderByTimeStampDesc(String chatRoomId);

    void deleteAllByRoomId(String roomId);
}

