package com.backend.dto.request.drive;


import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class DeletedRequest {

    private List<String> folders;
    private List<String> files;
}
