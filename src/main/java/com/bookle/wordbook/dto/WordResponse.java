package com.bookle.wordbook.dto;

import com.bookle.wordbook.domain.Word;
import com.bookle.wordbook.domain.WordStatus;
import java.time.Instant;

public record WordResponse(
    Long id,
    String word,
    String meaning,
    String example,
    WordStatus status,
    Long bookId,
    Instant createdAt
) {
    public static WordResponse from(Word word) {
        return new WordResponse(
            word.getId(),
            word.getWord(),
            word.getMeaning(),
            word.getExample(),
            word.getStatus(),
            word.getBook() != null ? word.getBook().getId() : null,
            word.getCreatedAt()
        );
    }
}
