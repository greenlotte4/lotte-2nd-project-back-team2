package com.backend.service;
import com.backend.document.chat.ChatMemberDocument;
import com.backend.dto.chat.UsersWithGroupNameDTO;
import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.request.user.*;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.dto.response.admin.user.GetGroupUsersDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.dto.response.user.RespCardInfoDTO;
import com.backend.dto.response.user.TermsDTO;
import com.backend.entity.group.Group;
import com.backend.entity.group.GroupMapper;
import com.backend.entity.user.*;
import com.backend.repository.GroupMapperRepository;
import com.backend.repository.GroupRepository;
import com.backend.repository.UserRepository;
import com.backend.repository.chat.ChatMemberRepository;
import com.backend.repository.user.*;
import com.backend.util.Role;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    @Value("${spring.mail.username}")
    private String sender;

    private final ProfileImgRepository profileImgRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMapperRepository groupMapperRepository;
    private final TermsRepository termsRepository;
    private final CardInfoRepository cardInfoRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final JavaMailSenderImpl mailSender;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;
    private final AlertRepository alertRepository;
    private final SftpService sftpService;
//    private final FolderService folderService;

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

    public Page<GetUsersAllDto> getUsersAll(int page,String company) {
        Pageable pageable = PageRequest.of(page, 5);
        if(company == null || company.isEmpty()){
            return null;
        }
        Page<User> users = userRepository.findAllByCompanyAndStatusIsNotOrderByLevelDesc(company,0,pageable);
        Page<GetUsersAllDto> dtos = users.map(User::toGetUsersAllDto);
        return dtos;
    }

    public Page<GetUsersAllDto> getUsersAllByKeyword(int page,String keyword) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<User> users = userRepository.findAllByCompanyAndNameContainingAndStatusIsNotOrderByLevelDesc("1246857",keyword,0,pageable);
        Page<GetUsersAllDto> dtos = users.map(User::toGetUsersAllDto);
        return dtos;
    }

    public Page<GetUsersAllDto> getUsersAllByKeywordAndGroup(int page, String keyword, Long id,String company) {
        Pageable pageable = PageRequest.of(page, 5);
        if(company == null || company.isEmpty()){
            return null;
        }
        Page<User> users = userRepository.findAllByCompanyAndNameContainingAndStatusIsNotAndGroupMappers_Group_IdOrderByLevelDesc(company,keyword,0,id,pageable);
        Optional<Group> group = groupRepository.findById(id);
        String groupName = group.get().getName();
        if(groupName.isEmpty()){
            return null;
        }
        Page<GetUsersAllDto> dtos = users.map(v->v.toGetUsersAllDto(groupName));
        return dtos;
    }

    public Page<GetUsersAllDto> getUsersAllByGroup(int page, Long id,String company) {
        Pageable pageable = PageRequest.of(page, 5);
        if(company == null || company.isEmpty()){
            return null;
        }
        Page<User> users = userRepository.findAllByCompanyAndStatusIsNotAndGroupMappers_Group_IdOrderByLevelDesc(company,0,id,pageable);
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

    public User insertUser(PostUserRegisterDTO dto) {
        String encodedPwd = passwordEncoder.encode(dto.getPwd());
        if(dto.getGrade() == 3 ){
            String companyCode = this.makeRandomCode(10);
            dto.setCompany(companyCode);
            log.info("여기 안 들어오니? "+companyCode);
        }
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
                            .level(dto.getLevel())
                            .day(dto.getDay())
                            .company(dto.getCompany())
                            .companyName(dto.getCompanyName())
                            .paymentId(dto.getPaymentId())
                            .build();

        User user = userRepository.save(entity);

        if(user == null){
            log.info("유저가 없나? "+user);
            return null;
        }
        return user;
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


    public CardInfo insertPayment(PaymentInfoDTO paymentInfoDTO) {
        CardInfo entity = CardInfo.builder()
                                .activeStatus(paymentInfoDTO.getActiveStatus())
                                .paymentCardNo(paymentInfoDTO.getPaymentCardNo())
                                .paymentCardNick(paymentInfoDTO.getPaymentCardNick())
                                .paymentCardExpiration(paymentInfoDTO.getPaymentCardExpiration())
                                .paymentCardCvc(paymentInfoDTO.getPaymentCardCvc())
                                .cardCompany(paymentInfoDTO.getCardCompany())
                                .globalPayment(paymentInfoDTO.getGlobalPayment())
                                .autoPayment(paymentInfoDTO.getAutoPayment())
                                .activeStatus(paymentInfoDTO.getActiveStatus())
                                .build();
        CardInfo cardInfo = cardInfoRepository.save(entity);
        return cardInfo;
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

    public Boolean validateCompany(String company) {
        Page<User> user = userRepository.findAllByCompany(company, Pageable.unpaged());
        if(user.isEmpty()){
            return false;
        }else {
            return true;
        }
    }


    public ResponseEntity<?> postAlert(PostUserAlarmDto dto, Long id) {
        Optional<User> user = userRepository.findById(id);
        if(user.isEmpty()){
            return ResponseEntity.badRequest().body("로그인 정보가 일치하지않습니다...");
        }
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedNow = now.format(formatter);
        Alert alert = Alert.builder()
                .user(user.get())
                .title(dto.getTitle())
                .status(2)
                .content(dto.getContent())
                .createAt(formattedNow)
                .type(1)
                .build();

        alertRepository.save(alert);

        return ResponseEntity.ok("성공");
    }


    public long findGroupByUserUid(String uid) {
        Optional<GroupMapper> opt = groupMapperRepository.findGroupByUserUid(uid);
        if(opt.isPresent()){
            return opt.get().getGroup().getId();
        }
        return 0;
    }



    public UserDto getMyUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));
        Group group = user.getGroupMappers().stream()
                .map(GroupMapper::getGroup)
                .findFirst()
                .orElse(null);
        String userlevel = user.selectLevelString();
        UserDto userDto= user.toDto();
        userDto.setLevelString(userlevel);
        if(group != null){
            String department = group.getName();
            userDto.setDepartment(department);
        }
        return userDto;
    }

    public Boolean uploadProfile(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));
        log.info("프로필 업로드 유저 정보 조회 {}", user);

        String remoteDir = "uploads/profilImg";
        String originalFilename = file.getOriginalFilename();
        String savedFilename = generateSavedName(originalFilename);
        String path = remoteDir + "/" + savedFilename;
        log.info("경로 확인 {}", path);

        // 기존 프로필 이미지 삭제 경로 확인
        String deletePath = user.getProfileImgPath() != null
                ? remoteDir + "/" + user.getProfileImgPath()
                : null;

        // 임시 파일 생성 및 SFTP 업로드
        File tempFile = null;
        try {
            tempFile = File.createTempFile("upload_", "_" + originalFilename);
            file.transferTo(tempFile);
            log.info("임시 파일 생성: {}", tempFile.getAbsolutePath());

            // SFTP 업로드
            sftpService.uploadFile(tempFile.getAbsolutePath(), remoteDir, savedFilename);

            // 기존 프로필 이미지 삭제
            if (deletePath != null) {
                sftpService.delete(deletePath);
                log.info("기존 프로필 이미지 삭제 완료: {}", deletePath);
                profileImgRepository.deleteByUserId(user.getId());
            }

            // 새 프로필 이미지 정보 저장
            ProfileImg profileImg = ProfileImg.builder()
                    .userId(userId)
                    .path(path)
                    .status(1)
                    .rName(originalFilename)
                    .sName(savedFilename)
                    .message(null)
                    .build();
            profileImgRepository.save(profileImg);

            // 유저 엔티티에 프로필 이미지 경로 업데이트
            user.updateProfileImg(savedFilename);
            userRepository.save(user);

            // 12.19 채팅용 유저에 프로필 이미지 경로 저장
            ChatMemberDocument chatMemberDocument = chatMemberRepository.findByUid(user.getUid());
            chatMemberDocument.setProfileUrl(path);
            chatMemberDocument.setProfileSName(savedFilename);
            chatMemberRepository.save(chatMemberDocument);

            log.info("프로필 업로드 및 저장 완료: {}", profileImg);
            return true;
        } catch (Exception e) {
            log.error("프로필 업로드 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("프로필 업로드에 실패했습니다.", e);
        } finally {
            if (tempFile != null && tempFile.exists() && tempFile.delete()) {
                log.info("임시 파일 삭제 성공: {}", tempFile.getAbsolutePath());
            }
        }
    }


    public String generateSavedName(String originalName) {
        // Validate input
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("Original file name cannot be null or empty");
        }

        // Extract file extension
        String extension = "";
        int dotIndex = originalName.lastIndexOf(".");
        if (dotIndex > 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex);
        }

        // Generate UUID and append extension
        String uuid = UUID.randomUUID().toString();
        return uuid + extension;
    }

    public UserDto getSliceUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("유저 정보를 찾을 수 없습니다."));
        Group group = user.getGroupMappers().stream()
                .map(GroupMapper::getGroup)
                .findFirst()
                .orElse(null);
        UserDto userDto= user.toSliceDto();
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setProfileImgPath("나중에 url");
        if(group != null){
            String department = group.getName();
            long groupId = group.getId();
            userDto.setDepartment(department);
            userDto.setGroupId(groupId);

        }
        return userDto;
    }


    public ResponseEntity<?> updateMessage(Long userId, String message) {
        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isPresent()){
            User user = optUser.get();
            user.updateMessage(message);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다. 다시 시도해 주세요.");
        }
    }

    public ResponseEntity<?> updateUser(Long userId, PostUserRegisterDTO dto) {
        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isPresent()){
            User user = optUser.get();
            user.updateUser(dto);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("유저 정보를 찾을 수 없습니다.");
        }
    }

    public ResponseEntity<?> confirmPass(Long userId, String pwd) {
        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        if(passwordEncoder.matches(pwd, optUser.get().getPwd())){
            return ResponseEntity.ok().build();
        }else{
            return ResponseEntity.status(HttpStatus.CONFLICT).body("비밀번호 안 맞음");
        }
    }

    public ResponseEntity<?> updatePass(Long userId, String pwd) {
        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }else{
            User user = optUser.get();
            String encodedPwd = passwordEncoder.encode(pwd);
            log.info("인코딩 패스워드 "+encodedPwd);
            user.updatePass(encodedPwd);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
    }

    public ResponseEntity<?> getCardInfo(Long userId) {
        List<CardInfo> cardInfos = cardInfoRepository.findAllByUserId(userId);
        if(cardInfos.isEmpty()){
            return ResponseEntity.ok().body("카드 정보가 없습니다.");
        }
        List<RespCardInfoDTO> dtos = cardInfos.stream().map( v -> {
            RespCardInfoDTO dto = v.toDto();
            return dto;
        }).toList();
        return ResponseEntity.ok().body(dtos);
    }

    public ResponseEntity<?> deletePayment(Long paymentId) {
        try {
            cardInfoRepository.deleteById(paymentId);
            return ResponseEntity.ok().build();
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("결제정보 삭제 중 오류가 발생했습니다.");
        }
    }

    public ResponseEntity addPayment(PaymentInfoDTO paymentInfoDTO, Long userId) {
        CardInfo entity = CardInfo.builder()
                .activeStatus(paymentInfoDTO.getActiveStatus())
                .paymentCardNo(paymentInfoDTO.getPaymentCardNo())
                .paymentCardNick(paymentInfoDTO.getPaymentCardNick())
                .paymentCardExpiration(paymentInfoDTO.getPaymentCardExpiration())
                .paymentCardCvc(paymentInfoDTO.getPaymentCardCvc())
                .cardCompany(paymentInfoDTO.getCardCompany())
                .globalPayment(paymentInfoDTO.getGlobalPayment())
                .autoPayment(paymentInfoDTO.getAutoPayment())
                .activeStatus(paymentInfoDTO.getActiveStatus())
                .userId(userId)
                .build();
        CardInfo cardInfo = cardInfoRepository.save(entity);
        if(cardInfo != null) {
            return ResponseEntity.ok().body("저장 성공");
        }else{
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    public ResponseEntity<?> deleteAccount(Long userId) {
        Optional<User> optUser = userRepository.findById(userId);
        if(optUser.isEmpty()){
            return ResponseEntity.notFound().build();
        }else{
            User user = optUser.get();
            user.updateStatus(0);
            userRepository.save(user);
            return ResponseEntity.ok().build();
        }
    }
}
