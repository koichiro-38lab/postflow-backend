package com.example.backend.dto.post;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagSummaryDto {
    private Long id;
    private String name;
    private String slug;
}
