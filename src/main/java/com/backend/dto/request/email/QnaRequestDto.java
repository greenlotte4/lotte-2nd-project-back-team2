package com.backend.dto.request.email;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
public class QnaRequestDto {
    private String title;
    private String name;
    private String email;
    private String category;
    private String priority;
    private String content;
    private MultipartFile attachments;
} 