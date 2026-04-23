package com.bookle.wordbook.repository;

import com.bookle.wordbook.domain.Word;
import com.bookle.wordbook.domain.WordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WordRepository extends JpaRepository<Word, Long> {

    List<Word> findAllByOrderByCreatedAtDesc();

    List<Word> findAllByBookIdOrderByCreatedAtDesc(Long bookId);

    List<Word> findAllByStatusOrderByCreatedAtDesc(WordStatus status);

    List<Word> findAllByBookIdAndStatusOrderByCreatedAtDesc(Long bookId, WordStatus status);
}
