package com.backend.dto.request.page;


import com.backend.document.page.Page;
import com.backend.util.PermissionType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class PageDto {
    private String id;
    private String title;
    private String content;
    private String ownerUid;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String permissions;


    public Page ToEntity() {
        Page page = null;

            page = Page.builder()
                    .id(id)
                    .title(title)
                    .content(content) // JSON 객체를 문자열로 변환
                    .ownerUid(ownerUid)
                    .createAt(createAt)
                    .updateAt(updateAt)
                    .build();

        return page;
    }

}
