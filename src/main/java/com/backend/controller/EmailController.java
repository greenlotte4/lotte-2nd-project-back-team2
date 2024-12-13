package com.backend.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;

import com.backend.dto.request.email.QnaRequestDto;
import com.backend.dto.request.email.CancellationRequestDto;
import com.backend.dto.request.email.ProductServiceRequestDto;
import com.backend.dto.request.email.PaymentRequestDto;
import com.backend.service.EmailService;

import lombok.extern.slf4j.Slf4j;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class EmailController {
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/send-qna")
    public ResponseEntity<?> sendQnaEmail(@ModelAttribute QnaRequestDto request) {
        try {
            emailService.sendQnaEmail(request);
            return ResponseEntity.ok().body(Map.of("message", "문의가 성공적으로 전송되었습니다."));
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "문의 전송에 실패했습니다: " + e.getMessage()));
        }
    }
    
    @PostMapping("/send-cancellation")
    public ResponseEntity<?> sendCancellationEmail(@RequestBody CancellationRequestDto request) {
        try {
            emailService.sendCancellationEmail(request);
            return ResponseEntity.ok().body(Map.of("message", "문의가 성공적으로 전송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "문의 전송에 실패했습니다."));
        }
    }
    
    @PostMapping("/send-product-service")
    public ResponseEntity<?> sendProductServiceEmail(@RequestBody ProductServiceRequestDto request) {
        try {
            emailService.sendProductServiceEmail(request);
            return ResponseEntity.ok().body(Map.of("message", "문의가 성공적으로 전송되었습니다."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "문의 전송에 실패했습니다."));
        }
    }
    
    @PostMapping("/send-payment")
    public ResponseEntity<?> sendPaymentEmail(@RequestBody PaymentRequestDto request) {
        try {
            log.debug("Received payment email request: {}", request);
            
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("message", "이메일 주소는 필수 항목입니다."));
            }
            
            emailService.sendPaymentEmail(request);
            return ResponseEntity.ok()
                .body(Map.of("message", "문의가 성공적으로 전송되었습니다."));
                
        } catch (Exception e) {
            log.error("이메일 전송 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "문의 전송에 실패했습니다: " + e.getMessage()));
        }
    }
}