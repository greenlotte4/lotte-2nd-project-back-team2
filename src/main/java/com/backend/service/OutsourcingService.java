package com.backend.service;

import com.backend.dto.request.admin.outsourcing.PostOutsourcingDto;
import com.backend.dto.response.admin.outsourcing.GetOutsourcingsDto;
import com.backend.entity.user.OutSourcing;
import com.backend.entity.user.User;
import com.backend.repository.UserRepository;
import com.backend.repository.outsourcing.OutsourcingRepository;
import com.backend.util.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutsourcingService {

    private final OutsourcingRepository outsourcingRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;

    public ResponseEntity<?> postOutsourcing(PostOutsourcingDto dto, String company) {
        LocalDate today = LocalDate.now();
        OutSourcing outSourcing = OutSourcing.builder()
                .size(dto.getSize())
                .hp(dto.getHp())
                .companyName(dto.getOutsourcingName())
                .endDate(dto.getEnd())
                .paymentDate(dto.getPayment())
                .startDate(today.toString())
                .company(company)
                .build();

        outsourcingRepository.save(outSourcing);
        List<User> users = new ArrayList<>();
        for(int i=0 ; i < dto.getSize(); i++ ){
            User user = User.builder()
                    .outsourcingId(outSourcing.getId())
                    .company(company)
                    .status(1)
                    .level(0)
                    .name(dto.getOutsourcingName()+i)
                    .uid(dto.getOutsourcingName()+i)
                    .pwd(bCryptPasswordEncoder.encode("15154548"))
                    .role(Role.OUTSOURCING)
                    .build();
            users.add(user);
        }
        userRepository.saveAll(users);

        return ResponseEntity.ok("외주업체 등록이 완료되었습니다.");
    }

    public Page<GetOutsourcingsDto> getOutsourcings(String company,int page) {
        Pageable pageable = PageRequest.of(page,5);
        Page<OutSourcing> outSourcings = outsourcingRepository.findAllByCompany(company,pageable);
        Page<GetOutsourcingsDto> dtos = outSourcings.map(OutSourcing::toGetOutsourcingDto);

        return dtos;
    }
}
