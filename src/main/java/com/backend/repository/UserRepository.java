package com.backend.repository;

import com.backend.entity.user.User;
import com.backend.util.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUid(String uid);

    List<User> findAllByRoleIsNot(Role role);

    List<User> findAllByRole(Role role);

    List<User> findAllByStatus(int i);

    Page<User> findAllByCompany(String number, Pageable pageable);

    Page<User> findAllByCompanyAndNameContaining(String number, String keyword, Pageable pageable);

    Page<User> findAllByCompanyAndStatusIsNot(String number, int i, Pageable pageable);

    Page<User> findAllByCompanyAndNameContainingAndStatusIsNot(String number, String keyword, int i, Pageable pageable);

    Page<User> findAllByCompanyAndNameContainingAndStatusIsNotAndGroupMappers_Group_Id(String number, String keyword, int i, Long id, Pageable pageable);

    Page<User> findAllByCompanyAndStatusIsNotAndGroupMappers_Group_Id(String number, int i, Long id, Pageable pageable);

    Optional<User> findByEmail(String email);

    Optional<User> findByHp(String hp);

    Page<User> findAllByCompanyAndStatusIsNotOrderByLevelDesc(String number, int i, Pageable pageable);

    Page<User> findAllByCompanyAndNameContainingAndStatusIsNotOrderByLevelDesc(String number, String keyword, int i, Pageable pageable);

    Page<User> findAllByCompanyAndNameContainingAndStatusIsNotAndGroupMappers_Group_IdOrderByLevelDesc(String number, String keyword, int i, Long id, Pageable pageable);

    Page<User> findAllByCompanyAndStatusIsNotAndGroupMappers_Group_IdOrderByLevelDesc(String number, int i, Long id, Pageable pageable);

    Long countByCompany(String company);
}
