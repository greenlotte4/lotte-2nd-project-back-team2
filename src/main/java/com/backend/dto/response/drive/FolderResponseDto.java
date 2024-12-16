package com.backend.dto.response.drive;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Builder
public class FolderResponseDto {
    private String uid;
    private List<FolderDto> folderDtoList;
    private List<FolderDto> shareFolderDtoList;
    private long size;
}
