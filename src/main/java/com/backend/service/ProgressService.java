package com.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;


@Service
@Log4j2
public class ProgressService {
    private final SimpMessagingTemplate messagingTemplate;

    public ProgressService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendProgress(String userId, int progress) {
        messagingTemplate.convertAndSend("/topic/progress/" + userId, progress);
    }

}
