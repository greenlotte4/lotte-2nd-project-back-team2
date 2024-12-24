package com.backend.dto.request.drive;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class MoveFolderRequest {
    private String folderId;
    private String targetFolderId;
    private double order;
    private double currentOrder;
    private String position;
    private String fileId;




}
