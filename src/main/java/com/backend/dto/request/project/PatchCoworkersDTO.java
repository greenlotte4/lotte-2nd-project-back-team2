package com.backend.dto.request.project;

import lombok.*;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchCoworkersDTO {
    private Long projectId;
    private Set<Long> addedCoworkers;
    private Set<Long> removedCoworkers;
}
