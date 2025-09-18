package com.example.backend.dto.post;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySummaryDto {
    private Long id;
    private String name;
    private String slug;
}
