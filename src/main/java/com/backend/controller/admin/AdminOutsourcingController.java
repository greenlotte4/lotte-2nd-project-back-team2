package com.backend.controller.admin;

import com.backend.dto.request.admin.outsourcing.PostOutsourcingDto;
import com.backend.dto.response.admin.outsourcing.GetOutsourcingsDto;
import com.backend.service.OutsourcingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminOutsourcingController {

    private final OutsourcingService outsourcingService;

    @PostMapping("/outsourcing")
    public ResponseEntity<?> postOutsourcing (
            @RequestBody PostOutsourcingDto dto
            , HttpServletRequest req
            ){
        String company = "1246857";
        ResponseEntity<?> response = outsourcingService.postOutsourcing(dto,company);
        return response;
    }

    @GetMapping("/outsourcings")
    public ResponseEntity<?> getOutsourcings (
            @RequestParam(value = "page",defaultValue = "0") int page
    ){
        Map<String,Object> map = new HashMap<>();
        String company = "1246857";
        Page<GetOutsourcingsDto> dtos = outsourcingService.getOutsourcings(company,page);

        map.put("outsourcings", dtos.getContent());
        map.put("totalPages", dtos.getTotalPages());
        map.put("totalElements", dtos.getTotalElements());
        map.put("currentPage", dtos.getNumber());
        map.put("hasNextPage", dtos.hasNext());
        return ResponseEntity.ok(map);
    }
}
