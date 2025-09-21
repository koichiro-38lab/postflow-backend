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
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Tag name must be alphanumeric or hyphen (A-Za-z0-9-)")
    private String name;

    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tag slug must be lowercase alphanumeric or hyphen (a-z0-9-)")
    private String slug;
}
