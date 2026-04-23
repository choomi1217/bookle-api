package com.bookle.wordbook.dto;

import java.util.List;

public record BookContentResponse(
    Long id,
    String title,
    List<String> sentences
) {
}
