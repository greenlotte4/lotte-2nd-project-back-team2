package com.backend.repository;

import com.backend.entity.group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByName(String name);

    List<Group> findAllByTypeAndStatus(int i, int i1);

    Page<Group> findAllByCompanyAndStatusIsNot(String number, int i, Pageable pageable);

    Page<Group> findAllByCompanyAndNameContainingAndStatusIsNot(String number, String keyword, int i, Pageable pageable);

    Optional<Group> findByIdAndStatusIsNot(Long id, int i);

    Optional<Group> findByNameAndStatusIsNotAndCompany(String name, int i, String company);
}
