package com.backend.repository.outsourcing;

import com.backend.entity.user.OutSourcing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutsourcingRepository extends JpaRepository<OutSourcing,Long> {
    Page<OutSourcing> findAllByCompany(String company, Pageable pageable);
    List<OutSourcing> findAllByCompany(String company);
}
