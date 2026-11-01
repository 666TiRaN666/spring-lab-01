package kz.iitu.springlab.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.iitu.springlab.web.Genre;

public record BookRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank String author,
        @Min(1450) @Max(2100) int year,
        @NotNull Genre genre) {
}
