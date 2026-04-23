package com.bookle.wordbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiWordRequest(
    @NotBlank @Size(max = 100) String word
) {
}
