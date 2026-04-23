package com.bookle.wordbook.controller;

import com.bookle.wordbook.domain.WordStatus;
import com.bookle.wordbook.dto.WordRequest;
import com.bookle.wordbook.dto.WordResponse;
import com.bookle.wordbook.dto.WordStatusUpdateRequest;
import com.bookle.wordbook.service.WordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wordbook/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping
    public List<WordResponse> list(
        @RequestParam(required = false) Long bookId,
        @RequestParam(required = false) WordStatus status
    ) {
        return wordService.findAll(bookId, status);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WordResponse create(@Valid @RequestBody WordRequest request) {
        return wordService.create(request);
    }

    @PatchMapping("/{id}/status")
    public WordResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody WordStatusUpdateRequest request
    ) {
        return wordService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        wordService.delete(id);
    }
}
