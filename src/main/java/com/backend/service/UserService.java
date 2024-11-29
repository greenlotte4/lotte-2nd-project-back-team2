package com.backend.service;

import com.backend.dto.request.admin.user.PatchAdminUserApprovalDto;
import com.backend.dto.response.GetAdminUsersRespDto;
import com.backend.dto.response.UserDto;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.util.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

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
    public List<GetAdminUsersRespDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(User::toGetAdminUsersRespDto).toList();
    }

}
