package com.bookle.wordbook.service;

import com.bookle.wordbook.domain.Book;
import com.bookle.wordbook.dto.BookContentResponse;
import com.bookle.wordbook.dto.BookResponse;
import com.bookle.wordbook.repository.BookRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private static final Pattern SENTENCE_SPLITTER =
        Pattern.compile("(?<=[.!?])\\s+(?=[A-Z\"'])");

    private final BookRepository bookRepository;
    private final Map<Long, List<String>> contentCache = new HashMap<>();

    @PostConstruct
    void loadBookContents() {
        bookRepository.findAll().forEach(book -> {
            String resource = "books/" + book.getId() + ".txt";
            try {
                ClassPathResource file = new ClassPathResource(resource);
                if (!file.exists()) {
                    log.warn("book content file not found: {}", resource);
                    return;
                }
                String raw = new String(
                    file.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
                );
                List<String> sentences = Arrays.stream(SENTENCE_SPLITTER.split(raw))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .toList();
                contentCache.put(book.getId(), sentences);
                log.info("loaded book {} ({} sentences)", book.getId(), sentences.size());
            } catch (IOException e) {
                log.error("failed to load book {}", book.getId(), e);
            }
        });
    }

    public List<BookResponse> findAll() {
        return bookRepository.findAll().stream()
            .map(BookResponse::from)
            .toList();
    }

    public BookResponse findById(Long id) {
        return BookResponse.from(getOrThrow(id));
    }

    public BookContentResponse findContent(Long id) {
        Book book = getOrThrow(id);
        List<String> sentences = contentCache.getOrDefault(id, List.of());
        return new BookContentResponse(book.getId(), book.getTitle(), sentences);
    }

    private Book getOrThrow(Long id) {
        return bookRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "book not found: " + id));
    }
}
