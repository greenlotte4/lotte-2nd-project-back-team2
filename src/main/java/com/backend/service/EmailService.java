package com.backend.service;

import com.backend.document.drive.Folder;
import com.backend.document.drive.Invitation;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.multipart.MultipartFile;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.backend.dto.request.email.QnaRequestDto;
import com.backend.dto.request.email.CancellationRequestDto;
import com.backend.dto.request.email.ProductServiceRequestDto;
import com.backend.dto.request.email.PaymentRequestDto;

@Service
@Slf4j
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")  // application.yml에 설정된 이메일 주소
    private String fromEmail;

    @Value("${admin.email}")
    private String adminEmail; // 관리자 이메일 설정

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

        // 이메일 전송 시 로깅
        logger.info("이메일 전송 시작: {}", subject);

        emailSender.send(message);

        // 성공적인 이메일 전송 로깅
        logger.info("이메일 전송 성공: {}", subject);
    }

    // QNA 이메일 전송 메서드
    public void sendQnaEmail(QnaRequestDto request) {
        try {
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

        } catch (Exception e) {
            logger.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            logger.error("상세 에러: ", e);
            sendErrorNotification(e);  // 실패 시 관리자에게 알림 전송
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    // 자동 응답 이메일 보내기
    public void sendAutoReplyEmail(String userEmail) {
        try {
            String subject = "[자동 응답] 문의가 접수되었습니다";
            String content = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #666bff; color: white; padding: 20px; text-align: center; }
                    .content { padding: 20px; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>문의가 접수되었습니다</h2>
                    </div>
                    <div class="content">
                        <p>안녕하세요,</p>
                        <p>귀하의 문의가 성공적으로 접수되었습니다.</p>
                        <p>접수 시간: %s</p>
                        <p>담당자가 확인 후 1-2일 이내에 답변 드리도록 하겠습니다.</p>
                        <p>추가 문의사항이 있으시다면 언제든 문의해 주시기 바랍니다.</p>
                        <br>
                        <p>감사합니다.</p>
                    </div>
                    <div class="footer">
                        <p>본 메일은 자동 발송되는 메일입니다.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
             MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
             helper.setFrom(fromEmail);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(content, true);
             emailSender.send(message);
            logger.info("자동 응답 이메일 전송 성공: {}", userEmail);
         } catch (Exception e) {
            logger.error("자동 응답 이메일 전송 실패: {}", e.getMessage());
            logger.error("상세 에러: ", e);
        }
    }
    // 취소/반품 이메일 전송 메서드
    public void sendCancellationEmail(CancellationRequestDto request) {
        try {
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

        } catch (Exception e) {
            logger.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            logger.error("상세 에러: ", e);
            sendErrorNotification(e);  // 실패 시 관리자에게 알림 전송
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    // 제품/서비스 이메일 전송 메서드
    public void sendProductServiceEmail(ProductServiceRequestDto request) {
        try {
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

        } catch (Exception e) {
            logger.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            logger.error("상세 에러: ", e);
            sendErrorNotification(e);  // 실패 시 관리자에게 알림 전송
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    // 결제 문의 이메일 전송 메서드
    public void sendPaymentEmail(PaymentRequestDto request) {
        try {
            String subject = "[결제 문의] " + (request.getTitle() != null ? request.getTitle() : "제목 없음");
            String content = String.format("""
                <h2>결제 문의가 접수되었습니다</h2>
                <p>안녕하세요, %s님.</p>
                <p>접수시간: %s</p>
                <hr>
                <p><strong>주문번호:</strong> %s</p>
                <p><strong>결제금액:</strong> %d원</p>
                <p><strong>결제방법:</strong> %s</p>
                <p><strong>결제일자:</strong> %s</p>
                <p><strong>문의유형:</strong> %s</p>
                <p><strong>문의내용:</strong></p>
                <p>%s</p>
                """,
                    request.getName() != null ? request.getName() : "고객",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    request.getOrderNumber(),
                    request.getPaymentAmount(),
                    request.getPaymentMethod(),
                    request.getPaymentDate(),
                    request.getInquiryType(),
                    request.getContent()
            );

            sendEmail(
                    new String[]{request.getEmail(), fromEmail},
                    subject,
                    content,
                    null
            );

        } catch (Exception e) {
            logger.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            logger.error("상세 에러: ", e);
            sendErrorNotification(e);  // 실패 시 관리자에게 알림 전송
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }

    // 관리자에게 실패 알림 이메일 보내기
    private void sendErrorNotification(Exception e) {
        String subject = "이메일 전송 실패 알림";
        String content = String.format("""
            <h2>이메일 전송이 실패하였습니다.</h2>
            <p>에러 메시지: %s</p>
            <p>상세 에러:</p>
            <pre>%s</pre>
            <hr>
            <p>관리자님, 확인 부탁드립니다.</p>
            """, e.getMessage(), e.getStackTrace());

        try {
            sendEmail(new String[]{adminEmail}, subject, content, null);
        } catch (MessagingException ex) {
            logger.error("관리자 이메일 전송 실패. 에러 메시지: {}", ex.getMessage());
        }
    }


    public void sendToInvitation(List<Invitation> invitations, Folder folder){
        try {
            for(Invitation invitation: invitations){
                String subject = "PLANTRY에서 새로운 폴더로 초대합니다!";

              //  String link = "http://localhost:8010/accept-invitation/" + invitation.getId();
                String link = "http://13.124.94.213:90/accept-invitation/" + invitation.getId();
                // HTML 이메일 내용
                String content = """
                    <!DOCTYPE html>
                    <html lang="ko">
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body { font-family: 'Pretendard', -apple-system, BlinkMacSystemFont, system-ui, sans-serif; line-height: 1.6; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; padding: 20px; background-color: #f9f9fc; border-radius: 12px; }
                            .header { background: linear-gradient(135deg, #6a11cb 0%%, #2575fc 100%%); color: white; padding: 20px; text-align: center; border-radius: 12px 12px 0 0; }
                            .content { background: white; padding: 30px; border-radius: 0 0 12px 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }
                            .btn { display: inline-block; background-color: #2575fc; color: #fffff; padding: 12px 24px; text-decoration: none; border-radius: 8px; margin: 20px 0; }
                            .footer { text-align: center; color: #888; font-size: 12px; margin-top: 20px; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h1>PLANTRY 초대</h1>
                            </div>
                            <div class="content">
                                <p>안녕하세요,</p>
                                <p><strong>%s</strong> 폴더로 초대합니다!</p>
                                <p>새로운 협업과 공유의 기회가 열렸어요.</p>
                                <a href="%s" class="btn">초대 수락하기</a>
                                <p>링크가 작동하지 않으면 아래 URL을 브라우저에 복사하여 붙여넣으세요:</p>
                                <p style="word-break: break-all;">%s</p>
                            </div>
                            <div class="footer">
                                <p>&copy; 2024 PLANTRY. All rights reserved.</p>
                                <p>이 이메일은 자동으로 생성되었습니다.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.formatted(folder.getName(), link, link);

                sendEmail(
                        new String[]{invitation.getEmail()},
                        subject,
                        content,
                        null
                );
            }
        } catch (Exception e) {
            logger.error("이메일 전송 실패. 에러 메시지: {}", e.getMessage());
            logger.error("상세 에러: ", e);
            sendErrorNotification(e);
            throw new RuntimeException("이메일 전송에 실패했습니다.", e);
        }
    }
}
