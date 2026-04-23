package com.bookle.wordbook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WordRequest(
    @NotBlank @Size(max = 100) String word,
    @NotBlank String meaning,
    String example,
    Long bookId
) {
}
