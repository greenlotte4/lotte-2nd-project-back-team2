package com.backend.entity.user;

import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.response.GetAdminUsersApprovalRespDto;
import com.backend.dto.response.GetAdminUsersDtailRespDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.dto.response.user.GetUsersAllDto;
import com.backend.entity.calendar.CalendarMapper;
import com.backend.entity.group.GroupMapper;
import com.backend.util.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Column(name = "hp")
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

    @Column(name = "company_code")
    private String companyCode;

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

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "user")
    private ProfileImg profileImg;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @ToString.Exclude
    private List<Attendance> attendance;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "user")
    @ToString.Exclude
    private List<CalendarMapper> calendars;

    public GetAdminUsersRespDto toGetAdminUsersRespDto() {
        return GetAdminUsersRespDto.builder()
                .email(email)
                .uid(uid)
                .id(id)
                .name(name)
                .build();
    }

    public GetAdminUsersDtailRespDto toGetAdminUsersDtailRespDto() {
        String levelString;
        switch (level) {
            case 1:
                levelString = "사원";
                break;
            case 2:
                levelString = "주임";
                break;
            case 3:
                levelString = "대리";
                break;
            case 4:
                levelString = "과장";
                break;
            case 5:
                levelString = "차장";
                break;
            case 6:
                levelString = "부장";
                break;
            default:
                levelString = "Unknown";  // Handle unexpected levels
                break;
        }
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
                .level(levelString)
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
                .pwd(this.pwd)
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
                .paymentId(this.paymentId)
                .day(this.day)
                .groupMappers(this.groupMappers)
                .profileImg(this.profileImg != null ? this.profileImg.getSName() : "default.png") // 기본값 설정
                .createAt(this.createAt)
                .lastLogin(this.lastLogin)
                .joinDate(this.joinDate)
                .attendance(this.attendance)
                .build();
    }

    public UserDto toSliceDto() {

        return UserDto.builder()
                .uid(this.uid)
                .grade(this.grade)
                .role(this.role)
                .id(this.id)
                .build();
    }

    public GetUsersAllDto toGetUsersAllDto (){
        String levelString;
        switch (level) {
            case 1:
                levelString = "사원";
                break;
            case 2:
                levelString = "주임";
                break;
            case 3:
                levelString = "대리";
                break;
            case 4:
                levelString = "과장";
                break;
            case 5:
                levelString = "차장";
                break;
            case 6:
                levelString = "부장";
                break;
            default:
                levelString = "외주";  // Handle unexpected levels
                break;
        }
        String group;
        if(!groupMappers.isEmpty()){
            group = groupMappers.get(0).getGroup().getName();
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
                .level(levelString)
                .build();
    }

    public GetUsersAllDto toGetUsersAllDto (String group){
        String levelString;
        switch (level) {
            case 1:
                levelString = "사원";
                break;
            case 2:
                levelString = "주임";
                break;
            case 3:
                levelString = "대리";
                break;
            case 4:
                levelString = "과장";
                break;
            case 5:
                levelString = "차장";
                break;
            case 6:
                levelString = "부장";
                break;
            default:
                levelString = "외주";  // Handle unexpected levels
                break;
        }

        return GetUsersAllDto.builder()
                .name(this.name)
                .email(this.email)
                .authority("아직빈칸")
                .group(group)
                .uid(this.uid)
                .id(this.id)
                .level(levelString)
                .build();
    }
    public void updateCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }
}
