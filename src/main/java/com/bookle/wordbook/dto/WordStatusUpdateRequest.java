package com.bookle.wordbook.dto;

import com.bookle.wordbook.domain.WordStatus;
import jakarta.validation.constraints.NotNull;

public record WordStatusUpdateRequest(
    @NotNull WordStatus status
) {
}
