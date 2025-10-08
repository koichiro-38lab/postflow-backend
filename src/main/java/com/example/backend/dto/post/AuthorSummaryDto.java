package com.example.backend.dto.post;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorSummaryDto {
    private Long id;
    private String displayName;
}
