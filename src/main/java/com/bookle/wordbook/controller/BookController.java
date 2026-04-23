package com.bookle.wordbook.controller;

import com.bookle.wordbook.dto.BookContentResponse;
import com.bookle.wordbook.dto.BookResponse;
import com.bookle.wordbook.service.BookService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wordbook/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookResponse> list() {
        return bookService.findAll();
    }

    @GetMapping("/{id}")
    public BookResponse get(@PathVariable Long id) {
        return bookService.findById(id);
    }

    @GetMapping("/{id}/content")
    public BookContentResponse content(@PathVariable Long id) {
        return bookService.findContent(id);
    }
}
