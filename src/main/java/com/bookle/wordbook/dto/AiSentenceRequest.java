package com.bookle.wordbook.dto;

import jakarta.validation.constraints.NotBlank;

public record AiSentenceRequest(
    @NotBlank String sentence
) {
}
