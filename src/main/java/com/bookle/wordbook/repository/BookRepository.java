package com.bookle.wordbook.repository;

import com.bookle.wordbook.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}
