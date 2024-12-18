package com.backend.dto.request.drive;


import com.backend.dto.request.FileRequestDto;
import com.backend.dto.response.drive.FolderDto;
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
    private List<FolderDto> subFolders;
    private List<FileRequestDto> fileDtos;


}
