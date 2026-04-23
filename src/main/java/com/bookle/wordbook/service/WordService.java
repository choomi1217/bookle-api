package com.bookle.wordbook.service;

import com.bookle.wordbook.domain.Book;
import com.bookle.wordbook.domain.Word;
import com.bookle.wordbook.domain.WordStatus;
import com.bookle.wordbook.dto.WordRequest;
import com.bookle.wordbook.dto.WordResponse;
import com.bookle.wordbook.repository.BookRepository;
import com.bookle.wordbook.repository.WordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final WordRepository wordRepository;
    private final BookRepository bookRepository;

    public List<WordResponse> findAll(Long bookId, WordStatus status) {
        List<Word> words;
        if (bookId != null && status != null) {
            words = wordRepository.findAllByBookIdAndStatusOrderByCreatedAtDesc(bookId, status);
        } else if (bookId != null) {
            words = wordRepository.findAllByBookIdOrderByCreatedAtDesc(bookId);
        } else if (status != null) {
            words = wordRepository.findAllByStatusOrderByCreatedAtDesc(status);
        } else {
            words = wordRepository.findAllByOrderByCreatedAtDesc();
        }
        return words.stream().map(WordResponse::from).toList();
    }

    @Transactional
    public WordResponse create(WordRequest request) {
        Book book = null;
        if (request.bookId() != null) {
            book = bookRepository.findById(request.bookId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "book not found: " + request.bookId()));
        }
        Word word = Word.builder()
            .word(request.word())
            .meaning(request.meaning())
            .example(request.example())
            .book(book)
            .build();
        return WordResponse.from(wordRepository.save(word));
    }

    @Transactional
    public WordResponse updateStatus(Long id, WordStatus status) {
        Word word = wordRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "word not found: " + id));
        word.changeStatus(status);
        return WordResponse.from(word);
    }

    @Transactional
    public void delete(Long id) {
        if (!wordRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "word not found: " + id);
        }
        wordRepository.deleteById(id);
    }
}
