package com.bookle.wordbook.controller;

import com.bookle.wordbook.dto.AiSentenceRequest;
import com.bookle.wordbook.dto.AiSentenceResponse;
import com.bookle.wordbook.dto.AiWordRequest;
import com.bookle.wordbook.dto.AiWordResponse;
import com.bookle.wordbook.service.ClaudeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wordbook/ai")
@RequiredArgsConstructor
public class AiController {

    private final ClaudeService claudeService;

    @PostMapping("/word")
    public AiWordResponse word(@Valid @RequestBody AiWordRequest request) {
        return claudeService.generateWordMeaning(request.word());
    }

    @PostMapping("/sentence")
    public AiSentenceResponse sentence(@Valid @RequestBody AiSentenceRequest request) {
        return claudeService.generateSentenceTranslation(request.sentence());
    }
}
