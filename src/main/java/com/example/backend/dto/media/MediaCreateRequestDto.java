package com.example.backend.dto.media;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaCreateRequestDto {

    @NotBlank
    @Size(max = 255)
    private String filename;

    @NotBlank
    @Size(max = 255)
    private String storageKey;

    @NotBlank
    @Size(max = 100)
    private String mime;

    @NotNull
    @Min(1)
    private Long bytes;

    @Min(1)
    private Integer width;

    @Min(1)
    private Integer height;

    @Size(max = 255)
    private String altText;
}
