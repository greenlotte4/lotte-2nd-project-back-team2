package com.backend.controller;

import com.backend.dto.request.user.*;
import com.backend.dto.response.user.TermsDTO;
import com.backend.entity.user.CardInfo;
import com.backend.entity.user.User;
import com.backend.service.UserService;
import com.backend.util.Role;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.Console;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
public class RegisterController {

    private final UserService userService;

    @GetMapping("/terms")
    public ResponseEntity<?> termsList(){
        List<TermsDTO> termsDTOS = userService.getTermsAll();
        log.info("텀즈 컨트롤러 접속"+termsDTOS);
        if(termsDTOS.isEmpty()){
            return ResponseEntity.noContent().build();
        }else{
            return ResponseEntity.ok(termsDTOS);
        }
    }

    @PostMapping("/sendMail")
    public ResponseEntity<?> sendMail(@RequestBody EmailDTO emailDTO){
        String receiver = emailDTO.getEmail();
        Boolean result = userService.sendEmailCode(receiver);
        if(result){
            return ResponseEntity.ok().body("success");
        }else{
            return ResponseEntity.ok().body("fail");
        }
    }

    @PostMapping("/verifyMail")
    public ResponseEntity<?> verifyMailCode(@RequestBody EmailDTO emailDTO){
        String code = userService.getEmailCode(emailDTO);
        if(code.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        if(code.equals(emailDTO.getCode())){
            return ResponseEntity.ok().body("success");
        }else {
            return ResponseEntity.ok().body("notMatch");
        }
    }

    @PostMapping("/validation")
    public ResponseEntity<?> registerValidation(@RequestBody RegisterValidationDTO dto){
        String type = dto.getType();
        String value = dto.getValue();
        log.info("유효성검사 컨트롤러 "+dto);
        try {
            Boolean result = userService.registerValidation(value, type);
            if (result) {
                return ResponseEntity.ok("available");
            } else {
                return ResponseEntity.ok("unavailable");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@RequestBody RegisterDTO reqdto){
        try {

            PaymentInfoDTO paymentInfoDTO = reqdto.getPayment();
            PostUserRegisterDTO dto = reqdto.getUser();
            String now = LocalDate.now().toString();

            log.info("회원가입 컨트롤러 PaymentInfoDTO: {}", paymentInfoDTO);
            log.info("회원가입 컨트롤러 PostUserRegisterDTO: {}", dto);

            if (dto.getGrade() == 0) {
                Boolean companyResult = userService.validateCompany(dto.getCompany());
                if(companyResult){
                    dto.setRole(Role.WORKER);
                }else{
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("not found company");
                }
            }

            CardInfo cardInfo = null;
            if (dto.getGrade() == 3 || dto.getGrade() == 2) {
                dto.setRole(dto.getGrade() == 3 ? Role.COMPANY : Role.USER);
                dto.setDay(now);
                paymentInfoDTO.setActiveStatus(1);
                cardInfo = userService.insertPayment(paymentInfoDTO);
            }

            if (cardInfo != null) {
                log.info("카드 엔티티: {}", cardInfo);
                dto.setPaymentId(cardInfo.getCardId());
            }

            Long userId = userService.insertUser(dto);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not Create");
            }

            if (cardInfo != null) {
                cardInfo.updateUserid(userId);
            }
            return ResponseEntity.ok().body("success");
        } catch (Exception e) {
            log.error("회원가입 처리 중 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("server error");
        }
    }
}
