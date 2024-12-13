package com.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.extern.slf4j.Slf4j;

import com.backend.dto.request.email.QnaRequestDto;
import com.backend.dto.request.email.CancellationRequestDto;
import com.backend.dto.request.email.ProductServiceRequestDto;
import com.backend.dto.request.email.PaymentRequestDto;

@Service
@Slf4j
public class EmailService {
    
    @Autowired
    private JavaMailSender emailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;

    // 공통 이메일 전송 메서드
    private void sendEmail(String[] to, String subject, String content, MultipartFile attachment) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        
        if (attachment != null && !attachment.isEmpty()) {
            helper.addAttachment(attachment.getOriginalFilename(), attachment);
        }
        
        emailSender.send(message);
    }

    public void sendQnaEmail(QnaRequestDto request) {
        try {
            log.info("QNA 이메일 전송 시작: {}", request.getEmail());
            
            String subject = "[QNA 문의 접수 확인] " + request.getTitle();
            String content = String.format("""
                <h2>QNA 문의가 접수되었습니다</h2>
                <p>안녕하세요, %s님.</p>
                <p>문의하신 내용이 정상적으로 접수되었습니다.</p>
                <p>접수시간: %s</p>
                <hr>
                <p><strong>카테고리:</strong> %s</p>
                <p><strong>우선순위:</strong> %s</p>
                <p><strong>문의자명:</strong> %s</p>
                <p><strong>이메일:</strong> %s</p>
                <p><strong>문의내용:</strong></p>
                <p>%s</p>
                <hr>
                <p>빠른 시일 내에 답변 드리도록 하겠습니다.</p>
                """, 
                request.getName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                request.getCategory(),
                request.getPriority(),
                request.getName(),
                request.getEmail(),
                request.getContent()
            );
            
            sendEmail(
                new String[]{fromEmail, request.getEmail()},
                subject,
                content,
                request.getAttachments()
            );
            
            log.info("QNA 이메일 전송 성공");
            
        } catch (Exception e) {
            log.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            log.error("상세 에러: ", e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    public void sendCancellationEmail(CancellationRequestDto request) {
        try {
            log.info("취소/반품 이메일 전송 시작: {}", request.getEmail());
            
            String subject = "[취소/반품 문의] " + request.getTitle();
            String content = String.format("""
                <h2>취소/반품 문의가 접수되었습니다</h2>
                <p>안녕하세요, %s님.</p>
                <p>접수시간: %s</p>
                <hr>
                <p><strong>주문번호:</strong> %s</p>
                <p><strong>상품명:</strong> %s</p>
                <p><strong>반품사유:</strong> %s</p>
                <p><strong>문의자명:</strong> %s</p>
                <p><strong>이메일:</strong> %s</p>
                <p><strong>상세내용:</strong></p>
                <p>%s</p>
                <hr>
                <p>빠른 시일 내에 답변 드리도록 하겠습니다.</p>
                """,
                request.getName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                request.getOrderNumber(),
                request.getProductName(),
                request.getReturnReason(),
                request.getName(),
                request.getEmail(),
                request.getContent()
            );
            
            sendEmail(
                new String[]{fromEmail, request.getEmail()},
                subject,
                content,
                null
            );
            
            log.info("취소/반품 이메일 전송 성공");
            
        } catch (Exception e) {
            log.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            log.error("상세 에러: ", e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    public void sendProductServiceEmail(ProductServiceRequestDto request) {
        try {
            log.info("제품/서비스 문의 이메일 전송 시작: {}", request.getEmail());
            
            String subject = "[제품/서비스 문의] " + request.getTitle();
            String content = String.format("""
                <h2>제품/서비스 문의가 접수되었습니다</h2>
                <p>안녕하세요, %s님.</p>
                <p>접수시간: %s</p>
                <hr>
                <p><strong>제품명:</strong> %s</p>
                <p><strong>제품유형:</strong> %s</p>
                <p><strong>서비스유형:</strong> %s</p>
                <p><strong>구매일자:</strong> %s</p>
                <p><strong>문의자명:</strong> %s</p>
                <p><strong>이메일:</strong> %s</p>
                <p><strong>상세내용:</strong></p>
                <p>%s</p>
                <hr>
                <p>빠른 시일 내에 답변 드리도록 하겠습니다.</p>
                """,
                request.getName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                request.getProductName(),
                request.getProductType(),
                request.getServiceType(),
                request.getPurchaseDate(),
                request.getName(),
                request.getEmail(),
                request.getContent()
            );
            
            sendEmail(
                new String[]{fromEmail, request.getEmail()},
                subject,
                content,
                null
            );
            
            log.info("제품/서비스 문의 이메일 전송 성공");
            
        } catch (Exception e) {
            log.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            log.error("상세 에러: ", e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
}