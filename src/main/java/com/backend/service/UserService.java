package com.backend.service;
import com.backend.dto.chat.UsersWithGroupNameDTO;
import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.request.user.EmailDTO;
import com.backend.dto.request.user.PaymentInfoDTO;
import com.backend.dto.request.user.PostUserRegisterDTO;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.admin.user.GetGroupUsersDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.dto.response.user.TermsDTO;
import com.backend.entity.group.Group;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.CardInfo;
import com.backend.entity.user.Terms;
import com.backend.entity.user.User;
import com.backend.repository.GroupMapperRepository;
import com.backend.repository.GroupRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.user.CardInfoRepository;
import com.backend.repository.user.TermsRepository;
import com.backend.util.Role;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    @Value("${spring.mail.username}")
    private String sender;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMapperRepository groupMapperRepository;
    private final TermsRepository termsRepository;
    private final CardInfoRepository cardInfoRepository;
    private final JavaMailSenderImpl mailSender;
    private final PasswordEncoder passwordEncoder;
    @Autowired
    private final RedisTemplate<String, String> redisTemplate;

    public List<GetAdminUsersRespDto> getUserNotTeamLeader() {
        List<User> users = userRepository.findAllByRole(Role.WORKER);
        return users.stream().map(User::toGetAdminUsersRespDto).toList();
    }

    public ResponseEntity<?> patchUserApproval(PatchAdminUserApprovalDto dto) {
        Optional<User> user = userRepository.findById(dto.getUserId());
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("해당 유저가 회원가입을 취소했습니다.");
        }
        user.get().patchUserApproval(dto);

        return ResponseEntity.ok().body("승인처리하였습니다.");
    }


    public User getUserByuid(String uid){
        Optional<User> user = userRepository.findByUid(uid);
        if(user.isEmpty()){
            return null;
        }

        User findUser = user.get();

        return findUser;
    }

    // 11.29 전규찬 전체 사용자 조회 기능 추가
    public List<UsersWithGroupNameDTO> getAllUsersWithGroupName() {
        List<GroupMapper> groupMappers = groupMapperRepository.findAll();
        List<UsersWithGroupNameDTO> usersWithGroupNameDTOs = new ArrayList<>();
        for(GroupMapper groupMapper : groupMappers){
            UsersWithGroupNameDTO dto = new UsersWithGroupNameDTO();

            User user = groupMapper.getUser();
            Group group = groupMapper.getGroup();

            dto.setId(user.getId());
            dto.setUid(user.getUid());
            dto.setName(user.getName());
            dto.setEmail(user.getEmail());
            dto.setGroupName(group.getName());
            usersWithGroupNameDTOs.add(dto);
        }
        return usersWithGroupNameDTOs;
    }

    public Page<GetUsersAllDto> getUsersAll(int page) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> users = userRepository.findAllByCompanyAndStatusIsNotOrderByLevelDesc("1246857",0,pageable);
        Page<GetUsersAllDto> dtos = users.map(User::toGetUsersAllDto);
        return dtos;
    }

    public Page<GetUsersAllDto> getUsersAllByKeyword(int page,String keyword) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> users = userRepository.findAllByCompanyAndNameContainingAndStatusIsNotOrderByLevelDesc("1246857",keyword,0,pageable);
        Page<GetUsersAllDto> dtos = users.map(User::toGetUsersAllDto);
        return dtos;
    }

    public Page<GetUsersAllDto> getUsersAllByKeywordAndGroup(int page, String keyword, Long id) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> users = userRepository.findAllByCompanyAndNameContainingAndStatusIsNotAndGroupMappers_Group_IdOrderByLevelDesc("1246857",keyword,0,id,pageable);
        Optional<Group> group = groupRepository.findById(id);
        String groupName = group.get().getName();
        if(groupName.isEmpty()){
            return null;
        }
        Page<GetUsersAllDto> dtos = users.map(v->v.toGetUsersAllDto(groupName));
        return dtos;
    }

    public Page<GetUsersAllDto> getUsersAllByGroup(int page, Long id) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> users = userRepository.findAllByCompanyAndStatusIsNotAndGroupMappers_Group_IdOrderByLevelDesc("1246857",0,id,pageable);
        Optional<Group> group = groupRepository.findById(id);
        String groupName = group.get().getName();
        if(groupName.isEmpty()){
            return null;
        }
        Page<GetUsersAllDto> dtos = users.map(v->v.toGetUsersAllDto(groupName));
        return dtos;
    }

    public List<TermsDTO> getTermsAll() {
        List<Terms> termsList = termsRepository.findAll();
        List<TermsDTO> termsDTOS = termsList.stream()
                .map(terms -> TermsDTO.builder()
                                    .id(terms.getId())
                                    .title(terms.getTitle())
                                    .content(terms.getContent())
                                    .necessary(terms.getNecessary())
                                    .build())
                .toList();
        return termsDTOS;
    }

    public void insertUser(PostUserRegisterDTO dto) {
        String encodedPwd = passwordEncoder.encode(dto.getPwd());

        User entity = User.builder()
                            .uid(dto.getUid())
                            .pwd(encodedPwd)
                            .role(dto.getRole())
                            .grade(dto.getGrade())
                            .email(dto.getEmail())
                            .hp(dto.getHp())
                            .name(dto.getName())
                            .addr1(dto.getAddr1())
                            .country(dto.getCountry())
                            .addr2(dto.getAddr2())
                .status(1)
                .day(dto.getDay())
                .company(dto.getCompany())
                .companyName(dto.getCompanyName())
                            .build();

        User user = userRepository.save(entity);

        if(user.getGrade() == 3 ){
            String companyCode = this.makeRandomCode(10);
            user.updateCompanyCode(companyCode);
        }
    }

    private String makeRandomCode(int length) {
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"; // 문자
        String numbers = "0123456789"; // 숫자
        StringBuilder code = new StringBuilder(length); // 결과를 저장할 StringBuilder
        Random random = new Random(); // 랜덤 생성기

        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) { // 짝수 인덱스는 문자 선택
                int letterIndex = random.nextInt(letters.length());
                code.append(letters.charAt(letterIndex));
            } else { // 홀수 인덱스는 숫자 선택
                int numberIndex = random.nextInt(numbers.length());
                code.append(numbers.charAt(numberIndex));
            }
        }
        return code.toString(); // 생성된 코드 반환
    }


    public Long insertPayment(PaymentInfoDTO paymentInfoDTO) {
        CardInfo entity = CardInfo.builder()
                                .activeStatus(paymentInfoDTO.getActiveStatus())
                                .paymentCardNo(paymentInfoDTO.getPaymentCardNo())
                                .paymentCardNick(paymentInfoDTO.getPaymentCardNick())
                                .paymentCardExpiration(paymentInfoDTO.getPaymentCardExpiration())
                                .paymentCardCvc(paymentInfoDTO.getPaymentCardCvc())
                                .build();
        CardInfo cardInfo = cardInfoRepository.save(entity);
        return cardInfo.getCardId();
    }


    public Boolean sendEmailCode( String receiver){
        // MimeMessage 생성
        MimeMessage message = mailSender.createMimeMessage();

        // 인증코드 생성 후 세션 저장
        String code = makeRandomCode(6);
        log.info("인증코드 만듦 "+code);

        // Redis에 저장
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
//        String redisKey = receiver; // Redis 키 생성
        valueOperations.set(receiver, code, 5, TimeUnit.MINUTES); // 5분 동안 저장
        log.info("인증코드 Redis에 저장: key={}, value={}", receiver, code);

        String title = "Plantry에서 보낸 인증코드를 확인하세요.";
        String content = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "<meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<title>Email Verification</title>" +
                "</head>" +
                "<body style=\"font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5;\">" +
                "    <div style=\"max-width: 600px; margin: 20px auto; background: #ffffff; border: 1px solid #ddd; border-radius: 10px; overflow: hidden;\">" +
                "        <div style=\"background-color: #8589FF; padding: 20px; text-align: center; color: white;\">" +
                "            <h1 style=\"margin: 0; font-size: 24px;\">이메일 인증코드 확인</h1>" +
                "        </div>" +
                "        <div style=\"padding: 20px; text-align: center;\">" +
                "            <p style=\"color: #333333; font-size: 16px;\">Plantry 서비스 이용을 위한 인증코드입니다.</p>" +
                "            <p style=\"color: #333333; font-size: 16px;\">아래 인증코드를 입력하여 인증을 완료하세요.</p>" +
                "            <div style=\"font-size: 32px; color: #6366F1; background: #DEDEE6; display: inline-block; padding: 10px 20px; border-radius: 8px; margin: 20px 0;\">" + code + "</div>" +
                "        </div>" +
                "        <div style=\"background: #f5f5f5; padding: 10px; text-align: center; font-size: 12px; color: #666666;\">" +
                "            <p>&copy; 2024 Plantry. All rights reserved.</p>" +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";

        try {
            message.setFrom(new InternetAddress(sender, "보내는 사람", "UTF-8"));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
            message.setSubject(title);
            message.setContent(content, "text/html;charset=UTF-8");

            // 메일 발송
            mailSender.send(message);
            return true;
        }catch(Exception e){
            log.error("sendEmailConde : " + e.getMessage());
            return false;
        }
    }

    public String getEmailCode(EmailDTO emailDTO) {
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String redisKey = emailDTO.getEmail(); // Redis 키 생성
        return valueOperations.get(redisKey);
    }

    public Boolean registerValidation(String value, String type) {
        Optional<User> optUser = Optional.empty();
        switch (type) {
            case "email":
                optUser = userRepository.findByEmail(value);
                break;
            case "hp":
                optUser = userRepository.findByHp(value);
                break;
            case "uid":
                optUser = userRepository.findByUid(value);
                break;
            default:
                log.warn("유효하지 않은 타입: {}", type);
                throw new IllegalArgumentException("유효하지 않은 타입입니다: " + type);
        }
        if(optUser.isPresent()) {
            log.info("유효성검사 데이터 잘 뽑히는지 확인 "+optUser);
            return false;
        }
        log.info("유효성검사 데이터 없는 거"+optUser);
        return true;
    }

    public ResponseEntity<?> getALlUsersCnt(String company) {
        Long cnt = userRepository.countByCompany(company);
        if(cnt >0){
            return ResponseEntity.ok(cnt);
        }
        return ResponseEntity.ok(0L);
    }

    public Page<GetGroupUsersDto> getAdminUsersAllByKeyword(int page, String keyword) {
        return null;
    }

    public Page<GetGroupUsersDto> getAdminUsersAllByKeywordAndGroup(int page, String keyword, Long id) {
        return null;
    }

    public Page<GetGroupUsersDto> getAdminUsersAllByGroup(int page, Long id) {
        return null;
    }

    public Page<GetGroupUsersDto> getAdminUsersAll(int page) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> users = userRepository.findAllByCompanyAndStatusIsNotOrderByLevelDesc("1246857",0,pageable);
        Page<GetGroupUsersDto> dtos = users.map(User::toGetGroupUsersDto);
        return dtos;
    }

    // 12.06 사용자 아이디로 소속 부서 id 추출
    public List<Group> getGroupsByUserUid(String uid) {
        log.info("내 아이디는 받아오나"+uid);
        return groupMapperRepository.findGroupsByUserUid(uid);
    }
}
