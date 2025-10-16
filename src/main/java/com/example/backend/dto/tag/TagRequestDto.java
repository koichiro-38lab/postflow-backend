package com.example.backend.dto.tag;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagRequestDto {
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "^[\\w\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FFF\\uAC00-\\uD7AF\\uFF00-\\uFFEF\\s\\p{Punct}/]+$", message = "Tag name contains invalid characters")
    private String name;

    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tag slug must be lowercase alphanumeric or hyphen (a-z0-9-)")
    private String slug;
}
