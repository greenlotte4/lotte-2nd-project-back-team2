package com.backend.dto.response.drive;


import lombok.*;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewNameResponseDto {
    private String folderName;
    private String folderUUID;
    private String Path;
}
