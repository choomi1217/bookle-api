package com.bookle.wordbook.dto;

import com.bookle.wordbook.domain.Book;
import java.time.Instant;

public record BookResponse(
    Long id,
    String title,
    String author,
    Instant createdAt
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getCreatedAt()
        );
    }
}
