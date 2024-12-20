package com.backend.entity.user;

import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.request.user.PostUserRegisterDTO;
import com.backend.dto.response.GetAdminUsersApprovalRespDto;
import com.backend.dto.response.GetAdminUsersDtailRespDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.dto.response.admin.user.GetGroupUsersDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.community.FavoriteBoard;
import com.backend.entity.group.GroupMapper;
import com.backend.util.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "user_status")
    private Integer status; // 상태

    @Column(name = "uid")
    private String uid; // 유아이디

    @Column(name = "pwd")
    private String pwd; // 비밀번호

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Role role; // 역할

    @Column(name = "level")
    private Integer level; //직급

    @Column(name = "grade")
    private Integer grade; // 결제등급 enum 변경

    @Column(name = "email")
    private String email; 

    @Column(name = "hp") // 010-5555-4444
    private String hp;

    @Column(name = "name")
    private String name;

    @Column(name = "city")
    private String addr1;

    @Column(name = "country")
    private String country;

    @Column(name = "address")
    private String addr2;

    @Column(name = "company")
    private String company;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "payment_id")
    private Long paymentId; // 결제정보 :: 카드아이디

    @Column(name = "payment_day")
    private String day;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @ToString.Exclude
    private List<GroupMapper> groupMappers;

    @Column(name = "create_at")
    @CreationTimestamp
    private LocalDateTime createAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "outsourcing_id")
    private Long outsourcingId;

    @Column(name = "profile_img_path")
    private String profileImgPath; // 프로필 이미지의 sName (파일명)

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @ToString.Exclude
    private List<Attendance> attendance;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @ToString.Exclude
    private List<CalendarMapper> calendars;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<FavoriteBoard> favoriteBoards = new ArrayList<>(); // 즐겨찾기 목록

    @Column(name="annual_vacation")
    @Builder.Default
    private Integer annualVacation = 15;

    @Column(name = "profile_message")
    private String profileMessage;

    public void updateUser(PostUserRegisterDTO dto) {
        this.email = dto.getEmail();
        this.hp = dto.getHp();
        this.name = dto.getName();
        this.addr1 = dto.getAddr1();
        this.country = dto.getCountry();
        this.addr2 = dto.getAddr2();
    }

    public String selectLevelString(){
        return switch (level) {
            case 0 -> "개인";
            case 1 -> "사원";
            case 2 -> "주임";
            case 3 -> "대리";
            case 4 -> "과장";
            case 5 -> "차장";
            case 6 -> "부장";
            case 7 -> "관리자"; //회사 관리자
            default -> "외주";
        };
    }

    public GetAdminUsersRespDto toGetAdminUsersRespDto() {
        return GetAdminUsersRespDto.builder()
                .email(email)
                .uid(uid)
                .id(id)
                .name(name)
                .build();
    }

    public GetAdminUsersDtailRespDto toGetAdminUsersDtailRespDto() {
        String yearMonth = this.todaysYearMonth();
        Attendance attendance1 = attendance.stream().filter(v->v.getYearMonth().equals(yearMonth)).findFirst().get();
        String todayAttendance;
        if(attendance1.getStatus()==1){
            todayAttendance = "결근";
        }else if(attendance1.getStatus()==2){
            todayAttendance = "출근";
        } else {
            todayAttendance = "퇴사";
        }
        return GetAdminUsersDtailRespDto.builder()
                .email(email)
                .level(this.selectLevelString())
                .attendance(todayAttendance)
                .createAt("아직 안뽑")
                .status(status)
                .name(name)
                .id(id)
                .build();
    }

    public GetAdminUsersApprovalRespDto toGetAdminUsersApprovalRespDto() {
        return GetAdminUsersApprovalRespDto.builder()
                .id(id)
                .createAt("아직 안뽑")
                .email(email)
                .name(name)
                .uid(uid)
                .build();
    }

    public void updateRole(Role role) {
        this.role = role;
    }

    public void patchUserApproval(PatchAdminUserApprovalDto dto) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date = LocalDate.parse(dto.getJoinDate(), formatter);
        this.status = 1;
        this.joinDate = date;
        this.level = dto.getLevel();
    }

    private String todaysYearMonth(){
        LocalDate date = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return date.format(formatter);
    }


    public UserDto toDto() {
        return UserDto.builder()
                .id(this.id)
                .status(this.status)
                .uid(this.uid)
                .pwd(null)
                .role(this.role)
                .level(this.level)
                .grade(this.grade)
                .email(this.email)
                .hp(this.hp)
                .name(this.name)
                .addr1(this.addr1)
                .country(this.country)
                .addr2(this.addr2)
                .company(this.company)
                .companyName(this.companyName)
                .paymentId(this.paymentId)
                .day(this.day)
//                .groupMappers(this.groupMappers)
                .profileImgPath(this.profileImgPath != null ? this.profileImgPath : "Default.png") // 기본값 설정
                .profileMessage(this.profileMessage)
                .createAt(this.createAt)
                .lastLogin(this.lastLogin)
                .joinDate(this.joinDate)
//                .attendance(this.attendance)
                .build();
    }

    public UserDto toSliceDto() {

        return UserDto.builder()
                .uid(this.uid)
                .grade(this.grade)
                .role(this.role)
                .company(this.company)
                .id(this.id)
                .build();
    }

    public GetUsersAllDto toGetUsersAllDto (){
        String group;
        List<GroupMapper> newGroupMappers = groupMappers.stream().filter(v->v.getGroup().getStatus() != 0 && v.getGroup().getType()==0).toList();
        if(!newGroupMappers.isEmpty()){
            group = newGroupMappers.get(0).getGroup().getName();
        } else {
            group = "소속없음";
        }
        return GetUsersAllDto.builder()
                .name(this.name)
                .email(this.email)
                .authority("아직빈칸")
                .group(group)
                .uid(this.uid)
                .id(this.id)
                .level(this.selectLevelString())
                .profile(this.profileImgPath)
                .build();
    }

    public GetUsersAllDto toGetUsersAllDto (String group){
        return GetUsersAllDto.builder()
                .name(this.name)
                .email(this.email)
                .authority("아직빈칸")
                .group(group)
                .uid(this.uid)
                .id(this.id)
                .level(this.selectLevelString())
                .profile(this.profileImgPath)
                .build();
    }

    public GetGroupUsersDto toGetGroupUsersDto (){
        String yearMonth = this.todaysYearMonth();
        Attendance attendance1 = attendance.stream().filter(v->v.getYearMonth().equals(yearMonth)).findFirst().get();
        String todayAttendance;
        if(attendance1.getStatus()==1){
            todayAttendance = "결근";
        }else if(attendance1.getStatus()==2){
            todayAttendance = "출근";
        } else {
            todayAttendance = "퇴사";
        }
        return GetGroupUsersDto.builder()
                .name(this.name)
                .state("미정")
                .attendance(todayAttendance)
                .level(this.selectLevelString())
                .createAt(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(createAt))
                .id(this.id)
                .build();
    }

    public void updateCompanyCode(String companyCode) {
        this.company = companyCode;
    }

    public void patchRole(Role role) {
        this.role = role;
    }

    public void updateLoginDate(LocalDateTime now) {
        this.lastLogin = now;
    }

    public void updateProfileImg(String profileImgPath) {
        this.profileImgPath = profileImgPath;
    }

    public void updateMessage(String message) { this.profileMessage = message; }

    public void updatePass(String pwd) {
        this.pwd = pwd;
    }

    public void updateStatus(int status) { this.status = status; }

}
